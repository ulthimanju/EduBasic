/**
 * Application-wide constants.
 * Import from here — never hardcode route strings or API paths in components.
 */

export const ROUTES = {
  LOGIN:         '/login',
  DASHBOARD:     '/my-courses',
  COURSES:       '/courses',
  EXAM:          '/exam/:attemptId',
  RESULT:        '/result/:attemptId',
  QUESTION_BANK: '/instructor/question-bank',
  EXAM_BUILDER:  '/instructor/exams',
  EXAM_DETAIL:   '/instructor/exams/:examId',
  GRADING:       '/instructor/grading/:attemptId',
  HOME:          '/',
};

export const API_PATHS = {
  ME:     '/api/auth/me',
  LOGOUT: '/api/auth/logout',
};
