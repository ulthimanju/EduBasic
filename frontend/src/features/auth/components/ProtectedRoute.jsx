import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import useCurrentUser from '../hooks/useCurrentUser';
import { ROUTES } from '../../../constants/appConstants';

/**
 * Route guard — wraps protected routes.
 *
 * Algorithm (§9.12 of design doc):
 * 1. useCurrentUser() → { isAuthenticated, isLoading }
 * 2. isLoading  → render <Spinner />
 * 3. NOT authenticated → <Navigate to="/login" replace />
 * 4. authenticated → render <Outlet /> (child routes)
 *
 * Use in React Router as:
 *   <Route element={<ProtectedRoute />}>
 *     <Route path="/dashboard" element={<Dashboard />} />
 *   </Route>
 */
export default function ProtectedRoute() {
  const { isAuthenticated, isLoading } = useCurrentUser();

  if (isLoading) {
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

  if (!isAuthenticated) {
    return <Navigate to={ROUTES.LOGIN} replace />;
  }

  return <Outlet />;
}
