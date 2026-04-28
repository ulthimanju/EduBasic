import examClient from './examClient';

export const examApi = {
  // Question Bank
  createQuestion: (data) => examClient.post('/api/v1/question-bank', data).then(r => r.data),
  getQuestions: (params) => examClient.get('/api/v1/question-bank', { params }).then(r => r.data),
  getQuestion: (id) => examClient.get(`/api/v1/question-bank/${id}`).then(r => r.data),
  updateQuestion: (id, data) => examClient.put(`/api/v1/question-bank/${id}`, data).then(r => r.data),
  deleteQuestion: (id) => examClient.delete(`/api/v1/question-bank/${id}`).then(r => r.data),
  getTags: () => examClient.get('/api/v1/question-bank/tags').then(r => r.data),
  bulkImportQuestions: (data) => examClient.post('/api/v1/question-bank/bulk', data).then(r => r.data),

  // Exam Builder
  createExam: (data) => examClient.post('/api/v1/exams', data).then(r => r.data),
  getExams: (params) => examClient.get('/api/v1/exams', { params }).then(r => r.data),
  getExam: (id) => examClient.get(`/api/v1/exams/${id}`).then(r => r.data),
  publishExam: (id) => examClient.post(`/api/v1/exams/${id}/publish`).then(r => r.data),
  addSection: (examId, data) => examClient.post(`/api/v1/exams/${examId}/sections`, data).then(r => r.data),
  addQuestionToExam: (examId, data) => examClient.post(`/api/v1/exams/${examId}/questions`, data).then(r => r.data),

  // Student Attempts
  startAttempt: (examId) => examClient.post('/api/v1/attempts', { examId }).then(r => r.data),
  getAttempt: (id) => examClient.get(`/api/v1/attempts/${id}`).then(r => r.data),
  syncAttempt: (attemptId, data) => examClient.put(`/api/v1/attempts/${attemptId}/sync`, data).then(r => r.data),
  submitAttempt: (attemptId) => examClient.post(`/api/v1/attempts/${attemptId}/submit`).then(r => r.data),
  getResult: (attemptId) => examClient.get(`/api/v1/results/${attemptId}`).then(r => r.data),
  recordViolation: (attemptId, data) => examClient.post(`/api/v1/attempts/${attemptId}/violations`, data).then(r => r.data),

  // Proctoring
  logProctoringEvent: (attemptId, data) => examClient.post(`/api/v1/proctoring/attempts/${attemptId}/log`, data).then(r => r.data),
};

export default examApi;
