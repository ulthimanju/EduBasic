import React, { useEffect, useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import LoginPage      from './features/auth/components/LoginPage';
import ProtectedRoute from './features/auth/components/ProtectedRoute';
import PublicOnlyRoute from './features/auth/components/PublicOnlyRoute';
import Dashboard      from './pages/Dashboard';
import Navbar         from './components/layout/Navbar';
import useAuthStore   from './features/auth/store/authStore';
import useCurrentUser from './features/auth/hooks/useCurrentUser';
import useThemeMode   from './hooks/useThemeMode';
import { ROUTES }     from './constants/appConstants';

/**
 * Root application component.
 *
 * Routing:
 *   /login      → LoginPage (public restricted)
 *   /           → redirect to /dashboard
 *   /dashboard  → Dashboard (protected via ProtectedRoute)
 *
 * Global: Listens for the 'auth:unauthorized' event dispatched by apiClient
 * interceptor — sets anonymous state and lets ProtectedRoute redirect to /login.
 */
function AppShell() {
  const resolveAnonymous = useAuthStore((s) => s.resolveAnonymous);
  
  // Triggers the shared auth bootstrap exactly once per app load
  useCurrentUser();

  const location = useLocation();
  const isLoginRoute = location.pathname === ROUTES.LOGIN;
  
  const { themeMode, setThemeMode: updateThemeMode, effectiveTheme } = useThemeMode();

  // Listen for 401 event emitted by apiClient interceptor
  useEffect(() => {
    const handler = () => resolveAnonymous();
    window.addEventListener('auth:unauthorized', handler);
    return () => window.removeEventListener('auth:unauthorized', handler);
  }, [resolveAnonymous]);

  return (
    <div className="app-shell">
      {!isLoginRoute && (
        <Navbar
          themeMode={themeMode}
          effectiveTheme={effectiveTheme}
          onThemeModeChange={updateThemeMode}
        />
      )}
      <main className={`app-main ${isLoginRoute ? 'app-main--auth' : ''}`}>
        <Routes>
          <Route element={<PublicOnlyRoute />}>
            <Route path={ROUTES.LOGIN} element={<LoginPage />} />
          </Route>

          <Route element={<ProtectedRoute />}>
            <Route path={ROUTES.DASHBOARD} element={<Dashboard />} />
          </Route>

          <Route path="*" element={<Navigate to={ROUTES.DASHBOARD} replace />} />
        </Routes>
      </main>
    </div>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AppShell />
    </BrowserRouter>
  );
}
