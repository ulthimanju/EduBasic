import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { useExamLockdown } from '../hooks/useExamLockdown';
import api from '../api/exam';
import { usePrompt } from '../context/PromptContext';
import { useNavigate } from 'react-router-dom';

vi.mock('../api/exam');
vi.mock('../context/PromptContext');
vi.mock('react-router-dom');

describe('useExamLockdown', () => {
  const attemptId = 'attempt-123';
  const openPrompt = vi.fn();
  const navigate = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    usePrompt.mockReturnValue({ openPrompt });
    useNavigate.mockReturnValue(navigate);
    
    // Mock requestFullscreen
    document.documentElement.requestFullscreen = vi.fn().mockResolvedValue();
  });

  it('should auto-submit and redirect when violations exceed limit', async () => {
    api.recordViolation.mockResolvedValue({
      data: {
        violationCount: 3,
        maxViolations: 3,
        autoSubmitted: true
      }
    });

    const onAutoSubmit = vi.fn();
    
    renderHook(() => useExamLockdown(attemptId, true, onAutoSubmit));

    // Simulate a visibility change (TAB_SWITCH)
    // We need to trigger the actual handler. 
    // In our implementation, reportViolation is internal. 
    // We trigger it via visibilitychange listener added in useEffect.
    
    // Manually trigger the listener
    const visibilityHandler = document.addEventListener.mock.calls.find(call => call[0] === 'visibilitychange')[1];
    
    // Mock document.hidden
    Object.defineProperty(document, 'hidden', { value: true, configurable: true });
    
    await visibilityHandler();

    expect(api.recordViolation).toHaveBeenCalledWith(attemptId, expect.objectContaining({
      violationType: 'TAB_SWITCH'
    }));

    await waitFor(() => {
      expect(onAutoSubmit).toHaveBeenCalled();
      expect(navigate).toHaveBeenCalledWith(expect.stringContaining('result/attempt-123'), expect.any(Object));
    });
  });
});
