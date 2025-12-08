import React, { useEffect, useMemo, useState, Suspense, lazy, useRef } from 'react';
import { Lightbulb, Maximize2, X, Moon, Sun, Copy, Check, BookOpen, Zap } from 'lucide-react';
import ProblemPageLayout from '../layouts/ProblemPageLayout';
import { Alert, useTheme } from '../components';
import { getAuthToken } from '../utils/apiClient';
import { useThemeState } from '../hooks/useThemeState';

const Editor = lazy(() => import('@monaco-editor/react'));

function ProblemPage({ onNavigate, data }) {
  const [selectedTheme, handleThemeChange] = useThemeState();
  const problemData = useMemo(() => {
    if (data) return data;
    try {
      const stored = localStorage.getItem('practiceProblemData');
      return stored ? JSON.parse(stored) : null;
    } catch (err) {
      console.warn('ProblemPage: failed to parse stored practice problem', err);
      return null;
    }
  }, [data]);

  const handleBack = () => {
    if (window?.history?.length > 1) {
      window.history.back();
    } else {
      onNavigate('view');
    }
  };

  return (
    <ProblemPageLayout selectedTheme={selectedTheme}>
      <ProblemPageContent
        problemData={problemData}
        handleBack={handleBack}
        onThemeChange={handleThemeChange}
        selectedTheme={selectedTheme}
      />
    </ProblemPageLayout>
  );
}

