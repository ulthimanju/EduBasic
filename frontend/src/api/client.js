import { createAuthenticatedClient } from './createClient';
import { API_BASE_URL, COURSE_SERVICE_BASE_URL } from '../config/runtimeConfig';

// Main client for Course Service (8083)
const apiClient = createAuthenticatedClient(COURSE_SERVICE_BASE_URL);

// Client for Auth Service (8080)
const authClient = createAuthenticatedClient(API_BASE_URL, { 
  withCredentials: true 
});

export { authClient };
export default apiClient;
