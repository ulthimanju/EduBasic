import { render, screen, act } from '@testing-library/react';
import LessonContent from './LessonContent';
import { useUpdateProgress } from '../../../hooks/useProgress';
import { expect, test, vi, beforeEach } from 'vitest';

vi.mock('../../../hooks/useProgress');

const mockLesson = {
  id: 'lesson-1',
  title: 'Test Lesson',
  contentType: 'TEXT',
  contentBody: '<p>Hello World</p>',
  progress: { status: 'NOT_STARTED' }
};

beforeEach(() => {
  vi.useFakeTimers();
  useUpdateProgress.mockReturnValue({
    mutate: vi.fn(),
    isPending: false
  });
});

test('renders text content correctly', () => {
  render(<LessonContent lesson={mockLesson} courseId="course-1" />);
  expect(screen.getByText('Hello World')).toBeInTheDocument();
});

test('triggers auto-complete after 30s for TEXT', async () => {
  const mutate = vi.fn();
  useUpdateProgress.mockReturnValue({ mutate, isPending: false });

  render(<LessonContent lesson={mockLesson} courseId="course-1" />);
  
  act(() => {
    vi.advanceTimersByTime(30000);
  });

  expect(mutate).toHaveBeenCalledWith({ lessonId: 'lesson-1', percent: 100 });
});

test('renders iframe for LINK content', () => {
  const linkLesson = { ...mockLesson, contentType: 'LINK', contentUrl: 'https://example.com' };
  render(<LessonContent lesson={linkLesson} courseId="course-1" />);
  
  const iframe = screen.getByTitle('Test Lesson');
  expect(iframe).toHaveAttribute('src', 'https://example.com');
  expect(screen.getByText(/open in new tab/i)).toBeInTheDocument();
});
