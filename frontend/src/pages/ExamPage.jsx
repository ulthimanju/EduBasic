import React, { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Clock, ChevronRight, CheckCircle2, XCircle } from 'lucide-react';
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

  return (
    <div className="max-w-3xl mx-auto page-enter">
      <div className="flex justify-between items-end mb-8">
        <div>
          <p className="text-xs font-bold text-text-muted uppercase tracking-widest mb-1">Adaptive Assessment</p>
          <h1 className="text-2xl font-semibold">Question {question.index} <span className="text-text-muted font-normal">/ 20</span></h1>
        </div>
        <div className={`flex items-center gap-2 px-4 py-2 rounded-lg border font-mono text-lg ${
          timeLeft < 10 ? 'border-accent text-accent animate-pulse' : 'border-border text-text-primary'
        }`}>
          <Clock size={18} />
          {timeLeft}s
        </div>
      </div>

      <div className="panel p-8 md:p-10">
        <div className="mb-6">
          <span className={`text-[10px] font-bold px-2 py-1 rounded-md uppercase tracking-wider ${
            question.difficulty === 'EASY' ? 'bg-green-500/10 text-green-500 border border-green-500/20' :
            question.difficulty === 'MEDIUM' ? 'bg-blue-500/10 text-blue-500 border border-blue-500/20' :
            question.difficulty === 'HARD' ? 'bg-orange-500/10 text-orange-500 border border-orange-500/20' :
            'bg-accent/10 text-accent border border-accent/20'
          }`}>
            {question.difficulty}
          </span>
        </div>
        
        <h2 className="text-2xl font-medium leading-relaxed mb-10 text-text-primary">
          {question.question}
        </h2>

        <div className="grid gap-3">
          {question.options.map((option, index) => {
            const isSelected = selectedOption === option;
            const isCorrect = feedback?.correctAnswer === option;
            const isWrong = feedback && isSelected && !feedback.correct;

            let stateClass = 'border-border-subtle bg-surface-glass hover:bg-interactive-hover';
            
            if (feedback) {
              if (isCorrect) {
                stateClass = 'border-green-500/50 bg-green-500/5 text-text-primary ring-1 ring-green-500/20';
              } else if (isWrong) {
                stateClass = 'border-accent/50 bg-accent/5 text-text-primary ring-1 ring-accent/20';
              } else {
                stateClass = 'border-border-subtle bg-surface-glass opacity-50';
              }
            } else if (isSelected) {
              stateClass = 'border-accent bg-accent-subtle text-text-primary ring-1 ring-accent';
            }

            return (
              <button
                key={index}
                disabled={!!feedback || submitting}
                onClick={() => setSelectedOption(option)}
                className={`w-full text-left p-5 rounded-xl border transition-all flex items-center justify-between group ${stateClass}`}
              >
                <span className="font-medium">{option}</span>
                <div className="flex items-center">
                  {feedback && isCorrect && <CheckCircle2 size={20} className="text-green-500" />}
                  {feedback && isWrong && <XCircle size={20} className="text-accent" />}
                  {!feedback && isSelected && <div className="w-2 h-2 rounded-full bg-accent" />}
                </div>
              </button>
            );
          })}
        </div>

        {feedback && (
          <div className="mt-10 p-6 rounded-xl bg-surface-glass border border-border-subtle animate-page-enter">
            <h3 className="text-sm font-bold uppercase tracking-widest text-text-secondary mb-3 flex items-center gap-2">
              Analysis & Explanation
            </h3>
            <p className="text-text-secondary leading-relaxed">
              {feedback.explanation}
            </p>
          </div>
        )}

        <div className="mt-10 pt-6 border-t border-border-subtle flex justify-end">
          {!feedback ? (
            <button
              disabled={!selectedOption || submitting}
              onClick={handleSubmit}
              className="btn btn-primary px-10 py-4 text-base"
            >
              {submitting ? 'Analyzing...' : 'Submit Answer'}
            </button>
          ) : (
            <button
              onClick={handleNext}
              className="btn btn-primary px-10 py-4 text-base flex items-center gap-2"
            >
              {feedback.sessionComplete ? 'View Proficiency Report' : 'Next Question'}
              <ChevronRight size={18} />
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export default ExamPage;