function ProblemPageContent({ problemData, handleBack, onThemeChange, selectedTheme }) {
  const colors = useTheme();
  const heading = problemData?.title || 'Practice Problem';
  const [editorValue, setEditorValue] = useState(() => problemData?.starterCode || '// Write your solution here');
  const [isEditorExpanded, setIsEditorExpanded] = useState(false);
  const [showHints, setShowHints] = useState(true);
  const [showSolutions, setShowSolutions] = useState(false);
  const [isDarkMode, setIsDarkMode] = useState(() => selectedTheme !== 'Default Light');
  const [copiedStatementId, setCopiedStatementId] = useState(null);
  const [leftGridHeight, setLeftGridHeight] = useState(null);
  const leftGridRef = useRef(null);

  useEffect(() => {
    if (problemData?.starterCode) {
      setEditorValue(problemData.starterCode);
    }
  }, [problemData?.starterCode]);

  useEffect(() => {
    // Measure left grid height after content is rendered (including constraints)
    const measureHeight = () => {
      if (leftGridRef?.current) {
        const height = leftGridRef.current.scrollHeight;
        setLeftGridHeight(height);
      }
    };

    // Measure immediately and also after a short delay to ensure all content is rendered
    measureHeight();
    const timer = setTimeout(measureHeight, 100);

    return () => clearTimeout(timer);
  }, [problemData]);

  useEffect(() => {
    setIsDarkMode(selectedTheme !== 'Default Light');
  }, [selectedTheme]);

  const handleThemeToggle = () => {
    const newMode = !isDarkMode;
    const nextTheme = newMode ? 'Midnight Sunset' : 'Default Light';
    onThemeChange(nextTheme);
    setIsDarkMode(newMode);
  };

  const handleCopyStatement = async () => {
    const text = problemData?.statement || '';
    try {
      await navigator.clipboard.writeText(text);
      setCopiedStatementId('statement');
      setTimeout(() => setCopiedStatementId(null), 2000);
    } catch (err) {
      console.error('Failed to copy statement', err);
    }
  };

  const detectLanguageFromCourseId = () => {
    if (!problemData?.courseId) return 'javascript';
    const courseId = problemData.courseId.toLowerCase();
    if (courseId.includes('java')) return 'java';
    if (courseId.includes('python')) return 'python';
    if (courseId.includes('javascript') || courseId.includes('js')) return 'javascript';
    if (courseId.includes('typescript') || courseId.includes('ts')) return 'typescript';
    if (courseId.includes('cpp') || courseId.includes('c++')) return 'cpp';
    if (courseId.includes('csharp') || courseId.includes('c#')) return 'csharp';
    if (courseId.includes('go')) return 'go';
    if (courseId.includes('rust')) return 'rust';
    if (courseId.includes('php')) return 'php';
    if (courseId.includes('ruby')) return 'ruby';
    if (courseId.includes('kotlin')) return 'kotlin';
    if (courseId.includes('swift')) return 'swift';
    return 'javascript';
  };

  return (
    <div
      className="h-screen w-full px-6 pb-6"
      style={{ color: colors.textMain, background: colors.bgApp, maxHeight: '100vh' }}
    >
      <div
        className="sticky top-0 z-10 mb-3 flex items-center py-2"
        style={{ background: colors.bgApp, borderBottom: `1px solid ${colors.border}` }}
      >
        <div className="flex items-center gap-3" style={{ width: '88px' }}>
          <button
            onClick={handleBack}
            className="px-4 py-2 rounded-lg text-sm font-semibold"
            style={{ background: colors.bgPanel, color: colors.textMain, border: `1px solid ${colors.border}` }}
          >
            ← Back
          </button>
        </div>
        <div className="flex-1 text-center">
          <h1 className="text-2xl font-bold">{heading}</h1>
        </div>
        <div className="flex items-center justify-end gap-3" style={{ width: '88px' }}>
          <button
            type="button"
            onClick={handleThemeToggle}
            className="px-2 py-2 rounded border hover:bg-opacity-10 transition-colors"
            style={{ color: colors.textMain, borderColor: colors.border }}
            aria-label="Toggle theme"
            title="Toggle theme"
          >
            {isDarkMode ? <Sun size={18} /> : <Moon size={18} />}
          </button>
        </div>
      </div>

      {!problemData && (
        <Alert type="warning" message="No practice problem found. Generate one from a lesson." />
      )}

      {problemData && (
        <div className="grid gap-3 md:grid-cols-2">
          <div className="space-y-3 overflow-y-auto max-h-[calc(100vh-70px)]" ref={leftGridRef}>
            {showSolutions ? (
              <ContentCard
                title="Solutions"
                colors={colors}
                body={normalizeContent(problemData.solutions || 'No solutions available yet.')}
                action={
                  <button
                    type="button"
                    onClick={() => setShowSolutions((prev) => !prev)}
                    className="px-2 py-1 rounded border hover:bg-opacity-10 transition-colors"
                    style={{ color: colors.textMain, borderColor: colors.border }}
                    aria-label="Hide solutions"
                    title="Hide solutions"
                  >
                    <BookOpen size={16} />
                  </button>
                }
              />
            ) : (
              <>
                {problemData.statement && (
                  <ContentCard
                    title="Statement"
                    body={normalizeContent(problemData.statement)}
                    colors={colors}
                    action={
                      <div className="flex items-center gap-2">
                        {problemData.hints && problemData.hints.length > 0 && (
                          <button
                            type="button"
                            onClick={() => setShowHints((prev) => !prev)}
                            className="px-2 py-1 rounded border hover:bg-opacity-10 transition-colors"
                            style={{ color: colors.textMain, borderColor: colors.border }}
                            aria-label={showHints ? 'Hide hints' : 'Show hints'}
                            title={showHints ? 'Hide hints' : 'Show hints'}
                          >
                            {showHints ? <Zap size={16} fill="currentColor" /> : <Lightbulb size={16} />}
                          </button>
                        )}
                        <button
                          type="button"
                          onClick={handleCopyStatement}
                          className="px-2 py-1 rounded border hover:bg-opacity-10 transition-colors"
                          style={{ color: colors.textMain, borderColor: colors.border }}
                          aria-label="Copy statement"
                          title="Copy statement"
                        >
                          {copiedStatementId === 'statement' ? <Check size={16} /> : <Copy size={16} />}
                        </button>
                        <button
                          type="button"
                          onClick={() => setShowSolutions((prev) => !prev)}
                          className="px-2 py-1 rounded border hover:bg-opacity-10 transition-colors"
                          style={{ color: colors.textMain, borderColor: colors.border }}
                          aria-label={showSolutions ? 'Hide solutions' : 'Show solutions'}
                          title={showSolutions ? 'Hide solutions' : 'Show solutions'}
                        >
                          <BookOpen size={16} />
                        </button>
                      </div>
                    }
                  />
                )}

                {showHints && problemData.hints && problemData.hints.length > 0 && (
                  <ContentCard
                    title="Hints"
                    colors={colors}
                    body={normalizeContent(problemData.hints)}
                  />
                )}

                {(problemData.inputFormat || problemData.outputFormat) && (
                  <div className="grid gap-3 md:grid-cols-2">
                    {problemData.inputFormat && (
                      <ContentCard title="Input Format" body={normalizeContent(problemData.inputFormat)} colors={colors} />
                    )}
                    {problemData.outputFormat && (
                      <ContentCard title="Output Format" body={normalizeContent(problemData.outputFormat)} colors={colors} />
                    )}
                  </div>
                )}

                {problemData.testCases && problemData.testCases.length > 0 && (
                  <TestCasesCard testCases={problemData.testCases} colors={colors} />
                )}

                {problemData.constraints && (
                  <ContentCard title="Constraints" body={normalizeContent(problemData.constraints)} colors={colors} />
                )}
              </>
            )}
          </div>

          <div className="space-y-3 overflow-y-auto max-h-[calc(100vh-70px)] pr-2">
            <EditorCard
              colors={colors}
              value={editorValue}
              onChange={setEditorValue}
              language={detectLanguageFromCourseId()}
              onExpand={() => setIsEditorExpanded(true)}
              testCases={problemData?.testCases || []}
              problemData={problemData}
              leftGridRef={leftGridRef}
              isDarkMode={isDarkMode}
            />
          </div>
        </div>
      )}

      {isEditorExpanded && (
        <EditorModal
          colors={colors}
          value={editorValue}
          onChange={setEditorValue}
          language={detectLanguageFromCourseId()}
          onClose={() => setIsEditorExpanded(false)}
        />
      )}
    </div>
  );
}

