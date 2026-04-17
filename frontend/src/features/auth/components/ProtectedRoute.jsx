import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import useCurrentUser from '../hooks/useCurrentUser';
import useAuthStore from '../store/authStore';
import ErrorMessage from '../../../components/ui/ErrorMessage/ErrorMessage';
import { ROUTES } from '../../../constants/appConstants';

/**
 * Route guard — wraps protected routes.
 *
 * Branching based on authStatus:
 * - idle/loading  → show skeleton
 * - anonymous     → <Navigate to="/login" replace />
 * - authenticated without user → <Navigate to="/login" replace />
 * - error         → inline ErrorMessage + Retry
 * - authenticated → <Outlet />
 */
export default function ProtectedRoute() {
  const { authStatus, retry } = useCurrentUser();
  const user = useAuthStore((state) => state.user);

  if (authStatus === 'idle' || authStatus === 'loading') {
    return (
      <div className="protected-loading page-enter">
        <div className="panel protected-skeleton">
          <div className="skeleton skeleton-line skeleton-line--lg" />
          <div className="skeleton skeleton-line" />
          <div className="skeleton skeleton-line skeleton-line--md" />
          <div className="divider" />
          <div className="skeleton skeleton-block" />
          <div className="skeleton skeleton-block" />
        </div>
      </div>
    );
  }

  if (authStatus === 'anonymous') {
    return <Navigate to={ROUTES.LOGIN} replace />;
  }

  if (authStatus === 'authenticated' && !user) {
    return <Navigate to={ROUTES.LOGIN} replace />;
  }

  if (authStatus === 'error') {
    return (
      <div className="protected-error page-enter">
        <div className="panel">
          <ErrorMessage message="Failed to load your session details." />
          <button 
            className="btn btn-secondary" 
            style={{ marginTop: '1rem' }} 
            onClick={retry}
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  return <Outlet />;
}
