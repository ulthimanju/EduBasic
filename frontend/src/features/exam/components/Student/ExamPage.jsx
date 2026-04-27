import React, { useEffect, useState, useRef, useCallback, useMemo, memo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import useExamStore from '../../store/examStore';
import { Clock, ChevronLeft, ChevronRight, Save, Send, ShieldAlert, List } from 'lucide-react';
import Spinner from '../../../../components/common/Spinner/Spinner';
import QuestionRenderer from './QuestionRenderer';
import { usePrompt } from '../../../../context/PromptContext';
import { ROUTES }     from '../../../../constants/appConstants';
import { useExamLockdown } from '../../../../hooks/useExamLockdown';
import TerminatedOverlay from './TerminatedOverlay';

const ExamTimer = memo(({ initialSeconds, onTimeUp }) => {
  const [timeLeft, setTimeLeft] = useState(initialSeconds);
  const timerRef = useRef(null);

  useEffect(() => {
    timerRef.current = setInterval(() => {
      setTimeLeft(prev => {
        if (prev <= 1) {
          clearInterval(timerRef.current);
          onTimeUp();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [onTimeUp]);

  const formatTime = (seconds) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <div className="exam-status-chip">
      <Clock size={16} />
      <div>
        <span className="exam-status-chip__label">Time Remaining</span>
        <strong>{formatTime(timeLeft)}</strong>
      </div>
    </div>
  );
});

const ExamPage = () => {
  const { attemptId } = useParams();
  const navigate = useNavigate();
  const { openPrompt } = usePrompt();
  
  const { fetchExam, fetchAttempt, currentExam, syncAttempt, submitAttempt, isLoading } = useExamStore();
  
  const [currentSectionIdx, setCurrentSectionIdx] = useState(0);
  const [currentQuestionIdx, setCurrentQuestionIdx] = useState(0);
  const [answers, setAnswers] = useState({}); // questionId -> answer
  const [dirtyQuestionIds, setDirtyQuestionIds] = useState(new Set());
  const [version, setVersion] = useState(0);
  const [isTerminatedInternal, setIsTerminatedInternal] = useState(false);

  const syncIntervalRef = useRef(null);

  const { isTerminated } = useExamLockdown(attemptId, !!currentExam && !isTerminatedInternal, () => {
    setIsTerminatedInternal(true);
    if (syncIntervalRef.current) clearInterval(syncIntervalRef.current);
  });

  useEffect(() => {
    if (isTerminated) {
      const timer = setTimeout(() => {
        navigate(ROUTES.RESULT.replace(':attemptId', attemptId), { replace: true });
      }, 3000);
      return () => clearTimeout(timer);
    }
  }, [isTerminated, attemptId, navigate]);

  useEffect(() => {
    const bootstrap = async () => {
      const start = performance.now();
      try {
        const attempt = await fetchAttempt(attemptId);
        await fetchExam(attempt.examId);
        const end = performance.now();
        console.log(`Exam bootstrap took ${(end - start).toFixed(2)}ms`);
      } catch (err) {
        console.error('Failed to bootstrap exam session', err);
        navigate(ROUTES.DASHBOARD);
      }
    };
    if (attemptId) {
      bootstrap();
    }
  }, [attemptId, fetchAttempt, fetchExam, navigate]);

  const serializeAnswers = useCallback((onlyDirty = false) => {
    const serialized = {};
    const idsToSerialize = onlyDirty ? Array.from(dirtyQuestionIds) : Object.keys(answers);
    
    idsToSerialize.forEach(qid => {
      const ans = answers[qid];
      serialized[qid] = typeof ans === 'object' ? JSON.stringify(ans) : String(ans);
    });
    return serialized;
  }, [answers, dirtyQuestionIds]);

  const handleSync = useCallback(async (isFullSync = false) => {
    if (!isFullSync && dirtyQuestionIds.size === 0) return;
    
    const start = performance.now();
    try {
      const deltaAnswers = serializeAnswers(!isFullSync);
      const updatedAttempt = await syncAttempt(attemptId, { version, answers: deltaAnswers });
      setVersion(updatedAttempt.version);
      if (!isFullSync) {
        setDirtyQuestionIds(new Set());
      }
      const end = performance.now();
      console.log(`Autosave (${isFullSync ? 'full' : 'delta'}) took ${(end - start).toFixed(2)}ms`);
    } catch (err) {
      console.error('Autosave failed', err);
    }
  }, [attemptId, version, syncAttempt, serializeAnswers, dirtyQuestionIds]);

  useEffect(() => {
    if (currentExam) {
      syncIntervalRef.current = setInterval(() => handleSync(false), 30000); // 30s delta sync
    }
    
    return () => {
      if (syncIntervalRef.current) clearInterval(syncIntervalRef.current);
    };
  }, [currentExam, handleSync]);

  const performSubmit = useCallback(async () => {
    try {
      await submitAttempt(attemptId);
      navigate(ROUTES.RESULT.replace(':attemptId', attemptId));
    } catch (err) {
      console.error('Submission failed', err);
    }
  }, [attemptId, submitAttempt, navigate]);

  const handleAutoSubmit = useCallback(() => {
    openPrompt({
      type: 'message',
      severity: 'warning',
      title: 'Time is up!',
      description: 'Your exam is being automatically submitted.',
      confirmLabel: 'OK',
      onConfirm: () => performSubmit()
    });
  }, [openPrompt, performSubmit]);

  const handleSubmitClick = useCallback(() => {
    openPrompt({
      type: 'confirm',
      severity: 'warning',
      title: 'Submit Exam?',
      description: 'Are you sure you want to finish the exam? You cannot change your answers after submission.',
      confirmLabel: 'Submit Now',
      cancelLabel: 'Go Back',
      onConfirm: performSubmit
    });
  }, [openPrompt, performSubmit]);

  const handleAnswerChange = useCallback((val) => {
    // Current question depends on indices
    // To keep handleAnswerChange stable, we use the mapping outside if possible, 
    // but here mapping is derived from state. 
    // We'll use a functional update for setAnswers and setDirtyQuestionIds.
    setAnswers(prev => {
      // Find question ID from current state (not ideal for useCallback but necessary here)
      // Actually we can pass qid to handleAnswerChange if QuestionRenderer supports it.
      // For now we'll stick to this but be careful with dependencies.
      return prev; 
    });
  }, []);

  // REDEFINE handleAnswerChange inside the component to have access to current indices but use refs or other tricks if needed.
  // Actually, a simpler way is to pass currentQuestion.id to handleAnswerChange.

  const sections = useMemo(() => {
    if (!currentExam) return [];
    return currentExam.hasSections ? currentExam.sections : [{ questions: currentExam.questions }];
  }, [currentExam]);

  const currentSection = useMemo(() => sections[currentSectionIdx], [sections, currentSectionIdx]);
  const currentMapping = useMemo(() => currentSection?.questions[currentQuestionIdx], [currentSection, currentQuestionIdx]);
  const currentQuestion = useMemo(() => currentMapping?.question, [currentMapping]);

  const onAnswerChange = useCallback((val) => {
    if (!currentQuestion) return;
    setAnswers(prev => ({ ...prev, [currentQuestion.id]: val }));
    setDirtyQuestionIds(prev => {
      const next = new Set(prev);
      next.add(currentQuestion.id);
      return next;
    });
  }, [currentQuestion]);

  const handlePrev = useCallback(() => {
    if (currentQuestionIdx > 0) setCurrentQuestionIdx(prev => prev - 1);
    else if (currentSectionIdx > 0) {
      const prevSecIdx = currentSectionIdx - 1;
      setCurrentSectionIdx(prevSecIdx);
      setCurrentQuestionIdx(sections[prevSecIdx].questions.length - 1);
    }
  }, [currentQuestionIdx, currentSectionIdx, sections]);

  const handleNext = useCallback(() => {
    if (currentQuestionIdx < currentSection.questions.length - 1) setCurrentQuestionIdx(prev => prev + 1);
    else if (currentSectionIdx < sections.length - 1) {
      setCurrentSectionIdx(prev => prev + 1);
      setCurrentQuestionIdx(0);
    }
  }, [currentQuestionIdx, currentSection, currentSectionIdx, sections]);

  if (!currentExam) return <Spinner />;

  return (
    <div className="exam-layout animate-page-enter" style={{ maxWidth: '1000px', margin: '0 auto' }}>
      {isTerminated && <TerminatedOverlay />}
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
          {currentExam.timeLimitMins && (
            <ExamTimer 
              initialSeconds={currentExam.timeLimitMins * 60} 
              onTimeUp={handleAutoSubmit} 
            />
          )}
          <button className="btn btn-secondary" onClick={() => handleSync(true)}>
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
            onChange={onAnswerChange}
          />

          <div className="exam-action-row" style={{ marginTop: 'auto', gap: 'var(--space-3)' }}>
            <button 
              className="btn btn-secondary" 
              disabled={currentQuestionIdx === 0 && currentSectionIdx === 0}
              onClick={handlePrev}
            >
              <ChevronLeft size={18} />
              Previous
            </button>
            <button 
              className="btn btn-secondary"
              disabled={currentQuestionIdx === currentSection.questions.length - 1 && currentSectionIdx === sections.length - 1}
              onClick={handleNext}
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
