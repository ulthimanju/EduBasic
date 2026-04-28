import axios from 'axios';
import useAuthStore from '../stores/authStore';
import { API_BASE_URL, COURSE_SERVICE_BASE_URL } from '../config/runtimeConfig';

// Main client for Course Service (8083)
const apiClient = axios.create({
  baseURL: COURSE_SERVICE_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

// Client for Auth Service (8080)
const authClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
});

const addAuthInterceptor = (client) => {
  client.interceptors.request.use((config) => {
    const token = useAuthStore.getState().getAccessToken();
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
  });

  client.interceptors.response.use(
    (res) => res,
    async (error) => {
      if (error.response?.status === 401) {
        useAuthStore.getState().clearAuth();
        // Only redirect to login if we're not already on the login page
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
      }
      return Promise.reject(error);
    }
  );
};

addAuthInterceptor(apiClient);
addAuthInterceptor(authClient);

export { authClient };
export default apiClient;