function TestCasesCard({ testCases, colors }) {
  const hasExplanation = testCases.some(
    (tc) => tc.explanation != null && String(tc.explanation).trim() !== ''
  );
  const inputWidth = hasExplanation ? '45%' : '55%';
  const outputWidth = hasExplanation ? '30%' : '45%';
  const visibleTestCases = testCases.slice(0, -2);
  const hiddenCount = Math.max(0, testCases.length - visibleTestCases.length);

  return (
    <div
      className="rounded-2xl p-4 border shadow"
      style={{ background: colors.bgCard, borderColor: colors.border }}
    >
      <div className="flex items-center justify-between mb-3">
        <div className="text-sm font-semibold" style={{ color: colors.textMain }}>
          Test Cases
        </div>
        {hiddenCount > 0 && (
          <div
            className="px-2 py-1 rounded-full text-xs font-semibold"
            style={{ background: colors.bgPanel, color: colors.textMuted }}
          >
            {hiddenCount} Hidden
          </div>
        )}
      </div>
      <div className="overflow-x-auto">
        <table className="w-full" style={{ borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ borderBottom: `2px solid ${colors.border}` }}>
              <th className="text-left py-2 px-3 text-xs font-semibold" style={{ color: colors.textMain, width: inputWidth }}>
                Input
              </th>
              <th className="text-left py-2 px-3 text-xs font-semibold" style={{ color: colors.textMain, width: outputWidth }}>
                Output
              </th>
              {hasExplanation && (
                <th className="text-left py-2 px-3 text-xs font-semibold" style={{ color: colors.textMain, width: '30%' }}>
                  Explanation
                </th>
              )}
            </tr>
          </thead>
          <tbody>
            {visibleTestCases.map((tc, idx) => (
              <tr
                key={idx}
                style={{
                  borderBottom: idx < visibleTestCases.length - 1 ? `1px solid ${colors.border}` : 'none',
                }}
              >
                <td className="py-3 px-3 text-xs" style={{ color: colors.textMain, verticalAlign: 'top' }}>
                  <code
                    className="block"
                    style={{
                      background: colors.bgPanel,
                      padding: '6px 8px',
                      borderRadius: '4px',
                      fontFamily: 'monospace',
                      whiteSpace: 'pre-wrap',
                      wordBreak: 'break-word',
                    }}
                  >
                    {tc.input || 'N/A'}
                  </code>
                </td>
                <td className="py-3 px-3 text-xs" style={{ color: colors.textMain, verticalAlign: 'top' }}>
                  <code
                    className="block"
                    style={{
                      background: colors.bgPanel,
                      padding: '6px 8px',
                      borderRadius: '4px',
                      fontFamily: 'monospace',
                      whiteSpace: 'pre-wrap',
                      wordBreak: 'break-word',
                    }}
                  >
                    {tc.output || 'N/A'}
                  </code>
                </td>
                {hasExplanation && (
                  <td className="py-3 px-3 text-xs" style={{ color: colors.textMuted, verticalAlign: 'top' }}>
                    {tc.explanation || '—'}
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function EditorCard({ colors, value, onChange, language, onExpand, testCases = [], problemData = {}, leftGridRef, isDarkMode }) {
  const [isRunning, setIsRunning] = useState(false);
  const [runResults, setRunResults] = useState(null);
  const [resultsHeight, setResultsHeight] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [modalMessage, setModalMessage] = useState({ type: 'success', text: '' });

  const formatLanguageName = (lang) => {
    return lang.charAt(0).toUpperCase() + lang.slice(1);
  };

  const handleRun = async () => {
    if (!testCases || testCases.length === 0) {
      alert('No test cases available');
      return;
    }

    // Measure left grid height when Run is clicked
    if (leftGridRef?.current) {
      const height = leftGridRef.current.scrollHeight;
      setResultsHeight(height);
    }

    setIsRunning(true);
    setRunResults(null);

    try {
      const inputs = testCases.map((tc) => tc.input || '');
      const expectedOutputs = testCases.map((tc) => tc.output || '');

      const token = getAuthToken();

      const response = await fetch('http://localhost:8080/api/code/execute', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({
          language: language,
          code: value,
          inputs: inputs,
          expectedOutputs: expectedOutputs,
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      setRunResults(data.results || []);
    } catch (error) {
      console.error('Error running code:', error);
      alert('Error running code: ' + error.message);
    } finally {
      setIsRunning(false);
    }
  };

  const handleSubmit = async () => {
    // Check if tests have been run
    if (!runResults || runResults.length === 0) {
      setModalMessage({ type: 'error', text: 'Please run the test cases first before submitting.' });
      setShowModal(true);
      return;
    }

    // Check if at least 3 test cases passed
    const passedCount = runResults.filter((r) => r.passed).length;
    if (passedCount < 3) {
      setModalMessage({ type: 'error', text: `At least 3 test cases must pass to submit. Currently ${passedCount} test case(s) passed.` });
      setShowModal(true);
      return;
    }

    try {
      const token = getAuthToken();
      
      const response = await fetch('http://localhost:8080/api/code/submit', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({
          problemId: problemData?.id || 'UNKNOWN',
          code: value,
          language: language,
        }),
      });

      const data = await response.json();

      if (data.success) {
        setModalMessage({ type: 'success', text: data.message });
      } else {
        setModalMessage({ type: 'error', text: data.message });
      }
      setShowModal(true);
    } catch (error) {
      console.error('Error submitting solution:', error);
      setModalMessage({ type: 'error', text: 'Error submitting solution: ' + error.message });
      setShowModal(true);
    }
  };

  return (
    <div
      className="rounded-2xl p-4 border shadow"
      style={{ background: colors.bgCard, borderColor: colors.border }}
    >
      <div className="flex items-center justify-between mb-2">
        <div className="text-sm font-semibold" style={{ color: colors.textMain }}>
          Your Solution
        </div>
        {runResults && (
          <div
            className="px-3 py-1 rounded-full text-xs font-semibold text-center"
            style={{
              background: runResults.filter((r) => r.passed).length === runResults.length
                ? 'rgba(48, 209, 88, 0.2)'
                : runResults.filter((r) => r.passed).length === 0
                ? 'rgba(255, 69, 58, 0.2)'
                : 'rgba(255, 193, 7, 0.2)',
              color: runResults.filter((r) => r.passed).length === runResults.length
                ? '#30D158'
                : runResults.filter((r) => r.passed).length === 0
                ? '#FF453A'
                : '#FFC107',
            }}
          >
            Passed: {runResults.filter((r) => r.passed).length}/{runResults.length}
          </div>
        )}
        <div className="flex items-center gap-2">
          <div
            className="px-2 py-1 rounded text-xs font-semibold"
            style={{ background: colors.bgPanel, color: colors.textMuted }}
          >
            {formatLanguageName(language)}
          </div>
          <button
            type="button"
            onClick={onExpand}
            className="px-2 py-1 rounded border hover:bg-opacity-10 transition-colors"
            style={{ color: colors.textMain, borderColor: colors.border }}
            aria-label="Expand editor"
            title="Expand editor"
          >
            <Maximize2 size={16} />
          </button>
        </div>
      </div>
      <Suspense
        fallback={
          <div className="h-64 rounded-lg flex items-center justify-center" style={{ background: colors.bgPanel }}>
            <span className="text-sm" style={{ color: colors.textMuted }}>Loading editor...</span>
          </div>
        }
      >
        <div style={{ height: '500px' }}>
          <Editor
            height="100%"
            language={language}
            theme={isDarkMode ? 'vs-dark' : 'vs-light'}
            value={value}
            onChange={(val) => onChange(val ?? '')}
            options={{
              fontSize: 14,
              minimap: { enabled: false },
              scrollBeyondLastLine: false,
              suggestOnTriggerCharacters: true,
              quickSuggestions: {
                other: true,
                comments: false,
                strings: false,
              },
              acceptSuggestionOnCommitCharacter: true,
            }}
          />
        </div>
      </Suspense>
      <div className="flex gap-2 mt-3">
        <button
          type="button"
          onClick={handleRun}
          disabled={isRunning}
          className="flex-1 px-4 py-2 rounded-lg text-sm font-semibold border transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          style={{
            background: colors.bgPanel,
            color: colors.textMain,
            borderColor: colors.border,
          }}
        >
          {isRunning ? 'Running...' : 'Run'}
        </button>
        <button
          type="button"
          onClick={handleSubmit}
          disabled={isRunning}
          className="flex-1 px-4 py-2 rounded-lg text-sm font-semibold text-white transition-colors hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed"
          style={{ background: colors.accent }}
        >
          Submit
        </button>
      </div>

      {runResults && (
        <div 
          className="mt-3 space-y-2 overflow-y-auto"
          style={{ maxHeight: resultsHeight ? `${resultsHeight}px` : '400px' }}
        >
          {runResults[0]?.error ? (
            <div
              className="p-3 rounded-lg border"
              style={{
                background: 'rgba(255, 69, 58, 0.1)',
                borderColor: '#FF453A',
              }}
            >
              <div className="text-xs" style={{ color: '#FF453A' }}>
                <strong>Error:</strong> {runResults[0].error}
              </div>
            </div>
          ) : (
            <>
              {runResults.map((result, idx) => (
                <div
                  key={idx}
                  className="p-3 rounded-lg border"
                  style={{
                    background: result.passed ? 'rgba(48, 209, 88, 0.1)' : 'rgba(255, 69, 58, 0.1)',
                    borderColor: result.passed ? '#30D158' : '#FF453A',
                  }}
                >
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-xs font-semibold" style={{ color: result.passed ? '#30D158' : '#FF453A' }}>
                      Test Case {idx + 1}: {result.passed ? '✓ PASSED' : '✗ FAILED'}
                    </span>
                  </div>
                  <div className="text-xs space-y-1" style={{ color: colors.textMuted }}>
                    {result.error ? (
                      <div style={{ color: '#FF453A' }}>
                        <strong>Error:</strong> {result.error}
                      </div>
                    ) : (
                      <>
                        <div><strong>Input:</strong> {result.input || 'N/A'}</div>
                        <div><strong>Expected:</strong> {result.expected || 'N/A'}</div>
                        <div><strong>Actual:</strong> {result.actual || 'N/A'}</div>
                      </>
                    )}
                  </div>
                </div>
              ))}
            </>
          )}
        </div>
      )}

      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={() => setShowModal(false)} />
          <div
            className="relative p-8 rounded-2xl shadow-2xl max-w-md w-full"
            style={{ background: colors.bgPanel }}
          >
            <div className="text-center mb-6">
              <div
                className="w-16 h-16 rounded-full mx-auto mb-4 flex items-center justify-center text-4xl"
                style={{
                  background: modalMessage.type === 'success' ? 'rgba(48, 209, 88, 0.2)' : 'rgba(255, 69, 58, 0.2)',
                  color: modalMessage.type === 'success' ? '#30D158' : '#FF453A',
                }}
              >
                {modalMessage.type === 'success' ? '✓' : '✕'}
              </div>
              <div className="text-xl font-bold mb-2" style={{ color: colors.textMain }}>
                {modalMessage.type === 'success' ? 'Success!' : 'Error'}
              </div>
              <div className="text-sm" style={{ color: colors.textMuted }}>
                {modalMessage.text}
              </div>
            </div>
            <button
              className="w-full py-3 rounded-lg font-semibold text-white"
              style={{ background: colors.accent }}
              onClick={() => setShowModal(false)}
            >
              Okay
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

function EditorModal({ colors, value, onChange, language, onClose }) {
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center"
      style={{ background: 'rgba(0,0,0,0.55)' }}
    >
      <div
        className="rounded-2xl border shadow-xl w-[90vw] max-w-5xl"
        style={{ background: colors.bgCard, borderColor: colors.border }}
      >
        <div className="flex items-center justify-between px-4 py-3" style={{ borderBottom: `1px solid ${colors.border}` }}>
          <div className="text-sm font-semibold" style={{ color: colors.textMain }}>
            Your Solution
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={onClose}
              className="px-2 py-1 rounded border hover:bg-opacity-10 transition-colors"
              style={{ color: colors.textMain, borderColor: colors.border }}
              aria-label="Close modal"
              title="Close modal"
            >
              <X size={16} />
            </button>
          </div>
        </div>
        <div className="px-4 pb-4 pt-2" style={{ height: '85vh' }}>
          <Suspense
            fallback={
              <div className="h-full rounded-lg flex items-center justify-center" style={{ background: '#111827' }}>
                <span className="text-sm text-gray-300">Loading editor...</span>
              </div>
            }
          >
            <div style={{ height: '100%' }}>
              <Editor
                height="100%"
                language={language}
                theme="vs-dark"
                value={value}
                onChange={(val) => onChange(val ?? '')}
                options={{
                  fontSize: 14,
                  minimap: { enabled: true },
                  scrollBeyondLastLine: false,
                  suggestOnTriggerCharacters: true,
                  quickSuggestions: {
                    other: true,
                    comments: false,
                    strings: false,
                  },
                  acceptSuggestionOnCommitCharacter: true,
                }}
              />
            </div>
          </Suspense>
        </div>
      </div>
    </div>
  );
}

function ContentCard({ title, body, colors, monospace = false, action }) {
  return (
    <div
      className="rounded-2xl p-4 border shadow"
      style={{ background: colors.bgCard, borderColor: colors.border }}
    >
      <div className="flex items-center justify-between mb-2">
        <div className="text-sm font-semibold" style={{ color: colors.textMain }}>
          {title}
        </div>
        {action && <div className="ml-2 flex-shrink-0">{action}</div>}
      </div>
      <SimpleMarkdown content={body} colors={colors} monospace={monospace} />
    </div>
  );
}

function normalizeContent(value) {
  if (typeof value === 'string') {
    const cleaned = cleanText(value);
    const lines = cleaned.split('\n').map((l) => l.trim()).filter(Boolean);
    if (lines.length <= 1) return cleaned;
    return lines.map((line) => `- ${line}`).join('\n');
  }
  if (value == null) return '';
  if (Array.isArray(value)) {
    const allStrings = value.every((item) => typeof item === 'string');
    if (allStrings) {
      const bullets = [];
      value.forEach((item) => {
        const lines = cleanText(item).split('\n').map((l) => l.trim()).filter(Boolean);
        lines.forEach((line) => bullets.push(`- ${line}`));
      });
      return bullets.join('\n');
    }
    return '```json\n' + JSON.stringify(value, null, 2) + '\n```';
  }
  if (typeof value === 'object') {
    return '```json\n' + JSON.stringify(value, null, 2) + '\n```';
  }
  return String(value);
}

function cleanText(text) {
  if (typeof text !== 'string') return String(text ?? '');
  let unescaped = text.replace(/\\n/g, '\n');
  unescaped = unescaped.replace(/\\"/g, '"');
  const normalized = unescaped.replace(/\r\n/g, '\n');
  const lines = normalized
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean);
  return lines.join('\n');
}

function renderFormattedText(text, colors) {
  if (!text) return null;
  // Handle backtick code, quoted strings, angle-bracket wrapped text, and bold text
  const parts = text.split(/(`[^`]+`|"[^"]*"|<[^>]+>|\*\*[^*]+\*\*)/g);

  return parts.map((part, index) => {
    if (part.startsWith('`') && part.endsWith('`')) {
      return (
        <code
          key={index}
          style={{
            background: colors.bgPanel,
            padding: '2px 5px',
            borderRadius: '4px',
            fontFamily: 'monospace',
            fontSize: '0.9em',
            color: colors.textMain,
            border: `1px solid ${colors.border}`,
            margin: '0 2px',
          }}
        >
          {part.slice(1, -1)}
        </code>
      );
    }
    if (part.startsWith('"') && part.endsWith('"')) {
      const content = part.slice(1, -1).replace(/\\"/g, '"');
      return (
        <code
          key={index}
          style={{
            background: colors.bgPanel,
            padding: '2px 5px',
            borderRadius: '4px',
            fontFamily: 'monospace',
            fontSize: '0.9em',
            color: colors.textMain,
            border: `1px solid ${colors.border}`,
            margin: '0 2px',
          }}
        >
          {content}
        </code>
      );
    }
    if (part.startsWith('<') && part.endsWith('>')) {
      const content = part.slice(1, -1);
      return (
        <code
          key={index}
          style={{
            background: colors.bgPanel,
            padding: '2px 5px',
            borderRadius: '4px',
            fontFamily: 'monospace',
            fontSize: '0.9em',
            color: colors.textMain,
            border: `1px solid ${colors.border}`,
            margin: '0 2px',
          }}
        >
          &lt;{content}&gt;
        </code>
      );
    }
    if (part.startsWith('**') && part.endsWith('**')) {
      const content = part.slice(2, -2);
      return (
        <strong key={index} style={{ color: colors.textMain, fontWeight: 'bold' }}>
          {content}
        </strong>
      );
    }
    return part;
  });
}

function highlightLiteral(text) {
  if (typeof text !== 'string') return text;
  const target = 'Welcome to Java! This is version ';
  if (!text.includes(target.trim())) return text;
  if (text.includes('`Welcome to Java! This is version')) return text;
  return text.replace(/Welcome to Java! This is version ?/g, (match) => `\`${match}\``);
}

function SimpleMarkdown({ content, monospace, colors }) {
  if (!content) return null;

  const lines = content.split('\n');
  const elements = [];
  let inCodeBlock = false;
  let codeBlockContent = [];

  lines.forEach((line, index) => {
    if (line.trim().startsWith('```')) {
      if (inCodeBlock) {
        elements.push(
          <div
            key={`code-${index}`}
            className="my-3 p-3 rounded-lg overflow-x-auto"
            style={{ background: 'rgba(0,0,0,0.3)', fontFamily: 'monospace' }}
          >
            <pre className="text-sm text-gray-300">{codeBlockContent.join('\n')}</pre>
          </div>
        );
        codeBlockContent = [];
        inCodeBlock = false;
      } else {
        inCodeBlock = true;
      }
      return;
    }

    if (inCodeBlock) {
      codeBlockContent.push(line);
      return;
    }

    const lineWithHighlight = highlightLiteral(line);
    const trimmed = lineWithHighlight.trim();

    if (lineWithHighlight.startsWith('# ')) {
      elements.push(
        <h1 key={index} className="text-xl font-bold mt-4 mb-2">
          {renderFormattedText(lineWithHighlight.substring(2), colors)}
        </h1>
      );
      return;
    }

    if (lineWithHighlight.startsWith('## ')) {
      elements.push(
        <h2 key={index} className="text-lg font-bold mt-3 mb-2">
          {renderFormattedText(lineWithHighlight.substring(3), colors)}
        </h2>
      );
      return;
    }

    const boldMatch = trimmed.match(/^\*\*(.+?)\*\*\s*(.*)$/);
    if (boldMatch) {
      const boldText = boldMatch[1];
      const rest = boldMatch[2];
      elements.push(
        <p
          key={index}
          className="mb-2 text-sm font-semibold"
          style={{ color: colors.textMain, lineHeight: 1.6 }}
        >
          <span className="font-bold">{renderFormattedText(boldText, colors)}</span>
          {rest ? ' ' : ''}
          {renderFormattedText(rest, colors)}
        </p>
      );
      return;
    }

    const stepsMatch = trimmed.match(/^-?\s*\*\*Steps:\*\*\s*(.*)$/i);
    if (stepsMatch) {
      const rest = stepsMatch[1].trim();
      elements.push(
        <p
          key={index}
          className="mb-2 text-sm font-semibold"
          style={{ color: colors.textMain, lineHeight: 1.6 }}
        >
          <span className="font-bold">Steps:</span>
          {rest ? ' ' : ''}
          {renderFormattedText(rest, colors)}
        </p>
      );
      return;
    }

    if (trimmed.startsWith('- ')) {
      elements.push(
        <li key={index} className="ml-4 list-none mb-1" style={{ color: colors.textMain }}>
          {renderFormattedText(trimmed.substring(2), colors)}
        </li>
      );
      return;
    }

    if (trimmed === '') {
      return;
    }

    elements.push(
      <p
        key={index}
        className={`mb-2 ${monospace ? 'font-mono text-xs' : 'text-sm'}`}
        style={{ color: colors.textMain, lineHeight: 1.6 }}
      >
        {renderFormattedText(lineWithHighlight, colors)}
      </p>
    );
  });

  return <div>{elements}</div>;
}

export default ProblemPage;
