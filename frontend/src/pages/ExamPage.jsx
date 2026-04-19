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
      <div className="flex items-center justify-center min-h-[60vh] page-enter">
        <div className="panel max-w-md w-full p-10 text-center flex flex-col items-center gap-6">
          <div className="w-16 h-16 rounded-2xl bg-accent-subtle text-accent flex items-center justify-center">
            <ShieldAlert size={32} />
          </div>
          <div className="grid gap-2">
            <h1 className="text-2xl font-bold">{EXAM_CONTENT.ENVIRONMENT.TITLE}</h1>
            <p className="text-text-secondary text-sm" dangerouslySetInnerHTML={{ __html: EXAM_CONTENT.ENVIRONMENT.DESCRIPTION }} />
          </div>
          <button onClick={handleStartExam} className="btn btn-primary w-full py-4 text-base gap-3">
            <Maximize2 size={18} />
            {EXAM_CONTENT.ENVIRONMENT.START_BTN}
          </button>
        </div>
      </div>
    );
  }

  if (isTerminated) {
    return (
      <div className="flex items-center justify-center min-h-[60vh] page-enter">
        <div className="panel max-w-md w-full p-10 text-center flex flex-col items-center gap-6 border-accent/30 bg-accent/5">
          <div className="w-16 h-16 rounded-2xl bg-accent text-white flex items-center justify-center shadow-lg shadow-accent/20">
            <AlertOctagon size={32} />
          </div>
          <div className="grid gap-2">
            <h1 className="text-2xl font-bold text-text-primary">{EXAM_CONTENT.TERMINATED.TITLE}</h1>
            <p className="text-text-secondary text-sm">
              {EXAM_CONTENT.TERMINATED.DESCRIPTION}
            </p>
            <div className="p-3 bg-interactive-hover rounded-lg text-xs font-mono text-accent mt-2">
              {terminationReason}
            </div>
          </div>
          <button 
            onClick={() => navigate(ROUTES.RESULT.replace(':sessionId', sessionId))} 
            className="btn btn-primary w-full py-4 text-base"
          >
            {EXAM_CONTENT.TERMINATED.ACTION}
          </button>
        </div>
      </div>
    );
  }

  if (loading && !question) return <Spinner />;
  if (error) return <ErrorMessage message={error} />;
  if (!question) return null;

  return (
    <div className="animate-page-enter">
      {showWarning && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="panel max-w-sm w-full p-8 text-center animate-page-enter shadow-2xl border-accent">
            <ShieldAlert size={48} className="text-accent mx-auto mb-4" />
            <h2 className="text-xl font-bold mb-2">{EXAM_CONTENT.WARNING.TITLE}</h2>
            <p className="text-text-secondary text-sm mb-6 leading-relaxed">
              {EXAM_CONTENT.WARNING.DESCRIPTION_PREFIX} <br/>
              <span className="font-semibold text-text-primary">"{warningReason}"</span>. <br/>
              {EXAM_CONTENT.WARNING.DESCRIPTION_STRIKES} <span className="text-accent font-bold">{EXAM_CONFIG.MAX_STRIKES - violationCount}</span>.
            </p>
            <button onClick={dismissWarning} className="btn btn-primary w-full py-3">
              {EXAM_CONTENT.WARNING.ACTION}
            </button>
          </div>
        </div>
      )}

      {/* HEADER META ACCURATE TO IMAGE */}
      <div className="exam-header-meta">
        <div className="exam-meta-group">
          <span className="exam-meta-label">{EXAM_CONTENT.HEADER.PROGRESS}</span>
          <span className="exam-meta-value">#Question {question.index} / {question.totalQuestions || EXAM_CONFIG.DEFAULT_TOTAL_QUESTIONS}</span>
        </div>
        
        <div className="exam-meta-group">
          <span className="exam-meta-label">{EXAM_CONTENT.HEADER.TIME_LEFT}</span>
          <span className="exam-meta-value">
            <Clock size={16} />
            {timeLeft}s
          </span>
        </div>

        <div className="exam-meta-group">
           <span className="exam-meta-value">
              <Target size={16} />
              {EXAM_CONTENT.HEADER.ADAPTIVE_SIGNAL}
           </span>
        </div>

        <button onClick={handleExit} className="exit-btn" disabled={exiting}>
          <LogOut size={14} />
          {exiting ? EXAM_CONTENT.HEADER.EXITING : EXAM_CONTENT.HEADER.EXIT}
        </button>
      </div>

      <main className="question-panel">
        <h1 className="question-title">{question.question}</h1>

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
                <span className="option-item__text">{option}</span>
                <div className="option-radio">
                  {isSelected && <div className="option-radio__dot" />}
                </div>
              </button>
            );
          })}
        </div>

        {feedback && (
          <div className="feedback-panel panel animate-page-enter">
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
           <div className="mt-auto pt-8 flex justify-end">
              <button
                onClick={handleNext}
                className="btn btn-secondary !py-4 !px-8"
              >
                {feedback.sessionComplete ? EXAM_CONTENT.QUESTION.RESULT_BTN : EXAM_CONTENT.QUESTION.NEXT_BTN}
                <ChevronRight size={18} />
              </button>
           </div>
        )}
      </main>

      <div className="security-footer">
        <Lock size={18} />
        <span>{EXAM_CONTENT.SECURITY_FOOTER.PREFIX} {violationCount > 0 ? `${violationCount} ${EXAM_CONTENT.SECURITY_FOOTER.VIOLATIONS}` : EXAM_CONTENT.SECURITY_FOOTER.ACTIVE}</span>
      </div>
    </div>
  );
};

export default ExamPage;
