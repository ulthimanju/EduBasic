import { useEffect, useRef, useState } from 'react';
import api from '../api/exam';
import { usePrompt } from '../context/PromptContext';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '../constants/appConstants';

export const useExamLockdown = (attemptId, isActive, onAutoSubmit) => {
  const { openPrompt } = usePrompt();
  const navigate = useNavigate();
  const lastReportTime = useRef({});
  const [isTerminated, setIsTerminated] = useState(false);

  useEffect(() => {
    if (!isActive || !attemptId || isTerminated) return;

    // Fullscreen enforcement
    const enterFullscreen = () => {
      if (document.documentElement.requestFullscreen) {
        document.documentElement.requestFullscreen().catch(err => {
          console.error(`Error attempting to enable fullscreen: ${err.message}`);
        });
      }
    };

    const handleFullscreenChange = () => {
      if (!document.fullscreenElement && !isTerminated) {
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
        reportViolation('SHORTCUT_BLOCKED', { key: e.key });
      }
    };

    const handleVisibilityChange = () => {
      if (document.hidden && !isTerminated) {
        reportViolation('TAB_SWITCH');
      }
    };

    const handleBlur = () => {
      if (!isTerminated) {
        reportViolation('TAB_SWITCH');
      }
    };

    const reportViolation = async (type, metadata = {}) => {
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
          setIsTerminated(true);
          if (onAutoSubmit) onAutoSubmit();

          // Force exit fullscreen to allow navigation/seeing browser bars
          if (document.fullscreenElement) {
            document.exitFullscreen().catch(() => {});
          }

          // Delay slightly so user can see the message before redirect (handled in component now)
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
  }, [attemptId, isActive, isTerminated, navigate, onAutoSubmit, openPrompt]);

  return { isTerminated };
};
