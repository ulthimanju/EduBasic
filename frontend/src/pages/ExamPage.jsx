import React, { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
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
    <div className="container mx-auto p-4 max-w-2xl">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-xl font-bold">Question {question.index} / 20</h1>
        <div className={`text-xl font-mono ${timeLeft < 10 ? 'text-red-500' : ''}`}>
          {timeLeft}s
        </div>
      </div>

      <div className="bg-white p-8 rounded-xl shadow-lg border">
        <div className="mb-2">
          <span className={`text-xs font-bold px-2 py-1 rounded uppercase ${
            question.difficulty === 'EASY' ? 'bg-green-100 text-green-800' :
            question.difficulty === 'MEDIUM' ? 'bg-blue-100 text-blue-800' :
            question.difficulty === 'HARD' ? 'bg-orange-100 text-orange-800' :
            'bg-red-100 text-red-800'
          }`}>
            {question.difficulty}
          </span>
        </div>
        <h2 className="text-2xl font-semibold mb-6">{question.question}</h2>

        <div className="space-y-4">
          {question.options.map((option, index) => {
            const isSelected = selectedOption === option;
            const isCorrect = feedback?.correctAnswer === option;
            const isWrong = feedback && isSelected && !feedback.correct;

            let bgColor = 'bg-gray-50 hover:bg-gray-100';
            let borderColor = 'border-gray-200';

            if (feedback) {
              if (isCorrect) {
                bgColor = 'bg-green-100';
                borderColor = 'border-green-500';
              } else if (isWrong) {
                bgColor = 'bg-red-100';
                borderColor = 'border-red-500';
              }
            } else if (isSelected) {
              borderColor = 'border-blue-500';
              bgColor = 'bg-blue-50';
            }

            return (
              <button
                key={index}
                disabled={!!feedback || submitting}
                onClick={() => setSelectedOption(option)}
                className={`w-full text-left p-4 rounded-lg border-2 transition-all ${bgColor} ${borderColor} flex items-center justify-between`}
              >
                <span>{option}</span>
                {feedback && isCorrect && <span className="text-green-600">✓</span>}
                {feedback && isWrong && <span className="text-red-600">✗</span>}
              </button>
            );
          })}
        </div>

        {feedback && (
          <div className="mt-8 p-4 bg-gray-50 rounded-lg border border-gray-200 animate-fadeIn">
            <p className="font-semibold mb-2">Explanation:</p>
            <p className="text-gray-700">{feedback.explanation}</p>
          </div>
        )}

        <div className="mt-8 flex justify-end">
          {!feedback ? (
            <button
              disabled={!selectedOption || submitting}
              onClick={handleSubmit}
              className="bg-blue-600 text-white px-8 py-3 rounded-lg font-bold hover:bg-blue-700 disabled:opacity-50"
            >
              {submitting ? 'Submitting...' : 'Submit Answer'}
            </button>
          ) : (
            <button
              onClick={handleNext}
              className="bg-blue-600 text-white px-8 py-3 rounded-lg font-bold hover:bg-blue-700"
            >
              {feedback.sessionComplete ? 'See Result' : 'Next Question'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export default ExamPage;
