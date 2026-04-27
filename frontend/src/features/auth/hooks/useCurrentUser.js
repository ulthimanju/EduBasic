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
        // Attempt to get user info / refresh token from secure cookie session
        const { data } = await axios.get(`${API_BASE_URL}/api/v1/me`, { withCredentials: true });
        if (data) {
          setAuth({
            accessToken: data.accessToken,
            userId: data.id,
            email: data.email,
            roles: data.roles || []
          });
        } else {
          clearAuth();
        }
      } catch (err) {
        clearAuth();
      } finally {
        setIsInitializing(false);
      }
    };

    bootstrap();
  }, [setAuth, clearAuth]);

  return { isInitializing };
}
