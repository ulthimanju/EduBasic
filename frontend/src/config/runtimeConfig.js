const DEFAULT_API_BASE_URL = 'http://localhost:8080';

export function resolveApiBaseUrl(rawValue = import.meta.env.VITE_API_BASE_URL) {
  if (typeof rawValue !== 'string') {
    return DEFAULT_API_BASE_URL;
  }

  const trimmedValue = rawValue.trim();
  if (!trimmedValue) {
    return DEFAULT_API_BASE_URL;
  }

  return trimmedValue.replace(/\/+$/, '');
}

export const API_BASE_URL = resolveApiBaseUrl();
