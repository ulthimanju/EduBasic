/**
 * Application-wide constants.
 * Import from here — never hardcode route strings or API paths in components.
 */

export const ROUTES = {
  LOGIN:         '/login',
  DASHBOARD:     '/dashboard',
  COURSES:       '/courses',
  EXAM:          '/exam/:sessionId',
  RESULT:        '/result/:sessionId',
  HOME:          '/',
};

export const API_PATHS = {
  ME:     '/api/auth/me',
  LOGOUT: '/api/auth/logout',
};
