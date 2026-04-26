import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import ExamPage from '../ExamPage';
import useExamStore from '../../../store/examStore';
import { usePrompt } from '../../../../../context/PromptContext';

const mockNavigate = vi.fn();

// Mock the store
vi.mock('../../../store/examStore', () => ({
  default: vi.fn(),
}));

// Mock the prompt context
vi.mock('../../../../../context/PromptContext', () => ({
  usePrompt: vi.fn(),
}));

// Mock the lockdown hook
vi.mock('../../../../../hooks/useExamLockdown', () => ({
  useExamLockdown: vi.fn(),
}));

// Mock useNavigate correctly at top level
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({ attemptId: 'attempt-1' })
  };
});

describe('ExamPage', () => {
  const mockFetchExam = vi.fn();
  const mockFetchAttempt = vi.fn();
  const mockSyncAttempt = vi.fn();
  const mockSubmitAttempt = vi.fn();
  const mockOpenPrompt = vi.fn();

  const mockAttempt = {
    id: 'attempt-1',
    examId: 'exam-1',
    version: 0
  };

  const mockExam = {
    id: 'exam-1',
    title: 'Test Exam',
    timeLimitMins: 60,
    questions: [
      {
        id: 'm1',
        marks: 10,
        question: {
          id: 'q1',
          title: 'Question 1',
          description: 'Desc 1',
          type: 'MCQ_SINGLE',
          payload: { options: [{ id: 'o1', text: 'Opt 1' }] }
        }
      },
      {
        id: 'm2',
        marks: 10,
        question: {
          id: 'q2',
          title: 'Question 2',
          description: 'Desc 2',
          type: 'MCQ_SINGLE',
          payload: { options: [{ id: 'o1', text: 'Opt 1' }] }
        }
      }
    ]
  };

  beforeEach(() => {
    vi.clearAllMocks();
    
    useExamStore.mockReturnValue({
      fetchExam: mockFetchExam,
      fetchAttempt: mockFetchAttempt,
      syncAttempt: mockSyncAttempt,
      submitAttempt: mockSubmitAttempt,
      currentExam: mockExam,
      isLoading: false
    });

    usePrompt.mockReturnValue({
      openPrompt: mockOpenPrompt
    });

    mockFetchAttempt.mockResolvedValue(mockAttempt);
    mockFetchExam.mockResolvedValue(mockExam);
  });

  const renderExamPage = () => {
    return render(
      <MemoryRouter initialEntries={['/exams/attempt-1']}>
        <Routes>
          <Route path="/exams/:attemptId" element={<ExamPage />} />
        </Routes>
      </MemoryRouter>
    );
  };

  it('loads attempt and exam on mount', async () => {
    renderExamPage();
    
    await waitFor(() => {
      expect(mockFetchAttempt).toHaveBeenCalledWith('attempt-1');
      expect(mockFetchExam).toHaveBeenCalledWith('exam-1');
    });

    expect(screen.getByText('Question 1')).toBeInTheDocument();
  });

  it('navigates between questions', async () => {
    renderExamPage();
    
    await waitFor(() => screen.getByText('Question 1'));

    const nextButton = screen.getByText(/next/i);
    fireEvent.click(nextButton);

    expect(screen.getByText('Question 2')).toBeInTheDocument();

    const prevButton = screen.getByText(/previous/i);
    fireEvent.click(prevButton);

    expect(screen.getByText('Question 1')).toBeInTheDocument();
  });

  it('updates answers and tracks dirty state', async () => {
    renderExamPage();
    
    await waitFor(() => screen.getByText('Question 1'));

    const option = screen.getByText('Opt 1');
    fireEvent.click(option);

    // Manual Save
    const saveButton = screen.getByText(/save/i);
    mockSyncAttempt.mockResolvedValue({ version: 1 });
    
    await act(async () => {
      fireEvent.click(saveButton);
    });

    expect(mockSyncAttempt).toHaveBeenCalledWith('attempt-1', expect.objectContaining({
      version: 0,
      answers: { q1: 'o1' }
    }));
  });

  it('calls submit API when confirmed', async () => {
    renderExamPage();
    
    await waitFor(() => screen.getByText('Question 1'));

    const submitButton = screen.getByText(/submit/i);
    fireEvent.click(submitButton);

    expect(mockOpenPrompt).toHaveBeenCalledWith(expect.objectContaining({
      title: 'Submit Exam?',
      onConfirm: expect.any(Function)
    }));

    // Simulate confirmation
    const onConfirm = mockOpenPrompt.mock.calls[0][0].onConfirm;
    mockSubmitAttempt.mockResolvedValue({});
    
    await act(async () => {
      onConfirm();
    });

    expect(mockSubmitAttempt).toHaveBeenCalledWith('attempt-1');
    expect(mockNavigate).toHaveBeenCalled();
  });

  it('handles auto-submit when timer runs out', async () => {
    // Override exam for quick timer
    const shortExam = { ...mockExam, timeLimitMins: 0.01 }; // Very short
    useExamStore.mockReturnValue({
      fetchExam: mockFetchExam,
      fetchAttempt: mockFetchAttempt,
      syncAttempt: mockSyncAttempt,
      submitAttempt: mockSubmitAttempt,
      currentExam: shortExam,
      isLoading: false
    });

    vi.useFakeTimers();
    renderExamPage();
    
    await act(async () => {
      vi.advanceTimersByTime(1000); // 1s
    });

    expect(mockOpenPrompt).toHaveBeenCalledWith(expect.objectContaining({
      title: 'Time is up!'
    }));
    
    vi.useRealTimers();
  });
});
