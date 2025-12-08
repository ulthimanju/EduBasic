import React, { useState, useEffect, useRef, lazy, Suspense } from 'react';
import { useTheme, ActionButton, OutlineButton, Arrow, Stepper, Tabs, Tab } from '../components';
import apiFetch, { apiEndpoints } from '../utils/apiClient';
import AppLayout from '../layouts/AppLayout';
import logoSvg from '../assets/logo.svg';

// Lazy load heavy dependencies - only loaded when ViewContentPage is accessed
const Editor = lazy(() => import('@monaco-editor/react'));

// Lazy load Mermaid only when first needed
let mermaidModule = null;
const loadMermaid = async () => {
  if (!mermaidModule) {
    mermaidModule = await import('mermaid');
  }
  return mermaidModule;
};

// Loading fallback for Editor
function EditorLoadingFallback() {
  return (
    <div style={{ height: '200px' }} className="rounded bg-gray-900 flex items-center justify-center">
      <div className="text-sm opacity-60">Loading editor...</div>
    </div>
  );
}

// Mermaid Component
function MermaidDiagram({
  chart,
  courseId,
  levelId,
  moduleId,
  lessonId,
  onFixSuccess,
  isExpanded = false,
}) {
  const mermaidRef = useRef(null);
  const [isLoading, setIsLoading] = useState(true);
  const [hasError, setHasError] = useState(false);

  useEffect(() => {
    if (!mermaidRef.current || !chart) return;

    const timer = setTimeout(async () => {
      try {
        const mermaidMod = await loadMermaid();
        const mermaid = mermaidMod.default;

        mermaid.initialize({
          startOnLoad: false,
          theme: 'dark',
          securityLevel: 'loose',
          flowchart: { useMaxWidth: true },
          graph: { useMaxWidth: true },
          sequence: { useMaxWidth: true },
          gantt: { useMaxWidth: true },
        });

        let cleanedChart = chart.trim();
        if (cleanedChart.startsWith('```mermaid')) {
          cleanedChart = cleanedChart
            .replace(/^```mermaid\s*/, '')
            .replace(/\s*```$/, '');
        } else if (cleanedChart.startsWith('```')) {
          cleanedChart = cleanedChart
            .replace(/^```\s*/, '')
            .replace(/\s*```$/, '');
        }

        const id = `mermaid-${Date.now()}-${Math.random()
          .toString(36)
          .slice(2, 9)}`;

        const { svg } = await mermaid.render(id, cleanedChart);

        if (!mermaidRef.current) return;
        mermaidRef.current.innerHTML = svg;

        const svgElement = mermaidRef.current.querySelector('svg');
        if (svgElement) {
          // kill intrinsic sizing so container is boss
          svgElement.removeAttribute('width');
          svgElement.removeAttribute('height');

          // scale nicely inside fixed box
          svgElement.setAttribute('preserveAspectRatio', 'xMidYMid meet');
          svgElement.style.width = '100%';
          svgElement.style.height = '100%';
          svgElement.style.display = 'block';
        }

        setIsLoading(false);
        setHasError(false);
      } catch (err) {
        console.error('Mermaid render error:', err);
        setHasError(true);
        setIsLoading(false);
      }
    }, 200);

    return () => clearTimeout(timer);
  }, [chart, isExpanded]);

  // 🔒 Consistent size for all diagrams
  const containerWidth = isExpanded ? 700 : 320;  // px
  const containerHeight = isExpanded ? 900 : 220; // px

  return (
    <div>
      {isLoading && !hasError && (
        <div
          style={{
            padding: '12px',
            textAlign: 'center',
            fontSize: '12px',
            opacity: 0.6,
          }}
        >
          Loading diagram...
        </div>
      )}

      {hasError && (
        <div
          style={{
            padding: '12px',
            textAlign: 'center',
            fontSize: '12px',
            color: '#f88',
          }}
        >
          Failed to render diagram
        </div>
      )}

      <div
        ref={mermaidRef}
        style={{
          backgroundColor: 'transparent',
          display: isLoading || hasError ? 'none' : 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          width: `${containerWidth}px`,
          height: `${containerHeight}px`,
          maxWidth: '100%',
          overflow: 'hidden',
          margin: '0 auto',
        }}
      />
    </div>
  );
}

