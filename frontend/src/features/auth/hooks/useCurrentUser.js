import { useEffect } from 'react';
import { getCurrentUser } from '../services/authService';
import useAuthStore from '../store/authStore';

let inFlightPromise = null;

/**
 * Hook that resolves the current authenticated user.
 *
 * Uses module-scoped promise to deduplicate simultaneous bootstrap calls.
 *
 * @returns {{ user, isAuthenticated, isLoading, error, authStatus, retry }}
 */
export default function useCurrentUser() {
  const { 
    user, isAuthenticated, authStatus, authError,
    startBootstrap, resolveAuthenticated, resolveAnonymous, resolveError
  } = useAuthStore();

  const loadUser = async () => {
    startBootstrap();
    try {
      const fetchedUser = await getCurrentUser();
      if (fetchedUser) {
        resolveAuthenticated(fetchedUser);
      } else {
        resolveAnonymous();
      }
    } catch (err) {
      if (err.response?.status === 401) {
        resolveAnonymous();
      } else {
        resolveError(err);
      }
    } finally {
      inFlightPromise = null;
    }
  };

  const executeBootstrap = () => {
    const currentState = useAuthStore.getState();
    if (!inFlightPromise && currentState.authStatus === 'idle') {
      inFlightPromise = loadUser();
    }
  };

  useEffect(() => {
    executeBootstrap();
  }, []);

  const retry = () => {
    inFlightPromise = null;
    inFlightPromise = loadUser();
  };

  const isLoading = authStatus === 'idle' || authStatus === 'loading';

  return { user, isAuthenticated, isLoading, error: authError, authStatus, retry };
}
