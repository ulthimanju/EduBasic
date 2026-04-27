import apiClient from '../../../api/client';
import { API_PATHS } from '../../../constants/appConstants';

/**
 * Atomic API call functions for the auth feature.
 */

/**
 * Fetch the currently authenticated user's profile.
 *
 * @returns {Promise<{id: string, email: string, name: string}>}
 */
export async function getCurrentUser() {
  const response = await apiClient.get(API_PATHS.ME);
  return response.data.data;
}

/**
 * Exchange the refresh token (in cookie) for a new access token.
 *
 * @returns {Promise<{accessToken: string, expiresIn: number}>}
 */
export async function refresh() {
  const response = await apiClient.post('/api/auth/refresh');
  return response.data;
}

/**
 * Log out the current user (server-side session revocation + cookie clear).
 *
 * @returns {Promise<void>}
 */
export async function logout() {
  await apiClient.post(API_PATHS.LOGOUT);
}
