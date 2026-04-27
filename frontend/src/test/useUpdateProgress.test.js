import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useUpdateProgress } from '../hooks/useProgress';
import { progressApi } from '../api/progress';

vi.mock('../api/progress', () => ({
  progressApi: {
    update: vi.fn(),
  },
}));

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
  return ({ children }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

describe('useUpdateProgress', () => {
  let queryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });
  });

  it('should roll back on error', async () => {
    const courseId = 'course-123';
    const lessonId = 'lesson-456';
    const initialData = {
      modules: [{
        lessons: [{ id: lessonId, progress: { progressPercent: 0, status: 'NOT_STARTED' } }]
      }]
    };

    const wrapper = ({ children }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    queryClient.setQueryData(['course-outline', courseId], initialData);

    progressApi.update.mockRejectedValue(new Error('Update failed'));

    const { result } = renderHook(() => useUpdateProgress(courseId), { wrapper });

    result.current.mutate({ lessonId, percent: 50 });

    // Expect optimistic update
    expect(queryClient.getQueryData(['course-outline', courseId]).modules[0].lessons[0].progress.progressPercent).toBe(50);

    // Wait for error and rollback
    await waitFor(() => expect(result.current.isError).toBe(true));

    expect(queryClient.getQueryData(['course-outline', courseId])).toEqual(initialData);
  });
});
