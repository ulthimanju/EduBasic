const DEFAULT_API_BASE_URL = 'http://localhost:8080';
const DEFAULT_EXAM_API_BASE_URL = 'http://localhost:8081';
const DEFAULT_COURSE_SERVICE_URL = 'http://localhost:8083';

export function resolveApiBaseUrl(rawValue, defaultValue) {
  if (typeof rawValue !== 'string') {
    return defaultValue;
  }

  const trimmedValue = rawValue.trim();
  if (!trimmedValue) {
    return defaultValue;
  }

  return trimmedValue.replace(/\/+$/, '');
}

export const API_BASE_URL = resolveApiBaseUrl(import.meta.env.VITE_API_BASE_URL, DEFAULT_API_BASE_URL);
export const EXAM_API_BASE_URL = resolveApiBaseUrl(import.meta.env.VITE_EXAM_API_BASE_URL, DEFAULT_EXAM_API_BASE_URL);
export const COURSE_SERVICE_BASE_URL = resolveApiBaseUrl(import.meta.env.VITE_COURSE_SERVICE_URL, DEFAULT_COURSE_SERVICE_URL);
