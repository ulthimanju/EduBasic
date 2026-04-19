import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, BookOpen, GraduationCap, Layers3, Sparkles } from 'lucide-react';
import examApi from '../services/examApi';
import Spinner from '../components/ui/Spinner/Spinner';
import ErrorMessage from '../components/ui/ErrorMessage/ErrorMessage';
import { COURSE_SELECT_CONTENT } from '../content/pageContent';
import { COURSE_CONFIG } from '../config/pageConfig';
import { getCourseTopicsInfo } from '../utils/viewModels';

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

  const getIcon = (iconName) => {
    switch (iconName) {
      case 'Sparkles': return <Sparkles size={16} strokeWidth={1.75} />;
      case 'BookOpen': return <BookOpen size={16} strokeWidth={1.75} />;
      case 'Layers3': return <Layers3 size={16} strokeWidth={1.75} />;
      default: return null;
    }
  };

  return (
    <section className="dashboard dashboard--wide course-select page-enter">
      <header className="course-select__hero panel">
        <div className="course-select__hero-main">
          <div className="course-select__eyebrow">
            <div className="login-logo" aria-hidden="true">
              <GraduationCap size={20} />
            </div>
            <span>{COURSE_SELECT_CONTENT.HERO.EYEBROW}</span>
          </div>

          <div className="course-select__headline">
            <h1 className="dashboard-hero__greeting">{COURSE_SELECT_CONTENT.HERO.HEADLINE}</h1>
            <p className="dashboard-hero__subtitle">
              {COURSE_SELECT_CONTENT.HERO.SUBTITLE}
            </p>
          </div>

          <div className="course-select__highlights" aria-label="Assessment highlights">
            {COURSE_SELECT_CONTENT.HIGHLIGHTS.map((highlight) => (
              <div key={highlight.label} className="course-select__highlight">
                {getIcon(highlight.icon)}
                <span>{highlight.label}</span>
              </div>
            ))}
          </div>
        </div>

        <aside className="course-select__hero-panel" aria-label="Course overview">
          <p className="course-select__panel-label">{COURSE_SELECT_CONTENT.ASIDE.LABEL}</p>
          <div className="course-select__stats">
            <div className="course-select__stat">
              <span className="course-select__stat-value">{summary.totalCourses}</span>
              <span className="course-select__stat-label">{COURSE_SELECT_CONTENT.ASIDE.STATS.COURSES}</span>
            </div>
            <div className="course-select__stat">
              <span className="course-select__stat-value">{summary.totalTopics}</span>
              <span className="course-select__stat-label">{COURSE_SELECT_CONTENT.ASIDE.STATS.TOPIC_TAGS}</span>
            </div>
            <div className="course-select__stat">
              <span className="course-select__stat-value">{summary.uniqueTopics}</span>
              <span className="course-select__stat-label">{COURSE_SELECT_CONTENT.ASIDE.STATS.UNIQUE_AREAS}</span>
            </div>
          </div>
          <p className="course-select__panel-note">
            {COURSE_SELECT_CONTENT.ASIDE.NOTE}
          </p>
        </aside>
      </header>

      {actionError && <ErrorMessage message={actionError} />}

      {courses.length === 0 ? (
        <section className="empty-state panel">
          <BookOpen className="empty-state__icon" aria-hidden="true" />
          <h2 className="empty-state__title">{COURSE_SELECT_CONTENT.EMPTY_STATE.TITLE}</h2>
          <p className="empty-state__text">
            {COURSE_SELECT_CONTENT.EMPTY_STATE.TEXT}
          </p>
        </section>
      ) : (
        <section className="course-select__grid" aria-label="Available courses">
          {courses.map((course, index) => {
            const { visibleTopics, hiddenCount } = getCourseTopicsInfo(course.topics, COURSE_CONFIG.TOPIC_PREVIEW_LIMIT);
            const isStarting = startingCourseId === course.id;

            return (
              <article key={course.id} className="course-card panel">
                <div className="course-card__eyebrow">
                  <span className="course-card__badge">{COURSE_SELECT_CONTENT.CARD.BADGE_PREFIX} {index + 1}</span>
                  <span className="course-card__meta">{course.topics.length} topics</span>
                </div>

                <div className="course-card__header">
                  <div className="dashboard-card__icon" aria-hidden="true">
                    <BookOpen size={18} strokeWidth={1.5} />
                  </div>

                  <div className="course-card__heading">
                    <h2 className="dashboard-card__title">{course.name}</h2>
                    <p className="course-card__description">
                      {COURSE_SELECT_CONTENT.CARD.DESCRIPTION}
                    </p>
                  </div>
                </div>

                <div className="course-card__topics" aria-label={`${course.name} topics`}>
                  {visibleTopics.map((topic) => (
                    <span key={topic} className="course-card__topic">
                      {topic}
                    </span>
                  ))}
                  {hiddenCount > 0 && (
                    <span className="course-card__topic course-card__topic--muted">+{hiddenCount} {COURSE_SELECT_CONTENT.CARD.TOPICS_MORE}</span>
                  )}
                </div>

                <div className="course-card__footer">
                  <p className="course-card__footnote">
                    {COURSE_SELECT_CONTENT.CARD.FOOTNOTE}
                  </p>

                  <button
                    onClick={() => handleStartExam(course.id)}
                    className="btn btn-primary course-card__action"
                    disabled={startingCourseId !== null}
                  >
                    <span>{isStarting ? COURSE_SELECT_CONTENT.CARD.ACTION_STARTING : COURSE_SELECT_CONTENT.CARD.ACTION}</span>
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
