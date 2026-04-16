import axios from 'axios';

/**
 * Shared Axios instance for all API calls.
 *
 * Key settings:
 * - baseURL pulled from Vite env var (VITE_API_BASE_URL)
 * - withCredentials: true  — required for browser to send HttpOnly auth cookie
 *   on cross-origin requests to the Spring Boot backend
 *
 * Interceptors:
 * - Response: on 401 redirect to /login (passive — no store import to avoid circular deps)
 */
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

// ── Response interceptor ───────────────────────────────────────────────────────
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Emit a custom DOM event so auth store can react without circular import
      window.dispatchEvent(new CustomEvent('auth:unauthorized'));
    }
    return Promise.reject(error);
  },
);

export default apiClient;
