import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import RoleGuard from './RoleGuard';
import useAuthStore from '../../../features/auth/store/authStore';
import { expect, test, vi, beforeEach } from 'vitest';

vi.mock('../../../features/auth/store/authStore');

beforeEach(() => {
  vi.clearAllMocks();
});

test('renders children when authenticated', () => {
  useAuthStore.mockImplementation((selector) => {
    const state = {
      accessToken: 'valid-token',
      hasRole: () => true
    };
    return selector ? selector(state) : state;
  });

  render(
    <MemoryRouter initialEntries={['/protected']}>
      <Routes>
        <Route element={<RoleGuard />}>
          <Route path="/protected" element={<div>Protected Content</div>} />
        </Route>
      </Routes>
    </MemoryRouter>
  );

  expect(screen.getByText('Protected Content')).toBeInTheDocument();
});

test('redirects to login when not authenticated', () => {
  useAuthStore.mockImplementation((selector) => {
    const state = {
      accessToken: null,
      hasRole: () => false
    };
    return selector ? selector(state) : state;
  });

  render(
    <MemoryRouter initialEntries={['/protected']}>
      <Routes>
        <Route element={<RoleGuard />}>
          <Route path="/protected" element={<div>Protected Content</div>} />
        </Route>
        <Route path="/login" element={<div>Login Page</div>} />
      </Routes>
    </MemoryRouter>
  );

  expect(screen.getByText('Login Page')).toBeInTheDocument();
});

test('allows ADMIN even if specific role is required', () => {
  useAuthStore.mockImplementation((selector) => {
    const state = {
      accessToken: 'admin-token',
      hasRole: (r) => r === 'ADMIN'
    };
    return selector ? selector(state) : state;
  });

  render(
    <MemoryRouter initialEntries={['/instructor']}>
      <Routes>
        <Route element={<RoleGuard role="INSTRUCTOR" />}>
          <Route path="/instructor" element={<div>Instructor Panel</div>} />
        </Route>
      </Routes>
    </MemoryRouter>
  );

  expect(screen.getByText('Instructor Panel')).toBeInTheDocument();
});

test('redirects to home when role mismatch', () => {
  useAuthStore.mockImplementation((selector) => {
    const state = {
      accessToken: 'student-token',
      hasRole: (r) => r === 'STUDENT'
    };
    return selector ? selector(state) : state;
  });

  render(
    <MemoryRouter initialEntries={['/instructor']}>
      <Routes>
        <Route element={<RoleGuard role="INSTRUCTOR" />}>
          <Route path="/instructor" element={<div>Instructor Panel</div>} />
        </Route>
        <Route path="/" element={<div>Home Page</div>} />
      </Routes>
    </MemoryRouter>
  );

  expect(screen.getByText('Home Page')).toBeInTheDocument();
});
