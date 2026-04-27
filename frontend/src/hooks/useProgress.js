import { useMutation, useQueryClient } from '@tanstack/react-query';
import { progressApi } from '../api/progress';

export const useUpdateProgress = (courseId) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ lessonId, percent }) => progressApi.update(lessonId, percent),
    onMutate: async ({ lessonId, percent }) => {
      // Optimistic update — sidebar reflects immediately
      await queryClient.cancelQueries({ queryKey: ['course-outline', courseId] });
      const prev = queryClient.getQueryData(['course-outline', courseId]);
      queryClient.setQueryData(['course-outline', courseId], (old) => {
        if (!old) return old;
        return {
          ...old,
          modules: old.modules.map((mod) => ({
            ...mod,
            lessons: mod.lessons.map((lesson) =>
              lesson.id === lessonId
                ? {
                    ...lesson,
                    progress: {
                      ...lesson.progress,
                      progressPercent: percent,
                      status:
                        percent === 100
                          ? 'COMPLETED'
                          : percent > 0
                          ? 'IN_PROGRESS'
                          : 'NOT_STARTED',
                    },
                  }
                : lesson
            ),
          })),
        };
      });
      return { prev };
    },
    onError: (_err, _vars, context) => {
      // Roll back on failure
      if (context?.prev) {
        queryClient.setQueryData(['course-outline', courseId], context.prev);
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['course-outline', courseId] });
      queryClient.invalidateQueries({ queryKey: ['my-courses'] });
      queryClient.invalidateQueries({ queryKey: ['completion-status', courseId] });
    },
  });
};
