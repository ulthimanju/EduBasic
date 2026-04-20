import React, { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Clock, ChevronRight, Brain, Target, ShieldAlert, Maximize2, AlertOctagon, LogOut, Lock } from 'lucide-react';
import examApi from '../services/examApi';
import Spinner from '../components/ui/Spinner/Spinner';
import ErrorMessage from '../components/ui/ErrorMessage/ErrorMessage';
import { useExamIntegrity } from '../hooks/useExamIntegrity';
import { ROUTES } from '../constants/appConstants';
import { usePrompt } from '../context/PromptContext';
import { EXAM_CONFIG } from '../config/pageConfig';
import { EXAM_CONTENT } from '../content/pageContent';

const ExamPage = () => {
  const { sessionId } = useParams();
  const navigate = useNavigate();
  const { openPrompt } = usePrompt();

  // State
  const [question, setQuestion] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedOption, setSelectedOption] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback] = useState(null);
  const [timeLeft, setTimeLeft] = useState(EXAM_CONFIG.DEFAULT_TIME_LIMIT);
  const [examStarted, setExamStarted] = useState(false);
  const [isTerminated, setIsTerminated] = useState(false);
  const [terminationReason, setTerminationReason] = useState('');
  const [exiting, setExiting] = useState(false);

  // Refs
  const timerRef = useRef(null);
  const startTimeRef = useRef(null);
  const autoNextTimeoutRef = useRef(null);

  // Integrity Hook
  const { 
    violationCount, 
    showWarning, 
    warningReason, 
    dismissWarning, 
    enterFullscreen 
  } = useExamIntegrity(sessionId, (reason) => {
    setIsTerminated(true);
    setTerminationReason(reason);
    if (timerRef.current) clearInterval(timerRef.current);
  }, examStarted);

  useEffect(() => {
    if (examStarted && !isTerminated) {
      fetchQuestion();
    }
    return () => {
      if (autoNextTimeoutRef.current) clearTimeout(autoNextTimeoutRef.current);
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [sessionId, examStarted, isTerminated]);

  const fetchQuestion = async () => {
    if (autoNextTimeoutRef.current) clearTimeout(autoNextTimeoutRef.current);
    setLoading(true);
    setError(null);
    setFeedback(null);
    setSelectedOption(null);
    try {
      const response = await examApi.getQuestion(sessionId);
      setQuestion(response.data);
      setTimeLeft(response.data.timeLimit || EXAM_CONFIG.DEFAULT_TIME_LIMIT);
      startTimeRef.current = Date.now();
      startTimer();
    } catch (err) {
      setError('Failed to load question');
    } finally {
      setLoading(false);
    }
  };

  const startTimer = () => {
    if (timerRef.current) clearInterval(timerRef.current);
    timerRef.current = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          clearInterval(timerRef.current);
          handleTimeout();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  };

  const handleTimeout = () => {
    if (!feedback && !isTerminated) {
      handleSubmit('NONE');
    }
  };

  const handleSubmit = async (option) => {
    if (submitting || feedback || isTerminated) return;
    setSubmitting(true);
    setSelectedOption(option);
    if (timerRef.current) clearInterval(timerRef.current);
    const timeTaken = Math.floor((Date.now() - startTimeRef.current) / 1000);

    try {
      const response = await examApi.submitAnswer(sessionId, {
        questionId: question.questionId,
        selectedOption: option || 'NONE',
        timeTaken,
      });
      setFeedback(response.data);
      setSubmitting(false);

      if (!response.data.sessionComplete) {
        autoNextTimeoutRef.current = setTimeout(() => {
          if (!isTerminated) fetchQuestion();
        }, EXAM_CONFIG.AUTO_NEXT_DELAY_MS);
      }
    } catch (err) {
      setError('Failed to submit answer');
      setSubmitting(false);
    }
  };

  const handleNext = () => {
    if (autoNextTimeoutRef.current) clearTimeout(autoNextTimeoutRef.current);
    if (feedback?.sessionComplete) {
      navigate(ROUTES.RESULT.replace(':sessionId', sessionId));
    } else {
      fetchQuestion();
    }
  };

  const handleExit = () => {
    openPrompt({
      type: 'confirm',
      severity: 'warning',
      title: EXAM_CONTENT.EXIT_PROMPT.TITLE,
      description: EXAM_CONTENT.EXIT_PROMPT.DESCRIPTION,
      confirmLabel: EXAM_CONTENT.EXIT_PROMPT.CONFIRM,
      cancelLabel: EXAM_CONTENT.EXIT_PROMPT.CANCEL,
      onConfirm: async () => {
        setExiting(true);
        try {
          await examApi.terminateSession(sessionId, 'User manually exited the exam');
          if (window.history.length > 1) {
            navigate(-1);
          } else {
            navigate(ROUTES.COURSES);
          }
        } catch (err) {
          console.error('Failed to exit exam:', err);
          setExiting(false);
          openPrompt({
            type: 'message',
            severity: 'danger',
            title: EXAM_CONTENT.EXIT_ERROR.TITLE,
            description: EXAM_CONTENT.EXIT_ERROR.DESCRIPTION,
            confirmLabel: EXAM_CONTENT.EXIT_ERROR.CONFIRM
          });
        }
      }
    });
  };

  const handleStartExam = () => {
    enterFullscreen();
    setExamStarted(true);
  };

  if (!examStarted) {
    return (
      <div className="exam-state-screen page-enter">
        <section className="exam-state-card">
          <div className="exam-state-card__icon">
            <ShieldAlert size={32} />
          </div>
          <span className="exam-state-card__eyebrow">Integrity Mode</span>
          <div className="exam-state-card__content">
            <h1 className="exam-state-card__title">{EXAM_CONTENT.ENVIRONMENT.TITLE}</h1>
            <p
              className="exam-state-card__description"
              dangerouslySetInnerHTML={{ __html: EXAM_CONTENT.ENVIRONMENT.DESCRIPTION }}
            />
          </div>
          <button onClick={handleStartExam} className="btn btn-primary exam-state-card__action">
            <Maximize2 size={18} />
            {EXAM_CONTENT.ENVIRONMENT.START_BTN}
          </button>
        </section>
      </div>
    );
  }

  if (isTerminated) {
    return (
      <div className="exam-state-screen page-enter">
        <section className="exam-state-card exam-state-card--terminated">
          <div className="exam-state-card__icon exam-state-card__icon--danger">
            <AlertOctagon size={32} />
          </div>
          <span className="exam-state-card__eyebrow">Session Locked</span>
          <div className="exam-state-card__content">
            <h1 className="exam-state-card__title">{EXAM_CONTENT.TERMINATED.TITLE}</h1>
            <p className="exam-state-card__description">
              {EXAM_CONTENT.TERMINATED.DESCRIPTION}
            </p>
            <div className="exam-state-card__reason">
              {terminationReason}
            </div>
          </div>
          <button
            onClick={() => navigate(ROUTES.RESULT.replace(':sessionId', sessionId))}
            className="btn btn-primary exam-state-card__action"
          >
            {EXAM_CONTENT.TERMINATED.ACTION}
          </button>
        </section>
      </div>
    );
  }

  if (loading && !question) return <Spinner />;
  if (error) return <ErrorMessage message={error} />;
  if (!question) return null;

  const totalQuestions = question.totalQuestions || EXAM_CONFIG.DEFAULT_TOTAL_QUESTIONS;
  const safeIndex = question.index || 1;
  const progressPercent = Math.max(0, Math.min(100, Math.round((safeIndex / totalQuestions) * 100)));
  const remainingStrikes = Math.max(0, EXAM_CONFIG.MAX_STRIKES - violationCount);

  return (
    <div className="exam-layout animate-page-enter">
      {showWarning && (
        <div className="exam-warning-overlay">
          <div className="exam-warning-dialog animate-page-enter">
            <ShieldAlert size={36} className="exam-warning-dialog__icon" />
            <h2 className="exam-warning-dialog__title">{EXAM_CONTENT.WARNING.TITLE}</h2>
            <p className="exam-warning-dialog__description">
              {EXAM_CONTENT.WARNING.DESCRIPTION_PREFIX}{' '}
              <span className="exam-warning-dialog__reason">"{warningReason}"</span>.{' '}
              {EXAM_CONTENT.WARNING.DESCRIPTION_STRIKES}{' '}
              <span className="exam-warning-dialog__strikes">{remainingStrikes}</span>.
            </p>
            <button onClick={dismissWarning} className="btn btn-primary exam-warning-dialog__action">
              {EXAM_CONTENT.WARNING.ACTION}
            </button>
          </div>
        </div>
      )}

      <header className="exam-toolbar">
        <div className="exam-toolbar__progress">
          <div className="exam-toolbar__headline">
            <span className="exam-meta-label">{EXAM_CONTENT.HEADER.PROGRESS}</span>
            <span className="exam-meta-value">
              Question {safeIndex} / {totalQuestions}
            </span>
          </div>
          <div className="exam-progress-track" role="progressbar" aria-valuenow={progressPercent} aria-valuemin={0} aria-valuemax={100}>
            <div className="exam-progress-fill" style={{ width: `${progressPercent}%` }} />
          </div>
        </div>

        <div className="exam-toolbar__status">
          <div className="exam-status-chip">
            <Clock size={16} />
            <div>
              <span className="exam-status-chip__label">{EXAM_CONTENT.HEADER.TIME_LEFT}</span>
              <strong>{timeLeft}s</strong>
            </div>
          </div>

          <div className="exam-status-chip">
            <Target size={16} />
            <div>
              <span className="exam-status-chip__label">Mode</span>
              <strong>{EXAM_CONTENT.HEADER.ADAPTIVE_SIGNAL}</strong>
            </div>
          </div>

          <button onClick={handleExit} className="exit-btn" disabled={exiting}>
            <LogOut size={14} />
            {exiting ? EXAM_CONTENT.HEADER.EXITING : EXAM_CONTENT.HEADER.EXIT}
          </button>
        </div>
      </header>

      <div className="exam-grid">
        <main className="question-panel">
          <h1 className="question-title">{question.question}</h1>
          <p className="question-subtitle">Choose the best answer. You can submit only once per question.</p>

          <div className="options-container">
            {question.options.map((option, index) => {
              const isSelected = selectedOption === option;
              const isCorrect = feedback?.correctAnswer === option;
              const isWrong = feedback && isSelected && !feedback.correct;

              let stateClass = isSelected ? 'is-selected' : '';
              if (feedback) {
                if (isCorrect) stateClass += ' is-correct';
                if (isWrong) stateClass += ' is-wrong';
              }

              return (
                <button
                  key={index}
                  disabled={!!feedback || submitting || showWarning}
                  onClick={() => handleSubmit(option)}
                  className={`option-item ${stateClass}`}
                >
                  <span className="option-item__label">{String.fromCharCode(65 + index)}</span>
                  <span className="option-item__text">{option}</span>
                  <span className="option-radio">
                    {isSelected && <span className="option-radio__dot" />}
                  </span>
                </button>
              );
            })}
          </div>

          {feedback && (
            <div className="feedback-panel animate-page-enter">
              <h3 className="feedback-panel__title">
                <Brain size={14} />
                {EXAM_CONTENT.QUESTION.ADAPTIVE_INSIGHT}
              </h3>
              <p className="feedback-panel__text">
                {feedback.explanation}
              </p>
            </div>
          )}

          {feedback && (
            <div className="exam-action-row">
              <button
                onClick={handleNext}
                className="btn btn-secondary exam-action-row__next"
              >
                {feedback.sessionComplete ? EXAM_CONTENT.QUESTION.RESULT_BTN : EXAM_CONTENT.QUESTION.NEXT_BTN}
                <ChevronRight size={18} />
              </button>
            </div>
          )}
        </main>

        <aside className="exam-side-panel">
          <div className="exam-side-card">
            <h2 className="exam-side-card__title">Session Control</h2>
            <p className="exam-side-card__text">
              Focus loss is tracked in real time. Keep this tab in fullscreen until completion.
            </p>
            <div className="exam-side-card__metric">
              <Lock size={16} />
              <span>
                {EXAM_CONTENT.SECURITY_FOOTER.PREFIX}{' '}
                {violationCount > 0 ? `${violationCount} ${EXAM_CONTENT.SECURITY_FOOTER.VIOLATIONS}` : EXAM_CONTENT.SECURITY_FOOTER.ACTIVE}
              </span>
            </div>
            <div className="exam-side-card__metric">
              <ShieldAlert size={16} />
              <span>{remainingStrikes} strike(s) remaining</span>
            </div>
          </div>
        </aside>
      </div>

      <footer className="security-footer">
        <Lock size={18} />
        <span>
          {EXAM_CONTENT.SECURITY_FOOTER.PREFIX}{' '}
          {violationCount > 0 ? `${violationCount} ${EXAM_CONTENT.SECURITY_FOOTER.VIOLATIONS}` : EXAM_CONTENT.SECURITY_FOOTER.ACTIVE}
        </span>
      </footer>
    </div>
  );
};

export default ExamPage;
