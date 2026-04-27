import apiClient from './client';

export const catalogApi = {
  browse: ({ keyword, page = 0, size = 12 } = {}) =>
    apiClient.get('/api/v1/catalog', { params: { keyword, page, size } })
      .then(r => r.data),

  getPreview: (courseId) =>
    apiClient.get(`/api/v1/catalog/${courseId}`).then(r => r.data),
};
