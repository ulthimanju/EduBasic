import { create } from 'zustand';
import examApi from '../../../api/exam';

const useExamStore = create((set, get) => ({
  questions: [],
  totalQuestions: 0,
  currentQuestion: null,
  tags: [],
  exams: [],
  currentExam: null,
  currentAttempt: null,
  currentResult: null,
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
        questions: response.content, 
        totalQuestions: response.totalElements,
        isLoading: false 
      });
    } catch (err) {
      set({ error: err.message, isLoading: false });
    }
  },

  fetchTags: async () => {
    try {
      const response = await examApi.getTags();
      set({ tags: response });
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
      set({ exams: response, isLoading: false });
    } catch (err) {
      set({ error: err.message, isLoading: false });
    }
  },

  fetchExam: async (id) => {
    set({ isLoading: true });
    try {
      const response = await examApi.getExam(id);
      set({ currentExam: response, isLoading: false });
    } catch (err) {
      set({ error: err.message, isLoading: false });
    }
  },

  createExam: async (data) => {
    set({ isLoading: true });
    try {
      const response = await examApi.createExam(data);
      set({ isLoading: false });
      return response;
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
      set({ currentAttempt: response, isLoading: false });
      return response;
    } catch (err) {
      set({ error: err.message, isLoading: false });
      throw err;
    }
  },

  fetchAttempt: async (id) => {
    set({ isLoading: true });
    try {
      const response = await examApi.getAttempt(id);
      set({ currentAttempt: response, isLoading: false });
      return response;
    } catch (err) {
      set({ error: err.message, isLoading: false });
      throw err;
    }
  },

  syncAttempt: async (attemptId, data) => {
    try {
      const response = await examApi.syncAttempt(attemptId, data);
      set({ currentAttempt: response });
      return response;
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
  },

  fetchResult: async (attemptId) => {
    set({ isLoading: true });
    try {
      const response = await examApi.getResult(attemptId);
      set({ currentResult: response, isLoading: false });
      return response;
    } catch (err) {
      set({ error: err.message, isLoading: false });
      throw err;
    }
  }
}));

export default useExamStore;
