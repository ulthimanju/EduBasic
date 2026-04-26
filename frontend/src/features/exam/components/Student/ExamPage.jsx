import React, { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import useExamStore from '../../store/examStore';
import { Clock, ChevronLeft, ChevronRight, Save, Send, ShieldAlert, List } from 'lucide-react';
import Spinner from '../../../../components/ui/Spinner/Spinner';
import QuestionRenderer from './QuestionRenderer';
import { usePrompt } from '../../../../context/PromptContext';
import { ROUTES } from '../../../../constants/appConstants';
import { useExamProctoring } from '../../../../hooks/useExamProctoring';

const ExamPage = () => {
  const { attemptId } = useParams();
  const navigate = useNavigate();
  const { openPrompt } = usePrompt();
  
  const { fetchExam, currentExam, syncAttempt, submitAttempt, isLoading } = useExamStore();
  
  const [currentSectionIdx, setCurrentSectionIdx] = useState(0);
  const [currentQuestionIdx, setCurrentQuestionIdx] = useState(0);
  const [answers, setAnswers] = useState({}); // questionId -> answer
  const [timeLeft, setTimeLeft] = useState(0);
  const [version, setVersion] = useState(0);

  const timerRef = useRef(null);
  const syncIntervalRef = useRef(null);

  useExamProctoring(attemptId, !!currentExam);

  useEffect(() => {
    // In a real app, fetchAttempt(attemptId) would give examId
    // For now, let's assume currentExam is already set or handle loading
    // fetchExam is called by some bootstrap or here if we have examId
  }, [attemptId]);

  useEffect(() => {
    if (currentExam && currentExam.timeLimitMins) {
      setTimeLeft(currentExam.timeLimitMins * 60);
      startTimer();
    }
    
    syncIntervalRef.current = setInterval(handleSync, 30000); // 30s autosave

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
      if (syncIntervalRef.current) clearInterval(syncIntervalRef.current);
    };
  }, [currentExam]);

  const startTimer = () => {
    timerRef.current = setInterval(() => {
      setTimeLeft(prev => {
        if (prev <= 1) {
          clearInterval(timerRef.current);
          handleAutoSubmit();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  };

  const handleSync = async () => {
    try {
      await syncAttempt(attemptId, { version, answers: serializeAnswers() });
      setVersion(prev => prev + 1);
    } catch (err) {
      console.error('Autosave failed', err);
    }
  };

  const serializeAnswers = () => {
    const serialized = {};
    Object.entries(answers).forEach(([qid, ans]) => {
      serialized[qid] = typeof ans === 'object' ? JSON.stringify(ans) : String(ans);
    });
    return serialized;
  };

  const handleAutoSubmit = () => {
    openPrompt({
      type: 'message',
      severity: 'warning',
      title: 'Time is up!',
      description: 'Your exam is being automatically submitted.',
      confirmLabel: 'OK',
      onConfirm: () => performSubmit()
    });
  };

  const performSubmit = async () => {
    try {
      await submitAttempt(attemptId);
      navigate(ROUTES.RESULT.replace(':attemptId', attemptId));
    } catch (err) {
      console.error('Submission failed', err);
    }
  };

  const handleSubmitClick = () => {
    openPrompt({
      type: 'confirm',
      severity: 'warning',
      title: 'Submit Exam?',
      description: 'Are you sure you want to finish the exam? You cannot change your answers after submission.',
      confirmLabel: 'Submit Now',
      cancelLabel: 'Go Back',
      onConfirm: performSubmit
    });
  };

  if (!currentExam) return <Spinner />;

  const sections = currentExam.hasSections ? currentExam.sections : [{ questions: currentExam.questions }];
  const currentSection = sections[currentSectionIdx];
  const currentMapping = currentSection.questions[currentQuestionIdx];
  const currentQuestion = currentMapping.question;

  const handleAnswerChange = (val) => {
    setAnswers({ ...answers, [currentQuestion.id]: val });
  };

  const formatTime = (seconds) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <div className="exam-layout animate-page-enter" style={{ maxWidth: '1000px', margin: '0 auto' }}>
      <header className="exam-toolbar panel">
        <div className="exam-toolbar__progress">
          <div className="exam-toolbar__headline">
            <span className="exam-meta-label">{currentExam.title}</span>
            <span className="exam-meta-value">
              {currentExam.hasSections ? `${currentSection.title} - ` : ''} 
              Question {currentQuestionIdx + 1} / {currentSection.questions.length}
            </span>
          </div>
          <div className="exam-progress-track">
            <div 
              className="exam-progress-fill" 
              style={{ width: `${((currentQuestionIdx + 1) / currentSection.questions.length) * 100}%` }} 
            />
          </div>
        </div>

        <div className="exam-toolbar__status">
          <div className="exam-status-chip">
            <Clock size={16} />
            <div>
              <span className="exam-status-chip__label">Time Remaining</span>
              <strong>{formatTime(timeLeft)}</strong>
            </div>
          </div>
          <button className="btn btn-secondary" onClick={handleSync}>
            <Save size={16} />
            <span>Save</span>
          </button>
          <button className="btn btn-primary" onClick={handleSubmitClick}>
            <Send size={16} />
            <span>Submit</span>
          </button>
        </div>
      </header>

      <div className="exam-grid" style={{ gridTemplateColumns: '1fr 240px' }}>
        <main className="question-panel">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
            <h1 className="question-title" style={{ fontSize: 'var(--text-xl)' }}>{currentQuestion.title}</h1>
            <span className="course-card__topic" style={{ background: 'var(--color-accent-subtle)', color: 'var(--color-accent)' }}>
              {currentMapping.marks} Marks
            </span>
          </div>
          <p className="question-subtitle">{currentQuestion.description}</p>
          
          <div className="divider" style={{ margin: 'var(--space-2) 0 var(--space-4)' }} />

          <QuestionRenderer 
            question={currentQuestion} 
            answer={answers[currentQuestion.id]} 
            onChange={handleAnswerChange}
          />

          <div className="exam-action-row" style={{ marginTop: 'auto', gap: 'var(--space-3)' }}>
            <button 
              className="btn btn-secondary" 
              disabled={currentQuestionIdx === 0 && currentSectionIdx === 0}
              onClick={() => {
                if (currentQuestionIdx > 0) setCurrentQuestionIdx(prev => prev - 1);
                else if (currentSectionIdx > 0) {
                  const prevSecIdx = currentSectionIdx - 1;
                  setCurrentSectionIdx(prevSecIdx);
                  setCurrentQuestionIdx(sections[prevSecIdx].questions.length - 1);
                }
              }}
            >
              <ChevronLeft size={18} />
              Previous
            </button>
            <button 
              className="btn btn-secondary"
              disabled={currentQuestionIdx === currentSection.questions.length - 1 && currentSectionIdx === sections.length - 1}
              onClick={() => {
                if (currentQuestionIdx < currentSection.questions.length - 1) setCurrentQuestionIdx(prev => prev + 1);
                else if (currentSectionIdx < sections.length - 1) {
                  setCurrentSectionIdx(prev => prev + 1);
                  setCurrentQuestionIdx(0);
                }
              }}
            >
              Next
              <ChevronRight size={18} />
            </button>
          </div>
        </main>

        <aside className="exam-side-panel">
          <div className="exam-side-card panel">
            <h2 className="exam-side-card__title" style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
              <List size={18} />
              <span>Questions</span>
            </h2>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 'var(--space-2)', marginTop: 'var(--space-3)' }}>
              {currentSection.questions.map((m, idx) => (
                <button
                  key={m.id}
                  className={`btn ${currentQuestionIdx === idx ? 'btn-primary' : answers[m.question.id] ? 'btn-secondary' : 'btn-ghost'}`}
                  style={{ padding: '8px', minWidth: '0' }}
                  onClick={() => setCurrentQuestionIdx(idx)}
                >
                  {idx + 1}
                </button>
              ))}
            </div>
          </div>

          <div className="exam-side-card panel" style={{ marginTop: 'var(--space-4)' }}>
            <h2 className="exam-side-card__title" style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
              <ShieldAlert size={18} />
              <span>Security</span>
            </h2>
            <p className="exam-side-card__text" style={{ fontSize: 'var(--text-xs)' }}>
              Do not switch tabs or leave fullscreen mode. Your activity is being monitored.
            </p>
          </div>
        </aside>
      </div>
    </div>
  );
};

export default ExamPage;
