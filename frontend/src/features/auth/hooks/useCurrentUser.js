import { useEffect, useState } from 'react';
import axios from 'axios';
import useAuthStore from '../../../stores/authStore';
import { API_BASE_URL } from '../../../config/runtimeConfig';

/**
 * Hook to bootstrap auth state on app load.
 */
export default function useAuthBootstrap() {
  const [isInitializing, setIsInitializing] = useState(true);
  const setAuth = useAuthStore(s => s.setAuth);
  const clearAuth = useAuthStore(s => s.clearAuth);

  useEffect(() => {
    const bootstrap = async () => {
      try {
        // 1. Exchange the refresh token (in cookie) for a new access token
        // Use axios directly to avoid interceptors and control headers precisely
        const refreshRes = await axios.post(
          `${API_BASE_URL}/api/auth/refresh`,
          {},
          { withCredentials: true }
        );

        const { accessToken } = refreshRes.data;

        if (accessToken) {
          // 2. Use the new access token to fetch user profile
          const userRes = await axios.get(`${API_BASE_URL}/api/auth/me`, {
            headers: { Authorization: `Bearer ${accessToken}` }
          });

          // API response format is { success: boolean, data: UserDTO, message: string }
          const userData = userRes.data.data;

          if (userData) {
            setAuth({
              accessToken,
              userId: userData.id,
              email: userData.email,
              roles: userData.roles || []
            });
          } else {
            clearAuth();
          }
        } else {
          clearAuth();
        }
      } catch (err) {
        // Only log if it's not a 401 (which just means no active session)
        if (err.response?.status !== 401) {
          console.error('Auth bootstrap failed:', err.message);
        }
        clearAuth();
      } finally {
        setIsInitializing(false);
      }
    };

    bootstrap();
  }, [setAuth, clearAuth]);

  return { isInitializing };
}
