import { create } from 'zustand';

const useAuthStore = create((set, get) => ({
  accessToken: null,
  userId: null,
  email: null,
  roles: [],

  setAuth: ({ accessToken, userId, email, roles }) =>
    set({ accessToken, userId, email, roles }),

  setAccessToken: (accessToken) => set({ accessToken }),

  clearAuth: () =>
    set({ accessToken: null, userId: null, email: null, roles: [] }),

  getAccessToken: () => get().accessToken,

  hasRole: (role) => get().roles.includes(role),

  isInstructor: () =>
    get().roles.includes('INSTRUCTOR') || get().roles.includes('ADMIN'),
}));

export default useAuthStore;
