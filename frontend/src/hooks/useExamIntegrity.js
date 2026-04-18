import { useEffect, useState, useCallback, useRef } from 'react';
import examApi from '../services/examApi';

export const useExamIntegrity = (sessionId, onTerminate, isActive = false) => {
  const [violationCount, setViolationCount] = useState(0);
  const [showWarning, setShowWarning] = useState(false);
  const [warningReason, setWarningReason] = useState('');
  const [isFullscreen, setIsFullscreen] = useState(false);
  
  const isTerminatedRef = useRef(false);
  const onTerminateRef = useRef(onTerminate);
  const hasStartedRef = useRef(false);

  // Keep onTerminate callback fresh without re-triggering effects
  useEffect(() => {
    onTerminateRef.current = onTerminate;
  }, [onTerminate]);

  // Sync started state
  useEffect(() => {
    if (isActive) {
       hasStartedRef.current = true;
    }
  }, [isActive]);

  const reportViolation = useCallback(async (reason) => {
    if (isTerminatedRef.current || !hasStartedRef.current) return;

    try {
      const response = await examApi.reportViolation(sessionId, reason);
      const newCount = response.data.violationCount;
      const status = response.data.status;
      
      setViolationCount(newCount);

      if (status === 'TERMINATED') {
        isTerminatedRef.current = true;
        onTerminateRef.current(reason);
      } else {
        setWarningReason(reason);
        setShowWarning(true);
      }
    } catch (err) {
      console.error('Failed to report violation:', err);
    }
  }, [sessionId]);

  const enterFullscreen = useCallback(() => {
    const elem = document.documentElement;
    try {
      if (elem.requestFullscreen) {
        elem.requestFullscreen();
      } else if (elem.webkitRequestFullscreen) {
        elem.webkitRequestFullscreen();
      } else if (elem.msRequestFullscreen) {
        elem.msRequestFullscreen();
      }
    } catch (e) {
      console.error("Fullscreen request failed", e);
    }
  }, []);

  useEffect(() => {
    if (!hasStartedRef.current) return;

    // 1. Tab / Visibility Change
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'hidden') {
        reportViolation('Tab switched or browser minimized');
      }
    };

    // 2. Window Blur (Lost Focus)
    const handleBlur = () => {
      // Small delay to prevent issues with browser-native dialogs
      setTimeout(() => {
        if (document.visibilityState === 'visible' && !document.hasFocus()) {
          reportViolation('Window lost focus');
        }
      }, 100);
    };

    // 3. Fullscreen Change
    const handleFullscreenChange = () => {
      const isFull = !!(document.fullscreenElement || document.webkitFullscreenElement || document.mozFullScreenElement || document.msFullscreenElement);
      setIsFullscreen(isFull);
      if (!isFull && !isTerminatedRef.current && hasStartedRef.current) {
        reportViolation('Exited fullscreen mode');
      }
    };

    // 4. Prevention of Right-click & Keyboard Shortcuts
    const handleContextMenu = (e) => e.preventDefault();
    const handleKeyDown = (e) => {
      if (
        (e.ctrlKey && (e.key === 'c' || e.key === 'v' || e.key === 'u' || e.key === 'a' || e.key === 'p' || e.key === 's')) ||
        e.key === 'F12' ||
        (e.ctrlKey && e.shiftKey && (e.key === 'I' || e.key === 'J' || e.key === 'C')) ||
        (e.metaKey && e.altKey && e.key === 'i')
      ) {
        e.preventDefault();
      }
    };

    // 5. Back Button Prevention
    window.history.pushState(null, null, window.location.href);
    const handlePopState = () => {
      window.history.pushState(null, null, window.location.href);
    };

    // 6. Before Unload
    const handleBeforeUnload = (e) => {
      if (!isTerminatedRef.current) {
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

    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      window.removeEventListener('blur', handleBlur);
      document.removeEventListener('fullscreenchange', handleFullscreenChange);
      document.removeEventListener('contextmenu', handleContextMenu);
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('popstate', handlePopState);
      window.removeEventListener('beforeunload', handleBeforeUnload);
    };
  }, [reportViolation, isActive]); // Depend on isActive to register listeners when exam starts

  return {
    violationCount,
    showWarning,
    warningReason,
    dismissWarning: () => {
      setShowWarning(false);
      // Small timeout to allow state to update before requesting fullscreen again
      setTimeout(enterFullscreen, 100);
    },
    isFullscreen,
    enterFullscreen
  };
};
