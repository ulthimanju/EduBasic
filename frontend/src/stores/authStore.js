import { create } from 'zustand';

const getResolvedAuthState = (user, accessToken) => (
  user
    ? { user, accessToken, isAuthenticated: true, authStatus: 'authenticated', authError: null }
    : { user: null, accessToken: null, isAuthenticated: false, authStatus: 'anonymous', authError: null }
);

/**
 * Zustand store for authentication state.
 */
const useAuthStore = create((set) => ({
  user: null,
  accessToken: null,
  isAuthenticated: false,
  authStatus: 'idle',
  authError: null,

  startBootstrap: () => set({ authStatus: 'loading', authError: null }),
  
  setAuth: (user, accessToken) => set(getResolvedAuthState(user, accessToken)),
  
  resolveAuthenticated: (user) => set(getResolvedAuthState(user, null)),
  resolveAnonymous: () => set({ user: null, accessToken: null, isAuthenticated: false, authStatus: 'anonymous', authError: null }),
  resolveError: (error) => set({ authStatus: 'error', authError: error }),

  setUser: (user) => set((state) => getResolvedAuthState(user, state.accessToken)),
  setAccessToken: (token) => set((state) => getResolvedAuthState(state.user, token)),

  clear: () => set({ user: null, accessToken: null, isAuthenticated: false, authStatus: 'idle', authError: null }),
}));

export default useAuthStore;
