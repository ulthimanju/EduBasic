import apiClient from '../../../services/apiClient';
import { API_PATHS } from '../../../constants/appConstants';

/**
 * Atomic API call functions for the auth feature.
 *
 * Rules (§9.9 of design doc):
 * - Each function is a single HTTP call.
 * - No state mutation. No navigation. No side effects.
 * - Those concerns belong in hooks, not here.
 */

/**
 * Fetch the currently authenticated user's profile.
 *
 * @returns {Promise<{id: string, email: string, name: string}>}
 * @throws  Axios error on 401 or network failure
 */
export async function getCurrentUser() {
  const response = await apiClient.get(API_PATHS.ME);
  return response.data.data;
}

/**
 * Log out the current user (server-side session revocation + cookie clear).
 *
 * @returns {Promise<void>}
 * @throws  Axios error on failure
 */
export async function logout() {
  await apiClient.post(API_PATHS.LOGOUT);
}
