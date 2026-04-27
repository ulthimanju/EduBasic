import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { logout as logoutService } from '../services/authService';
import useAuthStore from '../../../stores/authStore';
import { ROUTES } from '../../../constants/appConstants';

/**
 * Hook that performs a full logout sequence.
 *
 * Algorithm (§9.11 of design doc):
 * 1. Call authService.logout() (server-side revocation + cookie clear)
 * 2. SUCCESS → clear authStore → navigate to /login
 * 3. FAILURE → set error (do NOT clear local state — session may still be active)
 *
 * @returns {{ logout: () => Promise<void>, isLoading: boolean, error: Error|null }}
 */
export default function useLogout() {
  const clearAuth     = useAuthStore(s => s.clearAuth);
  const navigate      = useNavigate();
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError]         = useState(null);

  async function logout() {
    setIsLoading(true);
    setError(null);
    try {
      await logoutService();
      clearAuth();
      navigate(ROUTES.LOGIN, { replace: true });
    } catch (err) {
      // Do not clear state — the session may still be active server-side
      setError(err);
    } finally {
      setIsLoading(false);
    }
  }

  return { logout, isLoading, error };
}
