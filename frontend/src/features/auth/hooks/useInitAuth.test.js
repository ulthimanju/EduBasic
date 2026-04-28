import { renderHook, waitFor } from '@testing-library/react';
import useInitAuth from './useInitAuth';
import useAuthStore from '../store/authStore';
import axios from 'axios';
import { expect, test, vi, beforeEach } from 'vitest';

vi.mock('axios');
vi.mock('../store/authStore');

beforeEach(() => {
  vi.clearAllMocks();
});

test('successfully bootstraps auth state', async () => {
  const setAuth = vi.fn();
  useAuthStore.mockImplementation((selector) => {
    const state = { setAuth, clearAuth: vi.fn() };
    return selector(state);
  });

  axios.post.mockResolvedValueOnce({ data: { accessToken: 'new-token' } });
  axios.get.mockResolvedValueOnce({ 
    data: { data: { id: '1', email: 'test@test.com', roles: ['STUDENT'] } } 
  });

  const { result } = renderHook(() => useInitAuth());

  await waitFor(() => expect(result.current.isInitializing).toBe(false));
  expect(result.current.authStatus).toBe('authenticated');
  expect(setAuth).toHaveBeenCalledWith(expect.objectContaining({
    accessToken: 'new-token',
    userId: '1'
  }));
});

test('clears auth on 401 refresh error', async () => {
  const clearAuth = vi.fn();
  useAuthStore.mockImplementation((selector) => {
    const state = { setAuth: vi.fn(), clearAuth };
    return selector(state);
  });

  axios.post.mockRejectedValueOnce({ response: { status: 401 } });

  const { result } = renderHook(() => useInitAuth());

  await waitFor(() => expect(result.current.isInitializing).toBe(false));
  expect(result.current.authStatus).toBe('anonymous');
  expect(clearAuth).toHaveBeenCalled();
});

test('sets error status on non-401 error', async () => {
  useAuthStore.mockImplementation((selector) => {
    const state = { setAuth: vi.fn(), clearAuth: vi.fn() };
    return selector(state);
  });

  axios.post.mockRejectedValueOnce(new Error('Network Error'));

  const { result } = renderHook(() => useInitAuth());

  await waitFor(() => expect(result.current.isInitializing).toBe(false));
  expect(result.current.authStatus).toBe('error');
});
