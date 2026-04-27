import apiClient from './client';

export const enrollmentApi = {
  enroll: (courseId) =>
    apiClient.post(`/api/v1/courses/${courseId}/enroll`).then(r => r.data),

  drop: (courseId) =>
    apiClient.delete(`/api/v1/courses/${courseId}/enroll`),

  getMyCourses: () =>
    apiClient.get('/api/v1/me/courses').then(r => r.data),

  getCourseOutline: (courseId) =>
    apiClient.get(`/api/v1/me/courses/${courseId}`).then(r => r.data),

  getCompletionStatus: (courseId) =>
    apiClient.get(`/api/v1/me/courses/${courseId}/completion`).then(r => r.data),
};
