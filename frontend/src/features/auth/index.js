/**
 * Public exports for the auth feature.
 * Import from here — not from internal paths.
 */
export { default as useInitAuth }   from './hooks/useInitAuth';
export { default as useLogout }     from './hooks/useLogout';
export { default as useAuthStore }  from './store/authStore';
export { getCurrentUser, logout }   from './services/authService';
export { default as PublicOnlyRoute } from './components/PublicOnlyRoute';
