import axios from 'axios';
import { EXAM_API_BASE_URL } from '../config/runtimeConfig';

const examClient = axios.create({
  baseURL: EXAM_API_BASE_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

examClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      window.dispatchEvent(new CustomEvent('auth:unauthorized'));
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
