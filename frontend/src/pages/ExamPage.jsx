import React, { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Clock, ChevronRight, CheckCircle2, XCircle, Brain, Target, ShieldAlert, Maximize2, AlertOctagon, LogOut, Hash, Lock } from 'lucide-react';
import examApi from '../services/examApi';
import Spinner from '../components/ui/Spinner/Spinner';
import ErrorMessage from '../components/ui/ErrorMessage/ErrorMessage';
import { useExamIntegrity } from '../hooks/useExamIntegrity';
import { ROUTES } from '../constants/appConstants';

const ExamPage = () => {
  const { sessionId } = useParams();
  const navigate = useNavigate();
  
  // State
  const [question, setQuestion] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedOption, setSelectedOption] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback] = useState(null);
  const [timeLeft, setTimeLeft] = useState(60);
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
    isFullscreen,
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
      setTimeLeft(response.data.timeLimit || 60);
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
        }, 2000);
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

  const handleExit = async () => {
    if (window.confirm('Are you sure you want to end the exam early? This will finalize your current score.')) {
      setExiting(true);
      try {
        await examApi.terminateSession(sessionId, 'User manually exited the exam');
        // Navigate back, fallback to courses if no history
        if (window.history.length > 1) {
          navigate(-1);
        } else {
          navigate(ROUTES.COURSES);
        }
      } catch (err) {
        console.error('Failed to exit exam:', err);
        setExiting(false);
      }
    }
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
            <h1 className="text-2xl font-bold">Secure Exam Environment</h1>
            <p className="text-text-secondary text-sm">
              To ensure assessment integrity, this exam will run in <b>fullscreen mode</b>. 
              Switching tabs, minimizing the window, or exiting fullscreen will result in a violation strike.
            </p>
          </div>
          <button onClick={handleStartExam} className="btn btn-primary w-full py-4 text-base gap-3">
            <Maximize2 size={18} />
            Enter Fullscreen & Start
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
            <h1 className="text-2xl font-bold text-text-primary">Exam Terminated</h1>
            <p className="text-text-secondary text-sm">
              Your session has been ended due to multiple integrity violations:
            </p>
            <div className="p-3 bg-interactive-hover rounded-lg text-xs font-mono text-accent mt-2">
              {terminationReason}
            </div>
          </div>
          <button 
            onClick={() => navigate(ROUTES.RESULT.replace(':sessionId', sessionId))} 
            className="btn btn-primary w-full py-4 text-base"
          >
            View Partial Result
          </button>
        </div>
      </div>
    );
  }

  if (loading && !question) return <Spinner />;
  if (error) return <ErrorMessage message={error} />;
  if (!question) return null;

  const totalQuestions = 20;

  return (
    <div className="animate-page-enter">
      {showWarning && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="panel max-w-sm w-full p-8 text-center animate-page-enter shadow-2xl border-accent">
            <ShieldAlert size={48} className="text-accent mx-auto mb-4" />
            <h2 className="text-xl font-bold mb-2">Integrity Warning</h2>
            <p className="text-text-secondary text-sm mb-6 leading-relaxed">
              We detected a focus change or navigation event: <br/>
              <span className="font-semibold text-text-primary">"{warningReason}"</span>. <br/>
              Remaining strikes: <span className="text-accent font-bold">{2 - violationCount}</span>.
            </p>
            <button onClick={dismissWarning} className="btn btn-primary w-full py-3">
              I Understand, Resume Exam
            </button>
          </div>
        </div>
      )}

      {/* HEADER META ACCURATE TO IMAGE */}
      <div className="exam-header-meta">
        <div className="exam-meta-group">
          <span className="exam-meta-label">Progress</span>
          <span className="exam-meta-value">#Question {question.index} / {totalQuestions}</span>
        </div>
        
        <div className="exam-meta-group">
          <span className="exam-meta-label">Time Left</span>
          <span className="exam-meta-value">
            <Clock size={16} />
            {timeLeft}s
          </span>
        </div>

        <div className="exam-meta-group">
           <span className="exam-meta-value">
              <Target size={16} />
              Adaptive Signal
           </span>
        </div>

        <button onClick={handleExit} className="exit-btn" disabled={exiting}>
          <LogOut size={14} />
          {exiting ? 'Exiting...' : 'Exit'}
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
               // Optional: style correctly based on feedback
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
              Adaptive Insight
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
                {feedback.sessionComplete ? 'View Proficiency Report' : 'Next Question'}
                <ChevronRight size={18} />
              </button>
           </div>
        )}
      </main>

      <div className="security-footer">
        <Lock size={18} />
        <span>Security Layer: {violationCount > 0 ? `${violationCount} Violation(s)` : 'Active'}</span>
      </div>
    </div>
  );
};

export default ExamPage;
