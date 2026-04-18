import React, { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Clock, ChevronRight, CheckCircle2, XCircle, Brain, Target, Hash } from 'lucide-react';
import examApi from '../services/examApi';
import Spinner from '../components/ui/Spinner/Spinner';
import ErrorMessage from '../components/ui/ErrorMessage/ErrorMessage';

const ExamPage = () => {
  const { sessionId } = useParams();
  const navigate = useNavigate();
  const [question, setQuestion] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedOption, setSelectedOption] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback] = useState(null);
  const [timeLeft, setTimeLeft] = useState(60);
  const timerRef = useRef(null);
  const startTimeRef = useRef(null);
  const autoNextTimeoutRef = useRef(null);

  useEffect(() => {
    fetchQuestion();
    return () => {
      if (autoNextTimeoutRef.current) clearTimeout(autoNextTimeoutRef.current);
    };
  }, [sessionId]);

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
    if (!feedback) {
      handleSubmit('NONE');
    }
  };

  const handleSubmit = async (option) => {
    if (submitting || feedback) return;
    setSubmitting(true);
    setSelectedOption(option);
    clearInterval(timerRef.current);
    const timeTaken = Math.floor((Date.now() - startTimeRef.current) / 1000);

    try {
      const response = await examApi.submitAnswer(sessionId, {
        questionId: question.questionId,
        selectedOption: option || 'NONE',
        timeTaken,
      });
      setFeedback(response.data);
      setSubmitting(false);

      // Auto-advance after 2 seconds
      autoNextTimeoutRef.current = setTimeout(() => {
        if (response.data.sessionComplete) {
          navigate(`/result/${sessionId}`);
        } else {
          fetchQuestion();
        }
      }, 2000);

    } catch (err) {
      setError('Failed to submit answer');
      setSubmitting(false);
    }
  };

  const handleNext = () => {
    if (autoNextTimeoutRef.current) clearTimeout(autoNextTimeoutRef.current);
    if (feedback?.sessionComplete) {
      navigate(`/result/${sessionId}`);
    } else {
      fetchQuestion();
    }
  };

  if (loading) return <Spinner />;
  if (error) return <ErrorMessage message={error} />;
  if (!question) return null;

  const totalQuestions = 20;
  const progress = Math.round((question.index / totalQuestions) * 100);
  const timerStateClass = timeLeft < 10 ? ' exam-timer__spotlight--urgent' : '';
  const answerState = feedback
    ? feedback.correct
      ? 'Correct'
      : 'Reviewing a miss'
    : submitting
      ? 'Analyzing response'
      : 'Awaiting answer';

  return (
    <div className="dashboard dashboard--wide exam-page animate-page-enter">
      <div className="exam-layout">
        <main className="exam-main">
          <section className="exam-question panel">
            <header className="exam-question__header">
              <div className="exam-question__eyebrow">
                <span className="exam-question__kicker">Adaptive assessment</span>
                <span className="exam-question__index">Question {question.index}</span>
              </div>
              <h1 className="exam-question__title">{question.question}</h1>
            </header>

            <div className="exam-options-grid">
              {question.options.map((option, index) => {
                const isSelected = selectedOption === option;
                const isCorrect = feedback?.correctAnswer === option;
                const isWrong = feedback && isSelected && !feedback.correct;

                let btnClass = isSelected ? 'is-selected' : '';
                if (feedback) {
                  if (isCorrect) btnClass = 'is-correct';
                  else if (isWrong) btnClass = 'is-wrong';
                }

                return (
                  <button
                    key={index}
                    disabled={!!feedback || submitting}
                    onClick={() => handleSubmit(option)}
                    className={`option-btn ${btnClass}`}
                  >
                    <span className="font-medium pr-4">{option}</span>
                    <div className="option-indicator">
                      {feedback && isCorrect && <CheckCircle2 size={14} className="text-white" />}
                      {feedback && isWrong && <XCircle size={14} className="text-white" />}
                      {!feedback && isSelected && <div className="option-dot" />}
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

            <div className="exam-actions">
              {submitting && (
                <div className="exam-actions__status">
                  <Spinner size="sm" />
                  <span>Analyzing response...</span>
                </div>
              )}
              {feedback && (
                <button
                  onClick={handleNext}
                  className="btn btn-secondary exam-actions__next"
                >
                  {feedback.sessionComplete ? 'View Proficiency Report' : 'Next Question'}
                  <ChevronRight size={18} />
                </button>
              )}
            </div>
          </section>
        </main>

        <aside className="exam-meta">
          <div className="exam-meta__card panel">
            <div className="exam-meta__header">
              <p className="exam-meta__label">Session status</p>
              <div className="exam-meta__pill">
                <Target size={12} />
                <span>Adaptive</span>
              </div>
            </div>

            <div className={`exam-timer__spotlight${timerStateClass}`}>
              <span className="exam-timer__label">
                <Clock size={12} />
                Time remaining
              </span>
              <div className="exam-timer__row">
                <span className="exam-timer__value">{timeLeft}</span>
                <span className="exam-timer__unit">sec</span>
              </div>
            </div>

            <div className="exam-meta__stats">
              <article className="exam-meta__stat">
                <span className="exam-meta__stat-label">
                  <Hash size={12} />
                  Question
                </span>
                <span className="exam-meta__stat-value">
                  {question.index} / {totalQuestions}
                </span>
              </article>

              <article className="exam-meta__stat">
                <span className="exam-meta__stat-label">Completion</span>
                <span className="exam-meta__stat-value">{progress}%</span>
              </article>

              <article className="exam-meta__stat">
                <span className="exam-meta__stat-label">Response state</span>
                <span className="exam-meta__stat-value">{answerState}</span>
              </article>

              <article className="exam-meta__stat">
                <span className="exam-meta__stat-label">Question ID</span>
                <span className="exam-meta__stat-value exam-meta__stat-value--mono">{question.questionId}</span>
              </article>
            </div>

            <div className="exam-meta__note">
              {feedback
                ? 'Review the adaptive insight, then continue when you are ready.'
                : 'Choose one option before the timer runs out. Unanswered questions are submitted automatically.'}
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
};

export default ExamPage;
