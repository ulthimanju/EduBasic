import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import useCurrentUser from '../hooks/useCurrentUser';
import useAuthStore from '../store/authStore';
import ErrorMessage from '../../../components/ui/ErrorMessage/ErrorMessage';
import { ROUTES } from '../../../constants/appConstants';

/**
 * Route guard — wraps public-only routes like /login.
 *
 * Branching based on authStatus:
 * - idle/loading  → show lightweight skeleton
 * - authenticated with user → <Navigate to="/dashboard" replace />
 * - anonymous     → <Outlet />
 * - error         → inline ErrorMessage + Retry + <Outlet />
 */
export default function PublicOnlyRoute() {
  const { authStatus, retry } = useCurrentUser();
  const user = useAuthStore((state) => state.user);

  if (authStatus === 'idle' || authStatus === 'loading') {
    return (
      <div className="public-loading page-enter" style={{ display: 'flex', justifyContent: 'center', marginTop: '4rem' }}>
        <div className="panel protected-skeleton" style={{ width: '100%', maxWidth: '400px' }}>
          <div className="skeleton skeleton-line skeleton-line--lg" />
          <div className="skeleton skeleton-line" />
          <div className="skeleton skeleton-line skeleton-line--md" />
        </div>
      </div>
    );
  }

  if (authStatus === 'authenticated' && user) {
    return <Navigate to={ROUTES.DASHBOARD} replace />;
  }

  if (authStatus === 'error') {
    return (
      <div className="public-error-wrapper page-enter" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', marginTop: '2rem' }}>
        <div className="panel" style={{ width: '100%', maxWidth: '400px', marginBottom: '1rem' }}>
          <ErrorMessage message="Failed to check session status." />
          <button 
            className="btn btn-secondary" 
            style={{ marginTop: '1rem' }} 
            onClick={retry}
          >
            Retry
          </button>
        </div>
        <Outlet />
      </div>
    );
  }

  return <Outlet />;
}
