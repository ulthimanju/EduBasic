import { useEffect, useState, useCallback, useRef } from 'react';
import examApi from '../api/exam';

/**
 * Unified hook for exam proctoring and lockdown.
 * 
 * @param {string} sessionId - The attempt or session ID.
 * @param {object} options - Configuration options.
 * @param {boolean} options.isActive - Whether proctoring is active.
 * @param {function} options.onTerminate - Callback when exam is terminated.
 * @param {function} options.onWarning - Callback for non-terminating violations.
 * @param {boolean} options.preventCopyPaste - Whether to block copy/paste/cut.
 * @param {boolean} options.enforceFullscreen - Whether to force fullscreen.
 */
export const useProctoring = (sessionId, {
  isActive = false,
  onTerminate = () => {},
  onWarning = () => {},
  preventCopyPaste = true,
  enforceFullscreen = true
} = {}) => {
  const [violationCount, setViolationCount] = useState(0);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [isTerminated, setIsTerminated] = useState(false);
  
  const lastReportTime = useRef({});
  const onTerminateRef = useRef(onTerminate);
  const onWarningRef = useRef(onWarning);

  useEffect(() => {
    onTerminateRef.current = onTerminate;
    onWarningRef.current = onWarning;
  }, [onTerminate, onWarning]);

  const reportViolation = useCallback(async (type, metadata = {}) => {
    if (isTerminated || !isActive || !sessionId) return;

    // Rate limit reports of the same type
    const now = Date.now();
    if (lastReportTime.current[type] && (now - lastReportTime.current[type] < 5000)) {
      return;
    }
    lastReportTime.current[type] = now;

    try {
      const response = await examApi.recordViolation(sessionId, {
        violationType: type,
        timestamp: new Date().toISOString(),
        metadata: { ...metadata, url: window.location.href }
      });

      const { violationCount: newCount, status, autoSubmitted } = response.data;
      setViolationCount(newCount);

      if (status === 'TERMINATED' || autoSubmitted) {
        setIsTerminated(true);
        if (document.fullscreenElement) {
          document.exitFullscreen().catch(() => {});
        }
        onTerminateRef.current(type);
      } else {
        onWarningRef.current(type, newCount, response.data.maxViolations);
      }
    } catch (err) {
      console.error('Failed to report violation:', err);
    }
  }, [sessionId, isActive, isTerminated]);

  const enterFullscreen = useCallback(() => {
    const elem = document.documentElement;
    if (elem.requestFullscreen) {
      elem.requestFullscreen().catch(err => console.error('Fullscreen failed', err));
    }
  }, []);

  useEffect(() => {
    if (!isActive || isTerminated) return;

    if (enforceFullscreen) {
      enterFullscreen();
    }

    const handleVisibilityChange = () => {
      if (document.visibilityState === 'hidden') {
        reportViolation('TAB_SWITCH');
      }
    };

    const handleBlur = () => {
      // Small delay to prevent issues with browser-native dialogs
      setTimeout(() => {
        if (document.visibilityState === 'visible' && !document.hasFocus()) {
          reportViolation('WINDOW_BLUR');
        }
      }, 100);
    };

    const handleFullscreenChange = () => {
      const isFull = !!document.fullscreenElement;
      setIsFullscreen(isFull);
      if (enforceFullscreen && !isFull && !isTerminated) {
        reportViolation('FULLSCREEN_EXIT');
      }
    };

    const handleContextMenu = (e) => e.preventDefault();
    
    const handleCopyPaste = (e) => {
      if (preventCopyPaste) e.preventDefault();
    };

    const handleKeyDown = (e) => {
      const blockedKeys = ['c', 'v', 'u', 's', 'a', 'p'];
      if ((e.ctrlKey || e.metaKey) && blockedKeys.includes(e.key.toLowerCase())) {
        e.preventDefault();
        reportViolation('SHORTCUT_BLOCKED', { key: e.key });
      }
      if (e.key === 'F12' || (e.altKey && e.key === 'Tab')) {
        e.preventDefault();
        reportViolation('SHORTCUT_BLOCKED', { key: e.key });
      }
    };

    const handlePopState = () => {
      window.history.pushState(null, null, window.location.href);
    };

    const handleBeforeUnload = (e) => {
      if (!isTerminated) {
        e.preventDefault();
        e.returnValue = '';
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    window.addEventListener('blur', handleBlur);
    document.addEventListener('fullscreenchange', handleFullscreenChange);
    document.addEventListener('contextmenu', handleContextMenu);
    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('popstate', handlePopState);
    window.addEventListener('beforeunload', handleBeforeUnload);

    if (preventCopyPaste) {
      document.addEventListener('copy', handleCopyPaste);
      document.addEventListener('paste', handleCopyPaste);
      document.addEventListener('cut', handleCopyPaste);
      document.body.style.userSelect = 'none';
    }

    window.history.pushState(null, null, window.location.href);

    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      window.removeEventListener('blur', handleBlur);
      document.removeEventListener('fullscreenchange', handleFullscreenChange);
      document.removeEventListener('contextmenu', handleContextMenu);
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('popstate', handlePopState);
      window.removeEventListener('beforeunload', handleBeforeUnload);
      
      if (preventCopyPaste) {
        document.removeEventListener('copy', handleCopyPaste);
        document.removeEventListener('paste', handleCopyPaste);
        document.removeEventListener('cut', handleCopyPaste);
        document.body.style.userSelect = 'auto';
      }
    };
  }, [isActive, isTerminated, enforceFullscreen, preventCopyPaste, reportViolation, enterFullscreen]);

  return {
    violationCount,
    isFullscreen,
    isTerminated,
    enterFullscreen
  };
};
