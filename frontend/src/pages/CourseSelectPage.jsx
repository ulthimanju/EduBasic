import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, BookOpen, GraduationCap, Layers3, Sparkles } from 'lucide-react';
import examApi from '../services/examApi';
import Spinner from '../components/ui/Spinner/Spinner';
import ErrorMessage from '../components/ui/ErrorMessage/ErrorMessage';

const CourseSelectPage = () => {
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [startingCourseId, setStartingCourseId] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchCourses = async () => {
      try {
        const response = await examApi.getCourses();
        setCourses(response.data);
      } catch (err) {
        setLoadError('Failed to load courses');
      } finally {
        setLoading(false);
      }
    };
    fetchCourses();
  }, []);

  const summary = useMemo(() => {
    const totalTopics = courses.reduce((count, course) => count + course.topics.length, 0);
    const uniqueTopics = new Set(courses.flatMap((course) => course.topics)).size;

    return {
      totalCourses: courses.length,
      totalTopics,
      uniqueTopics,
    };
  }, [courses]);

  const handleStartExam = async (courseId) => {
    setActionError(null);
    setStartingCourseId(courseId);

    try {
      const response = await examApi.startExam(courseId);
      navigate(`/exam/${response.data.id}`);
    } catch (err) {
      setActionError('Failed to start exam. Please try again.');
      setStartingCourseId(null);
    }
  };

  if (loading) return <Spinner />;
  if (loadError) return <ErrorMessage message={loadError} />;

  return (
    <section className="dashboard dashboard--wide course-select page-enter">
      <header className="course-select__hero panel">
        <div className="course-select__hero-main">
          <div className="course-select__eyebrow">
            <div className="login-logo" aria-hidden="true">
              <GraduationCap size={20} />
            </div>
            <span>Assessment Library</span>
          </div>

          <div className="course-select__headline">
            <h1 className="dashboard-hero__greeting">Pick a course and start with a clearer benchmark</h1>
            <p className="dashboard-hero__subtitle">
              Each assessment adapts as you answer, so the first choice should be the subject you want the
              most accurate signal on right now.
            </p>
          </div>

          <div className="course-select__highlights" aria-label="Assessment highlights">
            <div className="course-select__highlight">
              <Sparkles size={16} strokeWidth={1.75} />
              <span>Adaptive difficulty</span>
            </div>
            <div className="course-select__highlight">
              <BookOpen size={16} strokeWidth={1.75} />
              <span>Topic-based coverage</span>
            </div>
            <div className="course-select__highlight">
              <Layers3 size={16} strokeWidth={1.75} />
              <span>Fast placement signal</span>
            </div>
          </div>
        </div>

        <aside className="course-select__hero-panel" aria-label="Course overview">
          <p className="course-select__panel-label">Available now</p>
          <div className="course-select__stats">
            <div className="course-select__stat">
              <span className="course-select__stat-value">{summary.totalCourses}</span>
              <span className="course-select__stat-label">courses</span>
            </div>
            <div className="course-select__stat">
              <span className="course-select__stat-value">{summary.totalTopics}</span>
              <span className="course-select__stat-label">topic tags</span>
            </div>
            <div className="course-select__stat">
              <span className="course-select__stat-value">{summary.uniqueTopics}</span>
              <span className="course-select__stat-label">unique areas</span>
            </div>
          </div>
          <p className="course-select__panel-note">
            Choose the card that best matches your next exam goal. You can start a new assessment in one click.
          </p>
        </aside>
      </header>

      {actionError && <ErrorMessage message={actionError} />}

      {courses.length === 0 ? (
        <section className="empty-state panel">
          <BookOpen className="empty-state__icon" aria-hidden="true" />
          <h2 className="empty-state__title">No courses are available yet</h2>
          <p className="empty-state__text">
            Course assessments will appear here as soon as they are published by the exam service.
          </p>
        </section>
      ) : (
        <section className="course-select__grid" aria-label="Available courses">
          {courses.map((course, index) => {
            const visibleTopics = course.topics.slice(0, 4);
            const hiddenTopicCount = Math.max(course.topics.length - visibleTopics.length, 0);
            const isStarting = startingCourseId === course.id;

            return (
              <article key={course.id} className="course-card panel">
                <div className="course-card__eyebrow">
                  <span className="course-card__badge">Course {index + 1}</span>
                  <span className="course-card__meta">{course.topics.length} topics</span>
                </div>

                <div className="course-card__header">
                  <div className="dashboard-card__icon" aria-hidden="true">
                    <BookOpen size={18} strokeWidth={1.5} />
                  </div>

                  <div className="course-card__heading">
                    <h2 className="dashboard-card__title">{course.name}</h2>
                    <p className="course-card__description">
                      Adaptive questions across the core concepts most likely to shape your proficiency level.
                    </p>
                  </div>
                </div>

                <div className="course-card__topics" aria-label={`${course.name} topics`}>
                  {visibleTopics.map((topic) => (
                    <span key={topic} className="course-card__topic">
                      {topic}
                    </span>
                  ))}
                  {hiddenTopicCount > 0 && (
                    <span className="course-card__topic course-card__topic--muted">+{hiddenTopicCount} more</span>
                  )}
                </div>

                <div className="course-card__footer">
                  <p className="course-card__footnote">
                    Best when you want a fast read on where to focus your next round of study.
                  </p>

                  <button
                    onClick={() => handleStartExam(course.id)}
                    className="btn btn-primary course-card__action"
                    disabled={startingCourseId !== null}
                  >
                    <span>{isStarting ? 'Starting...' : 'Start Assessment'}</span>
                    <ArrowRight size={16} strokeWidth={1.75} />
                  </button>
                </div>
              </article>
            );
          })}
        </section>
      )}
    </section>
  );
};

export default CourseSelectPage;
