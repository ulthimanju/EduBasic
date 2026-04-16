import React, { useEffect, useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import LoginPage      from './features/auth/components/LoginPage';
import ProtectedRoute from './features/auth/components/ProtectedRoute';
import Dashboard      from './pages/Dashboard';
import Navbar         from './components/layout/Navbar';
import useAuthStore   from './features/auth/store/authStore';
import { ROUTES }     from './constants/appConstants';

/**
 * Root application component.
 *
 * Routing:
 *   /login      → LoginPage (public)
 *   /           → redirect to /dashboard
 *   /dashboard  → Dashboard (protected via ProtectedRoute)
 *
 * Global: Listens for the 'auth:unauthorized' event dispatched by apiClient
 * interceptor — clears auth state and lets ProtectedRoute redirect to /login.
 */
function AppShell() {
  const clear = useAuthStore((s) => s.clear);
  const location = useLocation();
  const isLoginRoute = location.pathname === ROUTES.LOGIN;
  const [themeMode, setThemeMode] = useState(() => {
    if (typeof window === 'undefined') {
      return 'system';
    }

    return window.localStorage.getItem('ui-theme-mode') ?? 'system';
  });

  const [systemTheme, setSystemTheme] = useState(() => {
    if (typeof window === 'undefined') {
      return 'dark';
    }

    return window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
  });

  // Listen for 401 event emitted by apiClient interceptor
  useEffect(() => {
    const handler = () => clear();
    window.addEventListener('auth:unauthorized', handler);
    return () => window.removeEventListener('auth:unauthorized', handler);
  }, [clear]);

  useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: light)');

    const handleChange = (event) => {
      setSystemTheme(event.matches ? 'light' : 'dark');
    };

    handleChange(mediaQuery);
    mediaQuery.addEventListener('change', handleChange);

    return () => mediaQuery.removeEventListener('change', handleChange);
  }, []);

  const effectiveTheme = themeMode === 'system' ? systemTheme : themeMode;

  useEffect(() => {
    document.documentElement.dataset.theme = effectiveTheme;
    window.localStorage.setItem('ui-theme-mode', themeMode);
  }, [effectiveTheme, themeMode]);

  const updateThemeMode = (nextThemeMode) => {
    setThemeMode(nextThemeMode);
  };

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
          <Route path={ROUTES.LOGIN} element={<LoginPage />} />

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
