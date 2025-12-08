/**
 * API Client utility for making requests to backend.
 * Uses relative URLs so environment handles routing:
 * - Dev: Vite proxy routes to http://localhost:8080
 * - Prod: Same origin as React app (or use VITE_API_BASE_URL env var)
 */

const getApiBaseUrl = () => {
  // Check if a custom API base URL is set via env vars
  if (import.meta.env.VITE_API_BASE_URL) {
    return import.meta.env.VITE_API_BASE_URL;
  }
  // Default: use relative URLs (environment handles routing)
  return '';
};

/**
 * Helper to get auth token from localStorage (with SSR guard)
 */
export const getAuthToken = () => {
  if (typeof window !== 'undefined') {
    return localStorage.getItem('authToken');
  }
  return null;
};

/**
 * Generic fetch wrapper with auth token and base URL
 */
export const apiFetch = async (endpoint, options = {}) => {
  const baseUrl = getApiBaseUrl();
  const url = `${baseUrl}${endpoint}`;
  
  const token = getAuthToken();
  
  // For FormData, don't set any headers except Authorization
  // Browser will automatically set Content-Type with boundary
  const isFormData = options.body instanceof FormData;
  
  const headers = isFormData ? {} : {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  return fetch(url, {
    ...options,
    headers,
  });
};

/**
 * Common API endpoints
 */
export const apiEndpoints = {
  // Auth
  auth: {
    login: '/api/auth/login',
    register: '/api/auth/register',
    logout: '/api/auth/logout',
    me: '/api/auth/me',
  },
  // Profile
  profile: {
    settings: '/api/profile/settings',
  },
  // Announcements
  announcements: {
    list: '/api/announcements',
    get: (id) => `/api/announcements/${id}`,
    create: '/api/announcements',
    update: (id) => `/api/announcements/${id}`,
    delete: (id) => `/api/announcements/${id}`,
  },
  // Courses
  courses: {
    list: '/api/courses',
    get: (courseId) => `/api/courses/${courseId}`,
    levels: (courseId) => `/api/courses/${courseId}/levels`,
    modules: (courseId, levelId) => `/api/courses/${courseId}/levels/${levelId}/modules`,
    lessons: (courseId, levelId, moduleId) => `/api/courses/${courseId}/levels/${levelId}/modules/${moduleId}/lessons`,
    lesson: (courseId, levelId, moduleId, lessonId) => `/api/courses/${courseId}/levels/${levelId}/modules/${moduleId}/lessons/${lessonId}`,
    fixVisualization: (courseId, levelId, moduleId, lessonId) => `/api/courses/${courseId}/levels/${levelId}/modules/${moduleId}/lessons/${lessonId}/fix-visualization`,
    practiceProblem: (courseId, levelId, moduleId, lessonId) => `/api/courses/${courseId}/levels/${levelId}/modules/${moduleId}/lessons/${lessonId}/practice-problem`,
  },
  // Upload
  upload: '/api/upload',
};

export default apiFetch;
