import { create } from 'zustand';

/**
 * Zustand store for authentication state.
 *
 * Shape:
 *   user            — { id, email, name } | null
 *   isAuthenticated — derived boolean
 *   setUser(user)   — set user, mark as authenticated
 *   clear()         — reset to unauthenticated state
 */
const useAuthStore = create((set) => ({
  user: null,
  isAuthenticated: false,

  /** Called after successful getCurrentUser() — populates user and marks authenticated. */
  setUser: (user) => set({ user, isAuthenticated: true }),

  /** Called on logout or 401 — wipes all auth state. */
  clear: () => set({ user: null, isAuthenticated: false }),
}));

export default useAuthStore;
