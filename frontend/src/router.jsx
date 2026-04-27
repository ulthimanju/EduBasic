import React from 'react';
import { createBrowserRouter, RouterProvider, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import RoleGuard from './components/layout/RoleGuard/RoleGuard';
import LoginPage from './features/auth/components/LoginPage';
import CourseCatalogPage from './pages/CourseCatalogPage/CourseCatalogPage';
import CoursePreviewPage from './pages/CoursePreviewPage/CoursePreviewPage';
import LessonPlayerPage from './pages/LessonPlayerPage/LessonPlayerPage';
import MyCoursesPage from './pages/MyCoursesPage/MyCoursesPage';
import InstructorDashboardPage from './pages/InstructorDashboardPage/InstructorDashboardPage';
import CourseBuilderPage from './pages/CourseBuilderPage/CourseBuilderPage';
import QuestionBankPage from './pages/QuestionBankPage/QuestionBankPage';
import ResultPage from './pages/ResultPage/ResultPage';
import useAuthBootstrap from './features/auth/hooks/useCurrentUser';
import Spinner from './components/common/Spinner/Spinner';
import ConfirmModal from './components/common/ConfirmModal/ConfirmModal';
import useUiStore from './stores/uiStore';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  
  // Student routes
  {
    element: <RoleGuard />,
    children: [
      { path: '/',                                     element: <Navigate to="/courses" replace /> },
      { path: '/courses',                              element: <CourseCatalogPage /> },
      { path: '/courses/:courseId',                    element: <CoursePreviewPage /> },
      { path: '/courses/:courseId/learn',              element: <LessonPlayerPage /> },
      { path: '/courses/:courseId/learn/:lessonId',    element: <LessonPlayerPage /> },
      { path: '/my-courses',                           element: <MyCoursesPage /> },
      { path: '/assessments',                          element: <Navigate to="/my-courses" replace /> },
    ]
  },

  // Instructor routes
  {
    element: <RoleGuard role="INSTRUCTOR" />,
    children: [
      { path: '/instructor/courses',               element: <InstructorDashboardPage /> },
      { path: '/instructor/courses/new',           element: <CourseBuilderPage /> },
      { path: '/instructor/courses/:courseId',     element: <CourseBuilderPage /> },
      { path: '/instructor/question-bank',         element: <QuestionBankPage /> },
    ],
  },

  { path: '*', element: <Navigate to="/courses" replace /> }
]);

function AppContent() {
  const { isInitializing } = useAuthBootstrap();
  const { confirmModal, closeConfirmModal } = useUiStore();

  if (isInitializing) {
    return (
      <div style={{ height: '100vh', display: 'flex', alignItems: 'center', justifyItems: 'center' }}>
        <Spinner size="lg" />
      </div>
    );
  }

  return (
    <>
      <RouterProvider router={router} />
      {confirmModal && (
        <ConfirmModal 
          {...confirmModal} 
          onCancel={confirmModal.onCancel || closeConfirmModal}
        />
      )}
    </>
  );
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AppContent />
    </QueryClientProvider>
  );
}
