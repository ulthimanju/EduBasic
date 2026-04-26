import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, BookOpen, GraduationCap, Layers3, Sparkles, Clock, FileText } from 'lucide-react';
import examApi from '../services/examApi';
import Spinner from '../components/ui/Spinner/Spinner';
import ErrorMessage from '../components/ui/ErrorMessage/ErrorMessage';
import { ROUTES } from '../constants/appConstants';

const CourseSelectPage = () => {
  const [exams, setExams] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [startingExamId, setStartingExamId] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchExams = async () => {
      try {
        const response = await examApi.getExams({ status: 'PUBLISHED' });
        setExams(response.data);
      } catch (err) {
        setLoadError('Failed to load available exams');
      } finally {
        setLoading(false);
      }
    };
    fetchExams();
  }, []);

  const summary = useMemo(() => {
    return {
      totalExams: exams.length,
      sectioned: exams.filter(e => e.hasSections).length,
      flat: exams.filter(e => !e.hasSections).length,
    };
  }, [exams]);

  const handleStartExam = async (examId) => {
    setActionError(null);
    setStartingExamId(examId);

    try {
      const response = await examApi.startAttempt(examId);
      navigate(ROUTES.EXAM.replace(':attemptId', response.data.id));
    } catch (err) {
      setActionError('Failed to start exam attempt. Please try again.');
      setStartingExamId(null);
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
            <span>Academic Portal</span>
          </div>

          <div className="course-select__headline">
            <h1 className="dashboard-hero__greeting">Available Assessments</h1>
            <p className="dashboard-hero__subtitle">
              Choose an exam to begin. Your progress is saved automatically during the session.
            </p>
          </div>

          <div className="course-select__highlights">
            <div className="course-select__highlight">
              <Sparkles size={16} />
              <span>Real-time proctoring</span>
            </div>
            <div className="course-select__highlight">
              <Clock size={16} />
              <span>Timed sessions</span>
            </div>
          </div>
        </div>

        <aside className="course-select__hero-panel">
          <p className="course-select__panel-label">Session Overview</p>
          <div className="course-select__stats">
            <div className="course-select__stat">
              <span className="course-select__stat-value">{summary.totalExams}</span>
              <span className="course-select__stat-label">Exams</span>
            </div>
            <div className="course-select__stat">
              <span className="course-select__stat-value">{summary.sectioned}</span>
              <span className="course-select__stat-label">Sectioned</span>
            </div>
          </div>
        </aside>
      </header>

      {actionError && <ErrorMessage message={actionError} />}

      {exams.length === 0 ? (
        <section className="empty-state panel">
          <FileText className="empty-state__icon" />
          <h2 className="empty-state__title">No active exams</h2>
          <p className="empty-state__text">
            There are currently no published exams available for you to take.
          </p>
        </section>
      ) : (
        <section className="course-select__grid">
          {exams.map((exam, index) => {
            const isStarting = startingExamId === exam.id;

            return (
              <article key={exam.id} className="course-card panel">
                <div className="course-card__eyebrow">
                  <span className="course-card__badge">EXAM {index + 1}</span>
                  <span className="course-card__meta">{exam.hasSections ? 'Sectioned' : 'Flat'}</span>
                </div>

                <div className="course-card__header">
                  <div className="dashboard-card__icon">
                    <BookOpen size={18} strokeWidth={1.5} />
                  </div>

                  <div className="course-card__heading">
                    <h2 className="dashboard-card__title">{exam.title}</h2>
                    <p className="course-card__description">
                      {exam.description || 'No description provided.'}
                    </p>
                  </div>
                </div>

                <div className="course-card__footer">
                  <div style={{ display: 'flex', gap: 'var(--space-4)', color: 'var(--color-text-secondary)', fontSize: 'var(--text-xs)' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-1)' }}>
                      <Clock size={14} />
                      <span>{exam.timeLimitMins || 'No limit'} mins</span>
                    </div>
                  </div>

                  <button
                    onClick={() => handleStartExam(exam.id)}
                    className="btn btn-primary course-card__action"
                    disabled={startingExamId !== null}
                  >
                    <span>{isStarting ? 'Starting...' : 'Begin Assessment'}</span>
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
