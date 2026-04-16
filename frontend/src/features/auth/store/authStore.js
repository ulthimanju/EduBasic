import { create } from 'zustand';

/**
 * Zustand store for authentication state.
 *
 * Shape:
 *   user            — { id, email, name } | null
 *   isAuthenticated — derived boolean
 *   authStatus      — 'idle' | 'loading' | 'authenticated' | 'anonymous' | 'error'
 *   authError       — Error | null
 */
const useAuthStore = create((set) => ({
  user: null,
  isAuthenticated: false,
  authStatus: 'idle',
  authError: null,

  startBootstrap: () => set({ authStatus: 'loading', authError: null }),
  resolveAuthenticated: (user) => set({ user, isAuthenticated: true, authStatus: 'authenticated', authError: null }),
  resolveAnonymous: () => set({ user: null, isAuthenticated: false, authStatus: 'anonymous', authError: null }),
  resolveError: (error) => set({ authStatus: 'error', authError: error }),

  /** Called after successful getCurrentUser() — populates user and marks authenticated. */
  setUser: (user) => set({ user, isAuthenticated: true, authStatus: 'authenticated', authError: null }),

  /** Called on logout or 401 — wipes all auth state. */
  clear: () => set({ user: null, isAuthenticated: false, authStatus: 'idle', authError: null }),
}));

export default useAuthStore;
