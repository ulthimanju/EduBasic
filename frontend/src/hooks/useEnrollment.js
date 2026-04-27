import { useQuery } from '@tanstack/react-query';
import { enrollmentApi } from '../api/enrollment';

export const useMyCourses = () =>
  useQuery({
    queryKey: ['my-courses'],
    queryFn: enrollmentApi.getMyCourses,
  });

export const useCourseOutline = (courseId) =>
  useQuery({
    queryKey: ['course-outline', courseId],
    queryFn: () => enrollmentApi.getCourseOutline(courseId),
    enabled: !!courseId,
  });

export const useCompletionStatus = (courseId) =>
  useQuery({
    queryKey: ['completion-status', courseId],
    queryFn: () => enrollmentApi.getCompletionStatus(courseId),
    enabled: !!courseId,
    refetchInterval: 30_000,  // poll every 30s for completion
  });
