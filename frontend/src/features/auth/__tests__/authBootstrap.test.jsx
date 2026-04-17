import React, { StrictMode } from 'react';
import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom';
import ProtectedRoute from '../components/ProtectedRoute';
import PublicOnlyRoute from '../components/PublicOnlyRoute';
import useAuthStore from '../store/authStore';
import useCurrentUser from '../hooks/useCurrentUser';
import * as authService from '../services/authService';

vi.mock('../services/authService', () => ({
  getCurrentUser: vi.fn(),
}));

const LocationDisplay = () => {
  const location = useLocation();
  return <div data-testid="location-display">{location.pathname}</div>;
};

const TestApp = () => {
  const resolveAnonymous = useAuthStore((s) => s.resolveAnonymous);
  
  React.useEffect(() => {
    const handler = () => resolveAnonymous();
    window.addEventListener('auth:unauthorized', handler);
    return () => window.removeEventListener('auth:unauthorized', handler);
  }, [resolveAnonymous]);

  useCurrentUser();
  return (
    <>
      <LocationDisplay />
      <Routes>
        <Route element={<PublicOnlyRoute />}>
          <Route path="/login" element={<div data-testid="login-page">Login Page</div>} />
        </Route>
        <Route element={<ProtectedRoute />}>
          <Route path="/dashboard" element={<div data-testid="dashboard-page">Dashboard content</div>} />
        </Route>
      </Routes>
    </>
  );
};

const renderApp = (initialEntries = ['/dashboard']) => {
  return render(
    <MemoryRouter initialEntries={initialEntries}>
      <TestApp />
    </MemoryRouter>
  );
};

describe('Auth Bootstrap Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.getState().clear();
  });

  describe('Protected Route Guard', () => {
    it('shows skeleton while loading and renders content when authenticated', async () => {
      let resolvePromise;
      const promise = new Promise((resolve) => { resolvePromise = resolve; });
      authService.getCurrentUser.mockReturnValue(promise);

      renderApp(['/dashboard']);
      
      // Should show skeleton initially
      expect(screen.getByTestId('location-display').textContent).toBe('/dashboard');
      expect(document.querySelector('.protected-loading')).toBeInTheDocument();
      
      // Resolve auth
      await act(async () => {
        resolvePromise({ id: 'user-1', name: 'Test User' });
      });

      // Content renders
      expect(screen.queryByTestId('location-display').textContent).toBe('/dashboard');
      expect(screen.getByTestId('dashboard-page')).toBeInTheDocument();
      expect(document.querySelector('.protected-loading')).not.toBeInTheDocument();
    });

    it('redirects to /login when anonymous (401)', async () => {
      authService.getCurrentUser.mockRejectedValue({ response: { status: 401 } });

      renderApp(['/dashboard']);

      await waitFor(() => {
        expect(screen.getByTestId('location-display').textContent).toBe('/login');
      });
    });

    it('redirects to /login when auth resolves without a user payload', async () => {
      authService.getCurrentUser.mockResolvedValue(null);

      renderApp(['/dashboard']);

      await waitFor(() => {
        expect(screen.getByTestId('location-display').textContent).toBe('/login');
      });
    });

    it('renders inline error + retry button on non-401 error', async () => {
      authService.getCurrentUser.mockRejectedValue(new Error('Network error'));

      renderApp(['/dashboard']);

      // Should not redirect
      await waitFor(() => {
        expect(screen.getByTestId('location-display').textContent).toBe('/dashboard');
      });

      // Error message should be in document
      expect(screen.getByText('Failed to load your session details.')).toBeInTheDocument();
      const retryButton = screen.getByRole('button', { name: /retry/i });
      expect(retryButton).toBeInTheDocument();

      // Click retry
      authService.getCurrentUser.mockResolvedValue({ id: 'user-1', name: 'Test User' });
      await act(async () => {
        userEvent.click(retryButton);
      });

      // Should recover
      await waitFor(() => {
        expect(screen.getByTestId('dashboard-page')).toBeInTheDocument();
      });
    });
  });

  describe('Login Route Guard (PublicOnlyRoute)', () => {
    it('redirects authenticated users to /dashboard', async () => {
      authService.getCurrentUser.mockResolvedValue({ id: 'user-1' });

      renderApp(['/login']);

      await waitFor(() => {
        expect(screen.getByTestId('location-display').textContent).toBe('/dashboard');
      });
    });

    it('renders login page when anonymous', async () => {
      authService.getCurrentUser.mockRejectedValue({ response: { status: 401 } });

      renderApp(['/login']);

      await waitFor(() => {
        expect(screen.getByTestId('location-display').textContent).toBe('/login');
        expect(screen.getByTestId('login-page')).toBeInTheDocument();
      });
    });
    
    it('renders login page WITH inline error when non-401 error occurs', async () => {
      authService.getCurrentUser.mockRejectedValue(new Error('Network error'));

      renderApp(['/login']);

      await waitFor(() => {
        expect(screen.getByTestId('location-display').textContent).toBe('/login');
      });

      expect(screen.getByTestId('login-page')).toBeInTheDocument();
      expect(screen.getByText('Failed to check session status.')).toBeInTheDocument();
    });
  });

  describe('Bootstrap Deduping Logic', () => {
    it('deduplicates concurrent requests during strict mode double mount', async () => {
      authService.getCurrentUser.mockResolvedValue({ id: 'user-1' });

      const Comp = () => {
        useCurrentUser();
        return null;
      };

      // strict mode renders components twice
      render(
        <StrictMode>
          <Comp />
        </StrictMode>
      );

      // give promise time to resolve
      await waitFor(() => {
        expect(useAuthStore.getState().authStatus).toBe('authenticated');
      });

      // should only be called once despite strict mode
      expect(authService.getCurrentUser).toHaveBeenCalledTimes(1);
    });
  });

  describe('Store Global Event Integration', () => {
    it('auth:unauthorized sets authStatus to anonymous', () => {
      renderApp(['/dashboard']);
      useAuthStore.getState().resolveAuthenticated({ id: 'user-1' });
      expect(useAuthStore.getState().authStatus).toBe('authenticated');

      // Emulate apiClient dispatching event
      window.dispatchEvent(new Event('auth:unauthorized'));

      expect(useAuthStore.getState().authStatus).toBe('anonymous');
      expect(useAuthStore.getState().isAuthenticated).toBe(false);
    });
  });
});
