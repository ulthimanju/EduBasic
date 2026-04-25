import axios from 'axios';
import { EXAM_API_BASE_URL, API_BASE_URL } from '../config/runtimeConfig';
import useAuthStore from '../features/auth/store/authStore';

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
        // Always refresh against the AUTH service
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
  getCourses: () => examClient.get('/api/courses'),
  startExam: (courseId) => examClient.post('/api/exam/start', { courseId }),
  getQuestion: (sessionId) => examClient.get(`/api/exam/${sessionId}/question`),
  submitAnswer: (sessionId, answer) => examClient.post(`/api/exam/${sessionId}/answer`, answer),
  getResult: (sessionId) => examClient.get(`/api/exam/${sessionId}/result`),
  reportViolation: (sessionId, reason) => examClient.post(`/api/exam/${sessionId}/violation`, { reason }),
  terminateSession: (sessionId, reason) => examClient.post(`/api/exam/${sessionId}/terminate`, { reason }),
};

export default examApi;
