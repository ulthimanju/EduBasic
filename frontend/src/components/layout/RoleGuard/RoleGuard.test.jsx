import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import RoleGuard from './RoleGuard';
import useAuthStore from '../../../stores/authStore';
import { expect, test, vi, beforeEach } from 'vitest';

vi.mock('../../../stores/authStore');

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
