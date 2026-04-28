import { createAuthenticatedClient } from './createClient';
import useAuthStore from '../features/auth/store/authStore';
import { expect, test, vi, beforeEach } from 'vitest';
import axios from 'axios';

vi.mock('axios', async () => {
  const actual = await vi.importActual('axios');
  return {
    default: {
      ...actual.default,
      create: vi.fn(() => {
        const instance = vi.fn();
        instance.interceptors = {
          request: { use: vi.fn() },
          response: { use: vi.fn() }
        };
        return instance;
      }),
      post: vi.fn()
    }
  };
});

vi.mock('../features/auth/store/authStore');

beforeEach(() => {
  vi.clearAllMocks();
});

test('injects Bearer token into requests', () => {
  const getAccessToken = vi.fn(() => 'test-token');
  useAuthStore.getState = vi.fn(() => ({ getAccessToken }));

  const client = createAuthenticatedClient('http://test.api');
  
  // Get the request interceptor
  const [requestInterceptor] = axios.create.mock.results[0].value.interceptors.request.use.mock.calls[0];
  
  const config = { headers: {} };
  const result = requestInterceptor(config);
  
  expect(result.headers.Authorization).toBe('Bearer test-token');
});
