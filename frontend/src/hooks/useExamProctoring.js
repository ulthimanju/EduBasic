import { useEffect } from 'react';
import examApi from '../services/apiClient'; // Wait, should be examApi service
import api from '../services/examApi';
import { usePrompt } from '../context/PromptContext';

export const useExamProctoring = (attemptId, isActive) => {
  const { openPrompt } = usePrompt();

  useEffect(() => {
    if (!isActive || !attemptId) return;

    const handleVisibilityChange = () => {
      if (document.hidden) {
        logViolation('TAB_SWITCH', { timestamp: new Date().toISOString() });
      }
    };

    const handleBlur = () => {
      logViolation('WINDOW_BLUR', { timestamp: new Date().toISOString() });
    };

    const logViolation = async (type, data) => {
      try {
        await api.logProctoringEvent(attemptId, { eventType: type, eventData: data });
        openPrompt({
          type: 'message',
          severity: 'warning',
          title: 'Security Warning',
          description: 'A security violation (tab switch or focus loss) has been recorded. Please stay on this page to avoid disqualification.',
          confirmLabel: 'I Understand'
        });
      } catch (err) {
        console.error('Failed to log proctoring event', err);
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    window.addEventListener('blur', handleBlur);

    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      window.removeEventListener('blur', handleBlur);
    };
  }, [attemptId, isActive]);
};
