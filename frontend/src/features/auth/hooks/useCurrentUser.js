import { useEffect, useState } from 'react';
import { getCurrentUser } from '../services/authService';
import useAuthStore from '../store/authStore';

/**
 * Hook that resolves the current authenticated user on mount.
 *
 * Algorithm (§9.10 of design doc):
 * 1. Set isLoading = true on mount
 * 2. Call authService.getCurrentUser()
 * 3. SUCCESS → authStore.setUser(user), isLoading = false
 * 4. 401 error → authStore.clear(), isLoading = false
 * 5. Other error → set error state, isLoading = false
 *
 * @returns {{ user, isAuthenticated, isLoading, error }}
 */
export default function useCurrentUser() {
  const { user, isAuthenticated, setUser, clear } = useAuthStore();
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError]         = useState(null);

  useEffect(() => {
    let cancelled = false;

    async function fetchUser() {
      try {
        const fetchedUser = await getCurrentUser();
        if (!cancelled) {
          setUser(fetchedUser);
        }
      } catch (err) {
        if (!cancelled) {
          if (err.response?.status === 401) {
            clear();
          } else {
            setError(err);
          }
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    fetchUser();

    return () => {
      cancelled = true;
    };
  }, []); // runs once on mount

  return { user, isAuthenticated, isLoading, error };
}
