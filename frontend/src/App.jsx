import React, { useEffect, useState, lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import LoginPage      from './features/auth/components/LoginPage';
import ProtectedRoute from './features/auth/components/ProtectedRoute';
import PublicOnlyRoute from './features/auth/components/PublicOnlyRoute';
import Spinner        from './components/ui/Spinner/Spinner';

// Lazy loaded components
const Dashboard       = lazy(() => import('./pages/Dashboard'));
const CourseSelectPage = lazy(() => import('./pages/CourseSelectPage'));
const ExamPage        = lazy(() => import('./pages/ExamPage'));
const ResultPage      = lazy(() => import('./pages/ResultPage'));
const QuestionBank    = lazy(() => import('./features/exam/components/QuestionBank/QuestionBank'));
const ExamBuilder     = lazy(() => import('./features/exam/components/ExamBuilder/ExamBuilder'));

import Navbar         from './components/layout/Navbar';
import useAuthStore   from './features/auth/store/authStore';
import useCurrentUser from './features/auth/hooks/useCurrentUser';
import useThemeMode   from './hooks/useThemeMode';
import { ROUTES }     from './constants/appConstants';
import { PromptProvider } from './context/PromptContext';
import PromptDialog     from './components/ui/PromptDialog/PromptDialog';

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
  const isExamRoute = location.pathname.startsWith('/exam/');
  
  const { themeMode, setThemeMode: updateThemeMode, effectiveTheme } = useThemeMode();

  // Listen for 401 event emitted by apiClient interceptor
  useEffect(() => {
    const handler = () => resolveAnonymous();
    window.addEventListener('auth:unauthorized', handler);
    return () => window.removeEventListener('auth:unauthorized', handler);
  }, [resolveAnonymous]);

  return (
    <div className="app-shell">
      {!isLoginRoute && !isExamRoute && (
        <Navbar
          themeMode={themeMode}
          effectiveTheme={effectiveTheme}
          onThemeModeChange={updateThemeMode}
        />
      )}
      <main className={`app-main ${isLoginRoute ? 'app-main--auth' : isExamRoute ? 'app-main--exam' : ''}`}>
        <Suspense fallback={<div className="spinner-center"><Spinner size="lg" /></div>}>
          <Routes>
            <Route element={<PublicOnlyRoute />}>
              <Route path={ROUTES.LOGIN} element={<LoginPage />} />
            </Route>

            <Route element={<ProtectedRoute />}>
              <Route path={ROUTES.DASHBOARD} element={<Dashboard />} />
              <Route path={ROUTES.COURSES} element={<CourseSelectPage />} />
              <Route path={ROUTES.EXAM} element={<ExamPage />} />
              <Route path={ROUTES.RESULT} element={<ResultPage />} />
              <Route path={ROUTES.QUESTION_BANK} element={<QuestionBank />} />
              <Route path={ROUTES.EXAM_BUILDER} element={<ExamBuilder />} />
            </Route>

            <Route path="*" element={<Navigate to={ROUTES.DASHBOARD} replace />} />
          </Routes>
        </Suspense>
      </main>
      <PromptDialog />
    </div>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <PromptProvider>
        <AppShell />
      </PromptProvider>
    </BrowserRouter>
  );
}
