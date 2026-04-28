import { useEffect, useState, useCallback } from 'react';
import axios from 'axios';
import useAuthStore from '../store/authStore';
import { API_BASE_URL } from '../../../config/runtimeConfig';

/**
 * Hook to initialize auth state on app mount.
 * Renamed from useAuthBootstrap / useCurrentUser.
 */
export default function useInitAuth() {
  const [authStatus, setAuthStatus] = useState('idle'); // 'idle' | 'loading' | 'authenticated' | 'anonymous' | 'error'
  const setAuth = useAuthStore(s => s.setAuth);
  const clearAuth = useAuthStore(s => s.clearAuth);

  const bootstrap = useCallback(async () => {
    setAuthStatus('loading');
    try {
      // 1. Exchange the refresh token (in cookie) for a new access token
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

        const userData = userRes.data.data;

        if (userData) {
          setAuth({
            accessToken,
            userId: userData.id,
            email: userData.email,
            roles: userData.roles || []
          });
          setAuthStatus('authenticated');
        } else {
          clearAuth();
          setAuthStatus('anonymous');
        }
      } else {
        clearAuth();
        setAuthStatus('anonymous');
      }
    } catch (err) {
      if (err.response?.status === 401) {
        clearAuth();
        setAuthStatus('anonymous');
      } else {
        console.error('Auth bootstrap failed:', err.message);
        setAuthStatus('error');
      }
    }
  }, [setAuth, clearAuth]);

  useEffect(() => {
    bootstrap();
  }, [bootstrap]);

  return { 
    authStatus, 
    isInitializing: authStatus === 'idle' || authStatus === 'loading',
    retry: bootstrap 
  };
}
