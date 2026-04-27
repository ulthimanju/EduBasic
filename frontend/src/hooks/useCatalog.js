import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { catalogApi } from '../api/catalog';
import { enrollmentApi } from '../api/enrollment';
import { handleMutationError } from '../utils/errorHandlers';

export const useCatalog = ({ keyword, page = 0, size = 12 } = {}) =>
  useQuery({
    queryKey: ['catalog', keyword, page, size],
    queryFn: () => catalogApi.browse({ keyword, page, size }),
    staleTime: 1000 * 60 * 10,
    placeholderData: (prev) => prev,  // keeps previous page while loading next
  });

export const useCoursePreview = (courseId) =>
  useQuery({
    queryKey: ['catalog', 'preview', courseId],
    queryFn: () => catalogApi.getPreview(courseId),
    enabled: !!courseId,
  });

export const useEnroll = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: enrollmentApi.enroll,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-courses'] });
      queryClient.invalidateQueries({ queryKey: ['catalog'] });
    },
    onError: handleMutationError,
  });
};
