import apiClient from './client';

export const progressApi = {
  update: (lessonId, progressPercent) =>
    apiClient.put(`/api/v1/lessons/${lessonId}/progress`, { progressPercent })
      .then(r => r.data),

  get: (lessonId) =>
    apiClient.get(`/api/v1/lessons/${lessonId}/progress`).then(r => r.data),
};
