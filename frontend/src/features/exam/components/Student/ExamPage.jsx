import React, { useEffect, useState, useCallback, useMemo, useTransition, useDeferredValue } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import examApi from '../../../../api/exam';
import useExamStore from '../../store/examStore';
import { ChevronLeft, ChevronRight, Save, Send, ShieldAlert } from 'lucide-react';
import Spinner from '../../../../components/common/Spinner/Spinner';
import ProgressBar from '../../../../components/common/ProgressBar/ProgressBar';
import QuestionRenderer from './QuestionRenderer';
import { usePrompt } from '../../../../context/PromptContext';
import { ROUTES }     from '../../../../constants/appConstants';
import { useProctoring } from '../../../../hooks/useProctoring';
import TerminatedOverlay from './TerminatedOverlay';
import ExamTimer from './ExamTimer';
import QuestionNav from './QuestionNav';
import { useAnswerSync } from '../../hooks/useAnswerSync';

const ExamPage = () => {
  const { attemptId } = useParams();
  const navigate = useNavigate();
  const { openPrompt } = usePrompt();
  
  const { fetchExam, fetchAttempt, currentExam, syncAttempt, isLoading } = useExamStore();
  
  const [currentSectionIdx, setCurrentSectionIdx] = useState(0);
  const [currentQuestionIdx, setCurrentQuestionIdx] = useState(0);
  
  // React 19 useTransition for smooth UI during AI adaptive recalculations/answers
  const [isPendingTransition, startTransition] = useTransition();

  const { answers, onAnswerChange, handleSync, version } = useAnswerSync(attemptId, syncAttempt);

  const { isTerminated } = useProctoring(attemptId, {
    isActive: !!currentExam,
    onTerminate: () => {},
    onWarning: (type, count, max) => {
      openPrompt({
        type: 'message',
        severity: 'warning',
        title: 'Security Warning',
        description: `Security violation detected: ${type}. Warning ${count}/${max}. Your exam will be auto-submitted on next violation.`,
        confirmLabel: 'I Understand'
      });
    }
  });

  // Use React Query mutation for submission to prevent double submits without local flags
  const submitMutation = useMutation({
    mutationFn: () => examApi.submitAttempt(attemptId),
    onSuccess: () => navigate(ROUTES.RESULT.replace(':attemptId', attemptId), { replace: true }),
    onError: (err) => console.error('Submission failed', err)
  });

  useEffect(() => {
    if (isTerminated && !submitMutation.isPending && !submitMutation.isSuccess) {
      const timer = setTimeout(() => {
        submitMutation.mutate();
      }, 3000);
      return () => clearTimeout(timer);
    }
  }, [isTerminated, submitMutation]);

  useEffect(() => {
    const bootstrap = async () => {
      try {
        const attempt = await fetchAttempt(attemptId);
        await fetchExam(attempt.examId);
      } catch (err) {
        console.error('Failed to bootstrap exam session', err);
        navigate(ROUTES.DASHBOARD);
      }
    };
    if (attemptId) {
      bootstrap();
    }
  }, [attemptId, fetchAttempt, fetchExam, navigate]);

  const performSubmit = useCallback(() => {
    if (!submitMutation.isPending) {
      submitMutation.mutate();
    }
  }, [submitMutation]);

  const handleAutoSubmit = useCallback(() => {
    openPrompt({
      type: 'message',
      severity: 'warning',
      title: 'Time is up!',
      description: 'Your exam is being automatically submitted.',
      confirmLabel: 'OK',
      onConfirm: performSubmit
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

  const sections = useMemo(() => {
    if (!currentExam) return [];
    return currentExam.hasSections ? currentExam.sections : [{ questions: currentExam.questions }];
  }, [currentExam]);

  const currentSection = useMemo(() => sections[currentSectionIdx], [sections, currentSectionIdx]);
  const currentMapping = useMemo(() => currentSection?.questions[currentQuestionIdx], [currentSection, currentQuestionIdx]);
  const currentQuestion = useMemo(() => currentMapping?.question, [currentMapping]);
  
  // React 19 useDeferredValue keeps question rendering smooth if it's computationally heavy
  const deferredQuestion = useDeferredValue(currentQuestion);

  const handlePrev = useCallback(() => {
    startTransition(() => {
      if (currentQuestionIdx > 0) setCurrentQuestionIdx(prev => prev - 1);
      else if (currentSectionIdx > 0) {
        const prevSecIdx = currentSectionIdx - 1;
        setCurrentSectionIdx(prevSecIdx);
        setCurrentQuestionIdx(sections[prevSecIdx].questions.length - 1);
      }
    });
  }, [currentQuestionIdx, currentSectionIdx, sections]);

  const handleNext = useCallback(() => {
    startTransition(() => {
      if (currentQuestionIdx < currentSection.questions.length - 1) setCurrentQuestionIdx(prev => prev + 1);
      else if (currentSectionIdx < sections.length - 1) {
        setCurrentSectionIdx(prev => prev + 1);
        setCurrentQuestionIdx(0);
      }
    });
  }, [currentQuestionIdx, currentSection, currentSectionIdx, sections]);

  if (!currentExam || isLoading) return <Spinner />;

  return (
    <div className="exam-layout animate-page-enter" style={{ maxWidth: '1000px', margin: '0 auto', opacity: isPendingTransition ? 0.7 : 1 }}>
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
          <ProgressBar percent={((currentQuestionIdx + 1) / currentSection.questions.length) * 100} size="sm" />
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
          <button 
            className="btn btn-primary" 
            onClick={handleSubmitClick} 
            disabled={submitMutation.isPending}
          >
            {submitMutation.isPending ? <Spinner size="sm" /> : <Send size={16} />}
            <span>{submitMutation.isPending ? 'Submitting...' : 'Submit'}</span>
          </button>
        </div>
      </header>

      <div className="exam-grid" style={{ gridTemplateColumns: '1fr 240px' }}>
        <main className="question-panel">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
            <h1 className="question-title" style={{ fontSize: 'var(--text-xl)' }}>{deferredQuestion?.title}</h1>
            <span className="course-card__topic" style={{ background: 'var(--color-accent-subtle)', color: 'var(--color-accent)' }}>
              {currentMapping?.marks} Marks
            </span>
          </div>
          <p className="question-subtitle">{deferredQuestion?.description}</p>
          
          <div className="divider" style={{ margin: 'var(--space-2) 0 var(--space-4)' }} />

          {deferredQuestion && (
            <QuestionRenderer 
              question={deferredQuestion} 
              answer={answers[deferredQuestion.id]} 
              onChange={(val) => {
                startTransition(() => {
                  onAnswerChange(deferredQuestion.id, val);
                });
              }}
            />
          )}

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
          <QuestionNav 
            questions={currentSection.questions}
            currentQuestionIdx={currentQuestionIdx}
            setCurrentQuestionIdx={(idx) => startTransition(() => setCurrentQuestionIdx(idx))}
            answers={answers}
          />

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
