import axios from 'axios';
import { EXAM_API_BASE_URL, API_BASE_URL } from '../config/runtimeConfig';
import useAuthStore from '../stores/authStore';

const examClient = axios.create({
  baseURL: EXAM_API_BASE_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

examClient.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().accessToken;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

examClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(token => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return examClient(originalRequest);
        }).catch(err => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const { data } = await axios.post(`${API_BASE_URL}/api/auth/refresh`, {}, { withCredentials: true });
        const newToken = data.accessToken;
        
        useAuthStore.getState().setAccessToken(newToken);
        processQueue(null, newToken);
        
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return examClient(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        useAuthStore.getState().clear();
        window.dispatchEvent(new CustomEvent('auth:unauthorized'));
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }
    return Promise.reject(error);
  },
);

export const examApi = {
  // Question Bank
  createQuestion: (data) => examClient.post('/api/v1/question-bank', data),
  getQuestions: (params) => examClient.get('/api/v1/question-bank', { params }),
  getQuestion: (id) => examClient.get(`/api/v1/question-bank/${id}`),
  updateQuestion: (id, data) => examClient.put(`/api/v1/question-bank/${id}`, data),
  deleteQuestion: (id) => examClient.delete(`/api/v1/question-bank/${id}`),
  getTags: () => examClient.get('/api/v1/question-bank/tags'),
  bulkImportQuestions: (data) => examClient.post('/api/v1/question-bank/bulk', data),

  // Exam Builder
  createExam: (data) => examClient.post('/api/v1/exams', data),
  getExams: (params) => examClient.get('/api/v1/exams', { params }),
  getExam: (id) => examClient.get(`/api/v1/exams/${id}`),
  publishExam: (id) => examClient.post(`/api/v1/exams/${id}/publish`),
  addSection: (examId, data) => examClient.post(`/api/v1/exams/${examId}/sections`, data),
  addQuestionToExam: (examId, data) => examClient.post(`/api/v1/exams/${examId}/questions`, data),

  // Student Attempts
  startAttempt: (examId) => examClient.post('/api/v1/attempts', { examId }),
  getAttempt: (id) => examClient.get(`/api/v1/attempts/${id}`),
  syncAttempt: (attemptId, data) => examClient.put(`/api/v1/attempts/${attemptId}/sync`, data),
  submitAttempt: (attemptId) => examClient.post(`/api/v1/attempts/${attemptId}/submit`),
  getResult: (attemptId) => examClient.get(`/api/v1/results/${attemptId}`),
  recordViolation: (attemptId, data) => examClient.post(`/api/v1/attempts/${attemptId}/violations`, data),

  // Proctoring
  logProctoringEvent: (attemptId, data) => examClient.post(`/api/v1/proctoring/attempts/${attemptId}/log`, data),
};

export default examApi;
