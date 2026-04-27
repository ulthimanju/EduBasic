import { useEffect, useRef } from 'react';
import api from '../api/exam';
import { usePrompt } from '../context/PromptContext';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '../constants/appConstants';

export const useExamLockdown = (attemptId, isActive, onAutoSubmit) => {
  const { openPrompt } = usePrompt();
  const navigate = useNavigate();
  const lastReportTime = useRef({});

  useEffect(() => {
    if (!isActive || !attemptId) return;

    // Fullscreen enforcement
    const enterFullscreen = () => {
      if (document.documentElement.requestFullscreen) {
        document.documentElement.requestFullscreen().catch(err => {
          console.error(`Error attempting to enable fullscreen: ${err.message}`);
        });
      }
    };

    const handleFullscreenChange = () => {
      if (!document.fullscreenElement) {
        reportViolation('FULLSCREEN_EXIT');
      }
    };

    // Browser event blocking
    const handleContextMenu = (e) => e.preventDefault();
    const handleCopyPaste = (e) => e.preventDefault();
    
    const handleKeyDown = (e) => {
      const blockedKeys = ['c', 'v', 'u', 's', 'a'];
      if ((e.ctrlKey || e.metaKey) && blockedKeys.includes(e.key.toLowerCase())) {
        e.preventDefault();
        reportViolation('SHORTCUT_BLOCKED', { key: e.key });
      }
      if (e.key === 'F12' || (e.altKey && e.key === 'Tab')) {
        // Alt+Tab might not be catchable in all browsers but we try
        reportViolation('SHORTCUT_BLOCKED', { key: e.key });
      }
    };

    const handleVisibilityChange = () => {
      if (document.hidden) {
        reportViolation('TAB_SWITCH');
      }
    };

    const handleBlur = () => {
      reportViolation('TAB_SWITCH'); // Treating blur as tab switch for simplicity
    };

    const reportViolation = async (type, metadata = {}) => {
      // Throttle: don't report the same violation type more than once every 5 seconds
      const now = Date.now();
      if (lastReportTime.current[type] && (now - lastReportTime.current[type] < 5000)) {
        return;
      }
      lastReportTime.current[type] = now;

      try {
        const response = await api.recordViolation(attemptId, {
          violationType: type,
          timestamp: new Date().toISOString(),
          metadata: { ...metadata, url: window.location.href }
        });

        const { violationCount, maxViolations, autoSubmitted } = response.data;

        if (autoSubmitted) {
          openPrompt({
            type: 'message',
            severity: 'danger',
            title: 'Exam Terminated',
            description: 'Your exam has been automatically submitted due to multiple security violations.',
            confirmLabel: 'Exit',
            onConfirm: () => {
              if (onAutoSubmit) onAutoSubmit();
              navigate(ROUTES.RESULT.replace(':attemptId', attemptId));
            }
          });
        } else {
          openPrompt({
            type: 'message',
            severity: 'warning',
            title: 'Security Warning',
            description: `Security violation detected: ${type}. Warning ${violationCount}/${maxViolations}. Your exam will be auto-submitted on next violation.`,
            confirmLabel: 'I Understand'
          });
        }
      } catch (err) {
        console.error('Failed to report violation', err);
      }
    };

    // Initial activation
    enterFullscreen();

    // Event listeners
    document.addEventListener('fullscreenchange', handleFullscreenChange);
    document.addEventListener('contextmenu', handleContextMenu);
    document.addEventListener('copy', handleCopyPaste);
    document.addEventListener('paste', handleCopyPaste);
    document.addEventListener('cut', handleCopyPaste);
    window.addEventListener('keydown', handleKeyDown);
    document.addEventListener('visibilitychange', handleVisibilityChange);
    window.addEventListener('blur', handleBlur);

    // CSS lockdown
    document.body.style.userSelect = 'none';

    return () => {
      document.removeEventListener('fullscreenchange', handleFullscreenChange);
      document.removeEventListener('contextmenu', handleContextMenu);
      document.removeEventListener('copy', handleCopyPaste);
      document.removeEventListener('paste', handleCopyPaste);
      document.removeEventListener('cut', handleCopyPaste);
      window.removeEventListener('keydown', handleKeyDown);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      window.removeEventListener('blur', handleBlur);
      document.body.style.userSelect = 'auto';
    };
  }, [attemptId, isActive]);
};
