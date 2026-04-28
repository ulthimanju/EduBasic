import axios from 'axios';
import useAuthStore from '../features/auth/store/authStore';

let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

/**
 * Factory to create authenticated Axios instances.
 * 
 * @param {string} baseURL - Base URL for the client.
 * @param {object} options - Additional axios configuration.
 * @param {boolean} options.withRefreshQueue - Whether to enable queued retry on 401.
 */
export const createAuthenticatedClient = (baseURL, { withRefreshQueue = false, ...axiosOptions } = {}) => {
  const client = axios.create({
    baseURL,
    headers: { 'Content-Type': 'application/json' },
    ...axiosOptions
  });

  client.interceptors.request.use((config) => {
    const token = useAuthStore.getState().getAccessToken?.() || useAuthStore.getState().accessToken;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  });

  client.interceptors.response.use(
    (response) => response,
    async (error) => {
      const originalRequest = error.config;

      if (error.response?.status === 401 && !originalRequest._retry) {
        // If we don't need refresh queue, just clear auth and redirect (standard behavior)
        if (!withRefreshQueue) {
          useAuthStore.getState().clearAuth();
          if (window.location.pathname !== '/login') {
            window.location.href = '/login';
          }
          return Promise.reject(error);
        }

        // Handle queued refresh (mostly for Exam Client to prevent losing state)
        if (isRefreshing) {
          return new Promise((resolve, reject) => {
            failedQueue.push({ resolve, reject });
          }).then(token => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return client(originalRequest);
          }).catch(err => Promise.reject(err));
        }

        originalRequest._retry = true;
        isRefreshing = true;

        try {
          // Use a fresh axios instance for refresh to avoid interceptor loops
          const { data } = await axios.post(`${process.env.VITE_API_BASE_URL || ''}/api/auth/refresh`, {}, { withCredentials: true });
          const newToken = data.accessToken;
          
          useAuthStore.getState().setAccessToken(newToken);
          processQueue(null, newToken);
          
          originalRequest.headers.Authorization = `Bearer ${newToken}`;
          return client(originalRequest);
        } catch (refreshError) {
          processQueue(refreshError, null);
          useAuthStore.getState().clearAuth();
          window.dispatchEvent(new CustomEvent('auth:unauthorized'));
          return Promise.reject(refreshError);
        } finally {
          isRefreshing = false;
        }
      }

      return Promise.reject(error);
    }
  );

  return client;
};