function ViewContentPageContent({ onLogout, onNavigate, user, onOpenPracticeProblem }) {
  const colors = useTheme();
  const [courses, setCourses] = useState([]);
  const [selectedCourse, setSelectedCourse] = useState(null);
  const [levels, setLevels] = useState([]);
  const [levelsLoading, setLevelsLoading] = useState(false);
  const [selectedLevel, setSelectedLevel] = useState(null);
  const [modules, setModules] = useState([]);
  const [modulesLoading, setModulesLoading] = useState(false);
  const [selectedModule, setSelectedModule] = useState(null);
  const [lessons, setLessons] = useState([]);
  const [lessonsLoading, setLessonsLoading] = useState(false);
  const [selectedLesson, setSelectedLesson] = useState(null);
  const [lessonContent, setLessonContent] = useState(null);
  const [contentLoading, setContentLoading] = useState(false);
  const [jsonContent, setJsonContent] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [practiceProblem, setPracticeProblem] = useState(null);
  const [practiceProblemLoading, setPracticeProblemLoading] = useState(false);
  const [practiceProblemError, setPracticeProblemError] = useState(null);
  const [expandedDiagram, setExpandedDiagram] = useState(null);
  const [pendingSelection, setPendingSelection] = useState(() => {
    try {
      const raw = localStorage.getItem('viewLastSelection');
      return raw ? JSON.parse(raw) : null;
    } catch (err) {
      console.warn('Failed to read last selection', err);
      return null;
    }
  });
  const [restoringSelection, setRestoringSelection] = useState(false);

  const resetPracticeProblem = () => {
    setPracticeProblem(null);
    setPracticeProblemError(null);
    setPracticeProblemLoading(false);
  };

  // Fetch courses on mount
  useEffect(() => {
    fetchCourses();
  }, []);

  const fetchCourses = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await apiFetch(apiEndpoints.courses.list, {
        headers: {},
      });

      if (response.ok) {
        const data = await response.json();
        setCourses(Array.isArray(data) ? data : []);
      } else if (response.status === 401) {
        setError('Session expired. Please login again.');
        onLogout();
      } else {
        setError('Failed to load courses');
      }
    } catch (err) {
      setError('Error fetching courses: ' + err.message);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (!pendingSelection || restoringSelection) return;
    if (!courses || courses.length === 0) return;

    const restore = async () => {
      setRestoringSelection(true);
      try {
        const { courseId, levelId, moduleId, lessonId } = pendingSelection;
        const course = courses.find((c) => c.courseId === courseId);
        if (!course) return;

        setSelectedCourse(course);

        // Levels
        const levelResp = await apiFetch(apiEndpoints.courses.levels(courseId), {
          method: 'GET',
          headers: { 'Content-Type': 'application/json' },
        });
        if (!levelResp.ok) throw new Error(`Failed to fetch levels: ${levelResp.status}`);
        const levelsData = await levelResp.json();
        const levelsArray = Array.isArray(levelsData) ? levelsData : [];
        setLevels(levelsArray);
        const level = levelsArray.find((l) => l.levelId === levelId);
        if (!level) return;
        setSelectedLevel(level);

        // Modules
        const moduleResp = await apiFetch(apiEndpoints.courses.modules(courseId, levelId), {
          method: 'GET',
          headers: { 'Content-Type': 'application/json' },
        });
        if (!moduleResp.ok) throw new Error(`Failed to fetch modules: ${moduleResp.status}`);
        const modulesData = await moduleResp.json();
        const modulesArray = Array.isArray(modulesData) ? modulesData : [];
        setModules(modulesArray);
        const module = modulesArray.find((m) => m.moduleId === moduleId);
        if (!module) return;
        setSelectedModule(module);

        // Lessons
        const lessonResp = await apiFetch(apiEndpoints.courses.lessons(courseId, levelId, moduleId), {
          method: 'GET',
          headers: { 'Content-Type': 'application/json' },
        });
        if (!lessonResp.ok) throw new Error(`Failed to fetch lessons: ${lessonResp.status}`);
        const lessonsData = await lessonResp.json();
        const lessonsArray = Array.isArray(lessonsData) ? lessonsData : [];
        setLessons(lessonsArray);
        const lesson = lessonsArray.find((l) => l.lessonId === lessonId);
        if (!lesson) return;
        setSelectedLesson(lesson);

        await fetchLessonContent(courseId, levelId, moduleId, lessonId);
      } catch (err) {
        console.warn('Restore selection failed', err);
      } finally {
        setRestoringSelection(false);
        setPendingSelection(null);
      }
    };

    restore();
  }, [pendingSelection, courses, restoringSelection]);

  const fetchLevelsByCourseId = async (courseId, selectFirstLevel = false) => {
    try {
      setLevelsLoading(true);
      setError(null);
      const response = await apiFetch(apiEndpoints.courses.levels(courseId), {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json'
        }
      });
      if (!response.ok) {
        throw new Error(`Failed to fetch levels: ${response.status}`);
      }
      const data = await response.json();
      const levelsArray = Array.isArray(data) ? data : [];
      setLevels(levelsArray);
      
      // Auto-select first level if requested and levels exist
      if (selectFirstLevel && levelsArray.length > 0) {
        setSelectedLevel(levelsArray[0]);
        fetchModulesByLevelId(courseId, levelsArray[0].levelId);
      } else {
        setSelectedLevel(null);
        setModules([]);
        setSelectedModule(null);
        setLessons([]);
        setSelectedLesson(null);
        setLessonContent(null);
      }
    } catch (err) {
      setError(err.message);
      setLevels([]);
      setSelectedLevel(null);
      setModules([]);
      setSelectedModule(null);
      setLessons([]);
      setSelectedLesson(null);
      setLessonContent(null);
    } finally {
      setLevelsLoading(false);
    }
  };

  const fetchModulesByLevelId = async (courseId, levelId) => {
    try {
      setModulesLoading(true);
      setError(null);
      const response = await apiFetch(apiEndpoints.courses.modules(courseId, levelId), {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json'
        }
      });
      if (!response.ok) {
        throw new Error(`Failed to fetch modules: ${response.status}`);
      }
      const data = await response.json();
      setModules(Array.isArray(data) ? data : []);
      setSelectedModule(null);
      setLessons([]);
      setSelectedLesson(null);
      setLessonContent(null);
    } catch (err) {
      setError(err.message);
      setModules([]);
    } finally {
      setModulesLoading(false);
    }
  };

  const fetchLessonsByModuleId = async (courseId, levelId, moduleId, selectFirstLesson = false) => {
    try {
      setLessonsLoading(true);
      setError(null);
      const response = await apiFetch(apiEndpoints.courses.lessons(courseId, levelId, moduleId), {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json'
        }
      });
      if (!response.ok) {
        throw new Error(`Failed to fetch lessons: ${response.status}`);
      }
      const data = await response.json();
      const lessonsArray = Array.isArray(data) ? data : [];
      setLessons(lessonsArray);
      
      // Auto-select first lesson if requested and lessons exist
      if (selectFirstLesson && lessonsArray.length > 0) {
        setSelectedLesson(lessonsArray[0]);
        fetchLessonContent(courseId, levelId, moduleId, lessonsArray[0].lessonId);
      } else {
        setSelectedLesson(null);
        setLessonContent(null);
      }
    } catch (err) {
      setError(err.message);
      setLessons([]);
      setSelectedLesson(null);
      setLessonContent(null);
    } finally {
      setLessonsLoading(false);
    }
  };

  const fetchLessonContent = async (courseId, levelId, moduleId, lessonId) => {
    try {
      setContentLoading(true);
      setError(null);
      const response = await apiFetch(apiEndpoints.courses.lesson(courseId, levelId, moduleId, lessonId), {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json'
        }
      });
      if (!response.ok) {
        throw new Error(`Failed to fetch lesson content: ${response.status}`);
      }
      const data = await response.json();
      
      // Parse JSON string fields
      const parsedData = {
        ...data,
        objectives: data.objectives || [],
        examples: data.examplesJson ? (typeof data.examplesJson === 'string' ? JSON.parse(data.examplesJson) : data.examplesJson) : [],
        visualization: data.visualizationJson ? (typeof data.visualizationJson === 'string' ? JSON.parse(data.visualizationJson) : data.visualizationJson) : null,
        quiz: data.quizJson ? (typeof data.quizJson === 'string' ? JSON.parse(data.quizJson) : data.quizJson) : null,
        theory: data.theoryMarkdown || data.content || ''
      };
      
      setLessonContent(parsedData);
      setJsonContent(JSON.stringify(parsedData, null, 2));
    } catch (err) {
      setError(err.message);
      setLessonContent(null);
    } finally {
      setContentLoading(false);
    }
  };

  const handleGeneratePracticeProblem = async () => {
    if (practiceProblemLoading) return;

    if (!selectedCourse || !selectedLevel || !selectedModule || !selectedLesson) {
      setPracticeProblemError('Please select a course, level, module, and lesson first.');
      return;
    }

    // Persist current path so returning from Practice Problem restores the same lesson
    try {
      localStorage.setItem(
        'viewLastSelection',
        JSON.stringify({
          courseId: selectedCourse.courseId,
          levelId: selectedLevel.levelId,
          moduleId: selectedModule.moduleId,
          lessonId: selectedLesson.lessonId,
        })
      );
    } catch (err) {
      console.warn('Failed to persist last selection', err);
    }

    setPracticeProblemLoading(true);
    setPracticeProblemError(null);

    try {
      const response = await apiFetch(apiEndpoints.courses.practiceProblem(
        selectedCourse.courseId,
        selectedLevel.levelId,
        selectedModule.moduleId,
        selectedLesson.lessonId
      ), {
        method: 'POST',
        body: JSON.stringify({
          courseTitle: selectedCourse.title || selectedCourse.courseId || 'Course',
          levelType: selectedLevel.levelName || 'Level',
          moduleName: selectedModule.moduleTitle || 'Module',
          lessonTitle: selectedLesson.title || 'Lesson',
          username: user?.username || 'guest',
        }),
      });

      if (!response.ok) {
        throw new Error(`Practice problem request failed (${response.status})`);
      }

      const data = await response.json();
      setPracticeProblem(data);

      if (!data.success && data.message) {
        setPracticeProblemError(data.message);
      }

      if (onOpenPracticeProblem) {
        // Include courseId in the problem data for language detection
        onOpenPracticeProblem({
          ...data,
          courseId: selectedCourse.courseId,
        });
      }
    } catch (err) {
      setPracticeProblem(null);
      setPracticeProblemError(err.message);
    } finally {
      setPracticeProblemLoading(false);
    }
  };

  const handleCourseSelect = (course) => {
    resetPracticeProblem();
    setSelectedCourse(course);
    fetchLevelsByCourseId(course.courseId, true);
  };

  const handleLevelSelect = (level) => {
    resetPracticeProblem();
    setSelectedLevel(level);
    fetchModulesByLevelId(selectedCourse.courseId, level.levelId);
  };

  const handleModuleSelect = (module) => {
    resetPracticeProblem();
    setSelectedModule(module);
    fetchLessonsByModuleId(selectedCourse.courseId, selectedLevel.levelId, module.moduleId, true);
  };

  const handleLessonSelect = (lesson) => {
    resetPracticeProblem();
    setSelectedLesson(lesson);
    fetchLessonContent(selectedCourse.courseId, selectedLevel.levelId, selectedModule.moduleId, lesson.lessonId);
  };

  const copyToClipboard = () => {
    if (jsonContent) {
      navigator.clipboard.writeText(jsonContent);
      alert('JSON copied to clipboard!');
    }
  };

  const downloadJson = () => {
    if (jsonContent && selectedLesson) {
      const element = document.createElement('a');
      const file = new Blob([jsonContent], { type: 'application/json' });
      element.href = URL.createObjectURL(file);
      element.download = `${selectedLesson.lessonId || 'lesson'}.json`;
      document.body.appendChild(element);
      element.click();
      document.body.removeChild(element);
    }
  };

  return (
    <>
      <div
        className="min-h-screen w-full transition-colors duration-500"
        style={{ background: colors.bgApp, color: colors.textMain }}
      >


      {/* Mini Navigation Bar - Course List */}
      <div
        className="w-full rounded-none p-1.5 backdrop-blur-xl border-b shadow relative overflow-visible"
        style={{
          background: colors.bgPanel,
          borderColor: colors.border,
          boxShadow: colors.shadow,
        }}
      >
        <div className="flex items-center gap-2 px-3 overflow-x-auto overflow-y-hidden">
          {isLoading ? (
            <span className="text-xs opacity-50">Loading...</span>
          ) : courses.length === 0 ? (
            <span className="text-xs opacity-50">No courses</span>
          ) : (
            <>
              <div className="flex gap-1.5 flex-1 py-0.5">
                {courses.map((course, idx) => (
                  <button
                    key={idx}
                    onClick={() => handleCourseSelect(course)}
                    className="px-2.5 py-1 rounded-lg text-xs font-medium whitespace-nowrap transition-all duration-200 hover:scale-105 flex-shrink-0"
                    style={{
                      background: selectedCourse?.courseId === course.courseId ? colors.accent : colors.bgCard,
                      color: selectedCourse?.courseId === course.courseId ? '#fff' : colors.textMain,
                      border: selectedCourse?.courseId === course.courseId ? 'none' : `1px solid ${colors.border}`,
                    }}
                  >
                    {course.title || course.courseId || `Course ${idx + 1}`}
                  </button>
                ))}
              </div>
              
              {/* Curved Arrow pointing to first course */}
              {!selectedCourse && (
                <div className="absolute left-6 top-full mt-3 pointer-events-none z-50">
                  <div className="flex flex-col items-start">
                    <Arrow curved={true} color="#ffffff" animate={false} />
                    <p className="text-sm font-medium text-white opacity-90 ml-2">
                      Select course to start
                    </p>
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </div>

      {/* Main Content */}
      <div className="flex">
        {/* Left Sidebar - Levels */}
        {selectedCourse && (
          <div
            className="w-64 border-r sticky top-0 h-screen"
            style={{
              background: colors.bgPanel,
              borderColor: colors.border,
              display: 'flex',
              flexDirection: 'column',
            }}
          >
            <div className="p-6 pb-2 flex-shrink-0">
              <h2 className="text-xl font-bold">Levels</h2>
            </div>
            
            {levelsLoading ? (
              <div className="text-center py-4 px-6">
                <p className="opacity-60 text-xs">Loading...</p>
              </div>
            ) : levels.length === 0 ? (
              <div className="text-center py-4 px-6">
                <p className="opacity-60 text-xs">No levels found</p>
              </div>
            ) : (
              <div 
                className="space-y-2 px-6 pb-6 flex-1"
                style={{
                  overflowY: 'auto',
                  overflowX: 'hidden',
                }}
              >
                {levels.map((level, idx) => (
                  <div
                    key={idx}
                    onClick={() => handleLevelSelect(level)}
                    className="p-3 rounded-lg cursor-pointer transition-all duration-200 hover:opacity-80"
                    style={{
                      background: selectedLevel?.levelId === level.levelId ? colors.bgCard : 'transparent',
                      color: colors.textMain,
                    }}
                  >
                    <div className="font-semibold text-base">{level.levelName || `Level ${idx + 1}`}</div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Main Content Area */}
        <div className="flex-1 p-6 pt-6">
          <div className="grid gap-6" style={{ gridTemplateColumns: selectedModule ? '1fr' : '1fr 1fr' }}>
          {/* Modules List */}
          {selectedLevel && !selectedModule && (
          <div className="h-fit">
            <h2 className="text-xl font-bold mb-4">Modules</h2>
            
            {modulesLoading ? (
              <div className="text-center py-8">
                <p className="opacity-60 text-sm">Loading modules...</p>
              </div>
            ) : modules.length === 0 ? (
              <div className="text-center py-8">
                <p className="opacity-60 text-sm">No modules found</p>
              </div>
            ) : (
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {modules.map((module, idx) => {
                  const isActive = selectedModule === module;
                  const colorStops = [
                    'linear-gradient(135deg, #FFB347, #FF8C42)',
                    'linear-gradient(135deg, #1FB4A2, #17A2B8)',
                    'linear-gradient(135deg, #3A7BD5, #00D2FF)',
                    colors.accent
                  ];
                  const headerGradient = colorStops[idx % colorStops.length];
                  return (
                    <button
                      key={idx}
                      onClick={() => handleModuleSelect(module)}
                      className="text-left rounded-xl overflow-hidden transition-all duration-200 shadow-xl"
                      style={{
                        background: colors.bgCard,
                        color: colors.textMain,
                        border: `1px solid ${isActive ? colors.accent : colors.border}`,
                        transform: isActive ? 'translateY(-2px)' : 'none',
                      }}
                    >
                      <div
                        className="px-4 py-2 text-sm font-bold uppercase tracking-wide"
                        style={{
                          background: headerGradient,
                          color: '#fff',
                          letterSpacing: '0.04em',
                        }}
                      >
                        {module.moduleTitle || `Module ${idx + 1}`}
                      </div>
                      {module.estimatedTimeMinutes && (
                        <div className="px-4 py-3">
                          <div className="text-[11px] font-semibold opacity-75">⏱️ {module.estimatedTimeMinutes} min</div>
                        </div>
                      )}
                    </button>
                  );
                })}
              </div>
            )}
          </div>
          )}

          {/* Lessons List */}
          {selectedModule && selectedLevel && (
          <div className="h-fit">
            {lessonsLoading ? (
              <div className="text-center py-8">
                <p className="opacity-60 text-sm">Loading lessons...</p>
              </div>
            ) : lessons.length === 0 ? (
              <div className="text-center py-8">
                <p className="opacity-60 text-sm">No lessons found</p>
              </div>
            ) : (
              <div className="space-y-4">
                <div className="flex items-center gap-3 flex-wrap">
                  <button
                    onClick={() => setSelectedModule(null)}
                    className="px-2 py-1.5 rounded-lg text-lg font-medium transition-all duration-200 hover:opacity-80"
                    style={{
                      background: colors.bgCard,
                      color: colors.textMain,
                      border: `1px solid ${colors.border}`
                    }}
                  >
                    ←
                  </button>
                  <h2 className="text-xl font-bold">Lessons:</h2>
                  {lessons.map((lesson, idx) => (
                    <button
                      key={idx}
                      onClick={() => handleLessonSelect(lesson)}
                      className="px-3 py-1.5 rounded-lg cursor-pointer transition-all duration-200 hover:opacity-80 text-base font-medium whitespace-nowrap"
                      style={{
                        background: selectedLesson === lesson ? colors.accent : colors.bgCard,
                        border: `1px solid ${selectedLesson === lesson ? colors.accent : colors.border}`,
                        color: selectedLesson === lesson ? '#ffffff' : colors.textMain,
                      }}
                    >
                      {lesson.title || `Lesson ${idx + 1}`}
                    </button>
                  ))}
                </div>
              </div>
            )}
            
            {/* Content Viewer */}
            {selectedLesson && (
            <div className="mt-6">

            {contentLoading ? (
              <div
                className="flex items-center justify-center h-96 rounded-xl"
                style={{ background: colors.bgCard }}
              >
                <p className="text-center opacity-60 text-sm">Loading lesson content...</p>
              </div>
            ) : lessonContent ? (
              <div
                className="rounded-xl p-4"
                style={{ 
                  background: colors.bgCard,
                  maxHeight: 'calc(100vh - 200px)',
                  overflowY: 'auto',
                  overflowX: 'hidden'
                }}
              >
                {/* Title and Practice Problem Button */}
                <div className="mb-4 flex items-center justify-between">
                  {lessonContent.title && (
                    <h3 className="text-lg font-bold">{lessonContent.title}</h3>
                  )}
                  <OutlineButton
                    label={practiceProblemLoading ? 'Generating...' : 'Practice Problem'}
                    onClick={handleGeneratePracticeProblem}
                  />
                </div>

                {practiceProblemError && (
                  <div className="mb-3 text-sm px-3 py-2 rounded" style={{ background: colors.bgApp }}>
                    <span className="font-semibold">Practice problem error: </span>
                    <span className="opacity-80">{practiceProblemError}</span>
                  </div>
                )}

                {practiceProblem && practiceProblem.success && (
                  <div className="mb-4 rounded-lg p-4 space-y-3" style={{ background: colors.bgApp }}>
                    {practiceProblem.title && (
                      <div className="text-base font-bold">{practiceProblem.title}</div>
                    )}
                    {practiceProblem.statement && (
                      <p className="text-sm whitespace-pre-wrap">{practiceProblem.statement}</p>
                    )}
                    {practiceProblem.inputFormat && (
                      <div className="text-xs opacity-80">
                        <span className="font-semibold">Input:</span> {practiceProblem.inputFormat}
                      </div>
                    )}
                    {practiceProblem.outputFormat && (
                      <div className="text-xs opacity-80">
                        <span className="font-semibold">Output:</span> {practiceProblem.outputFormat}
                      </div>
                    )}
                    {practiceProblem.constraints && (
                      <div className="text-xs opacity-80">
                        <span className="font-semibold">Constraints:</span> {practiceProblem.constraints}
                      </div>
                    )}
                    {practiceProblem.hints && practiceProblem.hints.length > 0 && (
                      <div className="text-xs space-y-1">
                        <div className="font-semibold">Hints</div>
                        <ul className="space-y-1 pl-4 list-disc">
                          {practiceProblem.hints.map((hint, idx) => (
                            <li key={idx}>{hint}</li>
                          ))}
                        </ul>
                      </div>
                    )}
                    {practiceProblem.testCases && practiceProblem.testCases.length > 0 && (
                      <div className="text-xs space-y-1">
                        <div className="font-semibold">Test Cases</div>
                        <ul className="space-y-1 pl-4 list-disc">
                          {practiceProblem.testCases.map((tc, idx) => (
                            <li key={idx}>{tc}</li>
                          ))}
                        </ul>
                      </div>
                    )}
                  </div>
                )}

                {/* Objectives */}
                {lessonContent.objectives && Array.isArray(lessonContent.objectives) && lessonContent.objectives.length > 0 && (
                  <div className="mb-4">
                    <h4 className="font-semibold mb-2 text-sm">Learning Objectives</h4>
                    <ul className="space-y-1">
                      {lessonContent.objectives.map((objective, idx) => (
                        <li key={idx} className="text-sm opacity-80 ml-4">• {objective}</li>
                      ))}
                    </ul>
                  </div>
                )}

                {/* Theory/Content */}
                {lessonContent.theory && (
                  <div className="mb-4">
                    <h4 className="font-semibold mb-2 text-sm">Theory</h4>
                    <p className="text-sm whitespace-pre-wrap">{lessonContent.theory}</p>
                  </div>
                )}

                {/* Examples with embedded visualizations */}
                <div className="mb-4">
                  {lessonContent.examples && Array.isArray(lessonContent.examples) && lessonContent.examples.length > 0 && (
                    <div>
                      <h4 className="font-semibold mb-4 text-sm">Examples</h4>
                      <Tabs defaultActiveTab={0}>
                        {lessonContent.examples.map((example, idx) => {
                          // Format tab label: convert "method_structure" to "Method Structure"
                          const formatLabel = (text) => {
                            let formatted = text
                              .split('_')
                              .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
                              .join(' ');
                            // Remove "Explained" from the label
                            formatted = formatted.replace(/\s*Explained\s*/g, '').trim();
                            return formatted;
                          };
                          
                          return (
                            <Tab 
                              key={example.exmpl_id || idx}
                              label={formatLabel(String(example.exmpl_id || idx + 1))}
                            >
                            <div className="space-y-4 pt-4">
                              {/* Visualization Section */}
                              {example.visualization && (
                                <div className="rounded p-3 flex justify-center relative" style={{ background: colors.bgApp }}>
                                  <button
                                    onClick={() => setExpandedDiagram(example.visualization)}
                                    className="absolute top-3 right-3 p-1.5 rounded hover:opacity-80 transition-opacity"
                                    style={{ background: colors.bgCard, color: colors.textMain }}
                                    title="Expand diagram"
                                  >
                                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                      <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3" />
                                    </svg>
                                  </button>
                                  <MermaidDiagram 
                                    chart={example.visualization}
                                    courseId={selectedCourse?.courseId}
                                    levelId={selectedLevel?.levelId}
                                    moduleId={selectedModule?.moduleId}
                                    lessonId={selectedLesson?.lessonId}
                                    onFixSuccess={() => handleLessonSelect(selectedLesson)}
                                  />
                                </div>
                              )}
                              
                              {/* Code Section */}
                              <div className="space-y-3">
                                <div className="rounded overflow-hidden" style={{ background: colors.bgApp }}>
                                  <div className="p-3 pb-2 border-b" style={{ borderColor: colors.border }}>
                                    <div className="text-xs font-semibold">Sample Code</div>
                                  </div>
                                  {example.code && (
                                    <Suspense fallback={<EditorLoadingFallback />}>
                                      <Editor
                                        height={`${example.code.split('\n').length * 20}px`}
                                        defaultLanguage="java"
                                        value={example.code}
                                        theme={colors.isDark ? "vs-dark" : "vs"}
                                        options={{
                                          readOnly: true,
                                          minimap: { enabled: false },
                                          scrollBeyondLastLine: false,
                                          fontSize: 12,
                                          lineNumbers: 'on',
                                          renderLineHighlight: 'none',
                                          smoothScrolling: true,
                                          scrollbar: {
                                            vertical: 'auto',
                                            horizontal: 'auto',
                                            useShadows: true,
                                          },
                                        }}
                                      />
                                    </Suspense>
                                  )}
                                </div>
                                {example.output && Array.isArray(example.output) && example.output.length > 0 && (
                                  <div className="p-3 rounded space-y-1" style={{ background: colors.bgApp, borderLeft: `3px solid ${colors.accent}` }}>
                                    <div className="text-xs font-semibold mb-2">Output:</div>
                                    {example.output.map((line, lineIdx) => (
                                      <div key={lineIdx} className="text-xs opacity-80 font-mono">
                                        {line}
                                      </div>
                                    ))}
                                  </div>
                                )}
                              </div>
                            </div>
                            </Tab>
                          );
                        })}
                      </Tabs>
                    </div>
                  )}
                </div>

                {/* Quiz */}
                {false && lessonContent.quiz && lessonContent.quiz.questions && Array.isArray(lessonContent.quiz.questions) && lessonContent.quiz.questions.length > 0 && (
                  <div className="mb-4">
                    <h4 className="font-semibold mb-2 text-sm">Quiz Questions</h4>
                    <div className="space-y-3">
                      {lessonContent.quiz.questions.map((question, idx) => (
                        <div key={idx} className="p-3 rounded" style={{ background: colors.bgApp }}>
                          <p className="text-sm font-semibold mb-2">Q{idx + 1}: {question.question}</p>
                          <p className="text-xs opacity-60 mb-2">Type: {question.type || 'multiple-choice'}</p>
                          {question.options && Array.isArray(question.options) && question.options.length > 0 && (
                            <ul className="text-xs opacity-80 space-y-1 ml-2 mb-2">
                              {question.options.map((option, optIdx) => (
                                <li key={optIdx}>• {option}</li>
                              ))}
                            </ul>
                          )}
                          {question.answer && (
                            <p className="text-xs mt-2 font-semibold" style={{ color: colors.success || '#10b981' }}>
                              Answer: {question.answer}
                            </p>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Difficulty */}
                {lessonContent.difficulty && (
                  <div className="flex items-center gap-2 mt-4 p-2 rounded" style={{ background: colors.bgApp }}>
                    <span className="text-xs font-semibold">Difficulty:</span>
                    <span className="text-xs">{lessonContent.difficulty}</span>
                  </div>
                )}
              </div>
            ) : (
              <div
                className="flex items-center justify-center h-96 rounded-xl"
                style={{ background: colors.bgCard }}
              >
                <p className="text-center opacity-60 text-sm">No content available</p>
              </div>
            )}
          </div>
            )}
          </div>
          )}
            </div>
          </div>
        </div>
      </div>

      {/* Expanded Diagram Modal */}
      {expandedDiagram && (
        <div 
          className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
          onClick={() => setExpandedDiagram(null)}
        >
          <div 
            className="rounded-lg p-6 max-h-[90vh] overflow-auto relative"
            style={{ background: colors.bgApp, width: '700px', maxWidth: '100%' }}
            onClick={(e) => e.stopPropagation()}
          >
            <button
              onClick={() => setExpandedDiagram(null)}
              className="absolute top-3 right-3 p-2 rounded hover:opacity-80 transition-opacity z-10"
              style={{ background: colors.bgCard, color: colors.textMain }}
              title="Close"
            >
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
            <MermaidDiagram 
              chart={expandedDiagram}
              courseId={selectedCourse?.courseId}
              levelId={selectedLevel?.levelId}
              moduleId={selectedModule?.moduleId}
              lessonId={selectedLesson?.lessonId}
              onFixSuccess={() => handleLessonSelect(selectedLesson)}
              isExpanded={true}
            />
          </div>
        </div>
      )}
    </>
  );
}

function ViewContentPage({ onLogout, onNavigate, user, onOpenPracticeProblem }) {
  return (
    <AppLayout user={user} onLogout={onLogout} onNavigate={onNavigate} title="Courses">
      <ViewContentPageContent onLogout={onLogout} onNavigate={onNavigate} user={user}
        onOpenPracticeProblem={onOpenPracticeProblem} />
    </AppLayout>
  );
}

export default ViewContentPage;
