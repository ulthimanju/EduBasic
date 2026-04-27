import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { instructorApi } from '../api/instructor';

export const useMyCoursesInstructor = () =>
  useQuery({
    queryKey: ['instructor-courses'],
    queryFn: instructorApi.listMyCourses,
  });

export const useCourseDetail = (courseId) =>
  useQuery({
    queryKey: ['instructor-course', courseId],
    queryFn: () => instructorApi.getCourse(courseId),
    enabled: !!courseId,
  });

export const useCreateCourse = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: instructorApi.createCourse,
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ['instructor-courses'] }),
  });
};

export const useUpdateCourse = (courseId) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data) => instructorApi.updateCourse(courseId, data),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ['instructor-course', courseId] }),
  });
};

export const usePublishCourse = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: instructorApi.publishCourse,
    onSuccess: (_, courseId) => {
      queryClient.invalidateQueries({ queryKey: ['instructor-course', courseId] });
      queryClient.invalidateQueries({ queryKey: ['instructor-courses'] });
    },
  });
};

export const useDeleteCourse = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: instructorApi.deleteCourse,
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ['instructor-courses'] }),
  });
};

export const useAddModule = (courseId) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data) => instructorApi.addModule(courseId, data),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ['instructor-course', courseId] }),
  });
};

export const useDeleteModule = (courseId) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: instructorApi.deleteModule,
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ['instructor-course', courseId] }),
  });
};

export const useAddLesson = (courseId) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ moduleId, data }) => instructorApi.addLesson(moduleId, data),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ['instructor-course', courseId] }),
  });
};

export const useDeleteLesson = (courseId) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: instructorApi.deleteLesson,
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ['instructor-course', courseId] }),
  });
};

export const useLinkExam = (courseId) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data) => instructorApi.linkExam(courseId, data),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ['instructor-course', courseId] }),
  });
};

export const useUnlinkExam = (courseId) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (examId) => instructorApi.unlinkExam(courseId, examId),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ['instructor-course', courseId] }),
  });
};
