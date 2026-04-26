import { create } from 'zustand';
import examApi from '../../../services/examApi';

const useExamStore = create((set, get) => ({
  questions: [],
  totalQuestions: 0,
  currentQuestion: null,
  tags: [],
  exams: [],
  currentExam: null,
  currentAttempt: null,
  isLoading: false,
  error: null,

  setLoading: (loading) => set({ isLoading: loading }),
  setError: (error) => set({ error: error }),

  // Question Bank
  fetchQuestions: async (params) => {
    set({ isLoading: true });
    try {
      const response = await examApi.getQuestions(params);
      set({ 
        questions: response.data.content, 
        totalQuestions: response.data.totalElements,
        isLoading: false 
      });
    } catch (err) {
      set({ error: err.message, isLoading: false });
    }
  },

  fetchTags: async () => {
    try {
      const response = await examApi.getTags();
      set({ tags: response.data });
    } catch (err) {
      console.error('Failed to fetch tags', err);
    }
  },

  createQuestion: async (data) => {
    set({ isLoading: true });
    try {
      await examApi.createQuestion(data);
      get().fetchQuestions();
      get().fetchTags();
      set({ isLoading: false });
    } catch (err) {
      set({ error: err.message, isLoading: false });
      throw err;
    }
  },

  // Exam Builder
  fetchExams: async (params) => {
    set({ isLoading: true });
    try {
      const response = await examApi.getExams(params);
      set({ exams: response.data, isLoading: false });
    } catch (err) {
      set({ error: err.message, isLoading: false });
    }
  },

  fetchExam: async (id) => {
    set({ isLoading: true });
    try {
      const response = await examApi.getExam(id);
      set({ currentExam: response.data, isLoading: false });
    } catch (err) {
      set({ error: err.message, isLoading: false });
    }
  },

  createExam: async (data) => {
    set({ isLoading: true });
    try {
      const response = await examApi.createExam(data);
      set({ isLoading: false });
      return response.data;
    } catch (err) {
      set({ error: err.message, isLoading: false });
      throw err;
    }
  },

  // Student Attempts
  startAttempt: async (examId) => {
    set({ isLoading: true });
    try {
      const response = await examApi.startAttempt(examId);
      set({ currentAttempt: response.data, isLoading: false });
      return response.data;
    } catch (err) {
      set({ error: err.message, isLoading: false });
      throw err;
    }
  },

  fetchAttempt: async (id) => {
    set({ isLoading: true });
    try {
      const response = await examApi.getAttempt(id);
      set({ currentAttempt: response.data, isLoading: false });
      return response.data;
    } catch (err) {
      set({ error: err.message, isLoading: false });
      throw err;
    }
  },

  syncAttempt: async (attemptId, data) => {
    try {
      const response = await examApi.syncAttempt(attemptId, data);
      set({ currentAttempt: response.data });
      return response.data;
    } catch (err) {
      console.error('Failed to sync attempt', err);
      throw err;
    }
  },

  submitAttempt: async (attemptId) => {
    set({ isLoading: true });
    try {
      await examApi.submitAttempt(attemptId);
      set({ isLoading: false });
    } catch (err) {
      set({ error: err.message, isLoading: false });
      throw err;
    }
  }
}));

export default useExamStore;
