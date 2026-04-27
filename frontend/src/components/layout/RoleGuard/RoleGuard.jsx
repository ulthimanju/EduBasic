import { Navigate, Outlet } from 'react-router-dom';
import useAuthStore from '../../../stores/authStore';

export default function RoleGuard({ role }) {
  const hasRole = useAuthStore((s) => s.hasRole);
  
  // If no role is specified, just check if authenticated
  const isAuthenticated = useAuthStore((s) => s.accessToken !== null);
  
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (role && !hasRole(role) && !hasRole('ADMIN')) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}
