import React, { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Clock, ChevronRight, CheckCircle2, XCircle, Info, Brain } from 'lucide-react';
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

  useEffect(() => {
    fetchQuestion();
  }, [sessionId]);

  const fetchQuestion = async () => {
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
    handleSubmit();
  };

  const handleSubmit = async () => {
    if (submitting || feedback) return;
    setSubmitting(true);
    clearInterval(timerRef.current);
    const timeTaken = Math.floor((Date.now() - startTimeRef.current) / 1000);

    try {
      const response = await examApi.submitAnswer(sessionId, {
        questionId: question.questionId,
        selectedOption: selectedOption || 'NONE',
        timeTaken,
      });
      setFeedback(response.data);
    } catch (err) {
      setError('Failed to submit answer');
    } finally {
      setSubmitting(false);
    }
  };

  const handleNext = () => {
    if (feedback?.sessionComplete) {
      navigate(`/result/${sessionId}`);
    } else {
      fetchQuestion();
    }
  };

  if (loading) return <Spinner />;
  if (error) return <ErrorMessage message={error} />;
  if (!question) return null;

  const progress = (question.index / 20) * 100;

  return (
    <div className="dashboard--wide mx-auto animate-page-enter">
      <div className="exam-layout">
        <main className="exam-main">
          <div className="exam-header">
            <div className="exam-progress">
              <div 
                className="exam-progress__bar" 
                style={{ width: `${progress}%` }}
              />
            </div>
            <div className="flex justify-between items-center mt-2 px-1">
              <span className="text-xs font-bold text-text-muted uppercase tracking-widest">
                Progress: {question.index} / 20
              </span>
              <span className="text-xs font-bold text-text-muted uppercase tracking-widest">
                {Math.round(progress)}% Complete
              </span>
            </div>
          </div>

          <div className="panel p-8 md:p-12">
            <h1 className="text-2xl md:text-3xl font-semibold leading-snug mb-10 text-text-primary">
              {question.question}
            </h1>

            <div className="grid gap-4">
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
                    onClick={() => setSelectedOption(option)}
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
                <h3 className="feedback-panel__title flex items-center gap-2">
                  <Brain size={14} />
                  Adaptive Insight
                </h3>
                <p className="feedback-panel__text">
                  {feedback.explanation}
                </p>
              </div>
            )}

            <div className="mt-12 flex justify-end">
              {!feedback ? (
                <button
                  disabled={!selectedOption || submitting}
                  onClick={handleSubmit}
                  className="btn btn-primary px-10 py-4 text-base shadow-lg"
                >
                  {submitting ? 'Analyzing Response...' : 'Confirm Answer'}
                </button>
              ) : (
                <button
                  onClick={handleNext}
                  className="btn btn-primary px-10 py-4 text-base flex items-center gap-2 shadow-lg"
                >
                  {feedback.sessionComplete ? 'Finalize Assessment' : 'Next Question'}
                  <ChevronRight size={18} />
                </button>
              )}
            </div>
          </div>
        </main>

        <aside className="exam-meta">
          <div className={`exam-timer panel ${timeLeft < 10 ? 'exam-timer--urgent' : ''}`}>
            <span className="exam-timer__label flex items-center gap-2">
              <Clock size={12} />
              Time Remaining
            </span>
            <span className="exam-timer__value">
              {timeLeft}<span className="text-lg ml-1 font-medium opacity-50">s</span>
            </span>
          </div>

          <div className="exam-info-card panel">
            <div className="exam-info-item">
              <span className="exam-info-label">Difficulty</span>
              <span className={`exam-info-value font-bold ${
                question.difficulty === 'EASY' ? 'text-green-500' :
                question.difficulty === 'MEDIUM' ? 'text-blue-500' :
                question.difficulty === 'HARD' ? 'text-orange-500' :
                'text-accent'
              }`}>
                {question.difficulty}
              </span>
            </div>
            <div className="divider my-1" />
            <div className="exam-info-item">
              <span className="exam-info-label">Assessment Type</span>
              <span className="exam-info-value">Adaptive Signal</span>
            </div>
            <div className="divider my-1" />
            <div className="exam-info-item">
              <span className="exam-info-label flex items-center gap-1">
                <Info size={10} />
                Benchmark
              </span>
              <span className="exam-info-value">Standard placement</span>
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
};

export default ExamPage;
