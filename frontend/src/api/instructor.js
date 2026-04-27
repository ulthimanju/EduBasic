import apiClient from './client';

export const instructorApi = {
  createCourse: (data) =>
    apiClient.post('/api/v1/courses', data).then(r => r.data),

  listMyCourses: () =>
    apiClient.get('/api/v1/courses').then(r => r.data),

  getCourse: (courseId) =>
    apiClient.get(`/api/v1/courses/${courseId}`).then(r => r.data),

  updateCourse: (courseId, data) =>
    apiClient.put(`/api/v1/courses/${courseId}`, data).then(r => r.data),

  deleteCourse: (courseId) =>
    apiClient.delete(`/api/v1/courses/${courseId}`),

  publishCourse: (courseId) =>
    apiClient.post(`/api/v1/courses/${courseId}/publish`).then(r => r.data),

  archiveCourse: (courseId) =>
    apiClient.post(`/api/v1/courses/${courseId}/archive`).then(r => r.data),

  addModule: (courseId, data) =>
    apiClient.post(`/api/v1/courses/${courseId}/modules`, data).then(r => r.data),

  updateModule: (moduleId, data) =>
    apiClient.put(`/api/v1/modules/${moduleId}`, data).then(r => r.data),

  deleteModule: (moduleId) =>
    apiClient.delete(`/api/v1/modules/${moduleId}`),

  addLesson: (moduleId, data) =>
    apiClient.post(`/api/v1/modules/${moduleId}/lessons`, data).then(r => r.data),

  updateLesson: (lessonId, data) =>
    apiClient.put(`/api/v1/lessons/${lessonId}`, data).then(r => r.data),

  deleteLesson: (lessonId) =>
    apiClient.delete(`/api/v1/lessons/${lessonId}`),

  linkExam: (courseId, data) =>
    apiClient.post(`/api/v1/courses/${courseId}/exams`, data).then(r => r.data),

  unlinkExam: (courseId, examId) =>
    apiClient.delete(`/api/v1/courses/${courseId}/exams/${examId}`),
};
