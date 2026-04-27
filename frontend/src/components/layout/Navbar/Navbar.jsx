import React from 'react';
import { NavLink } from 'react-router-dom';
import { Bot, LayoutDashboard, LogOut, Monitor, MoonStar, SunMedium, UserCircle2, BookOpen } from 'lucide-react';
import useAuthStore from '../../../stores/authStore';
import useLogout from '../../../features/auth/hooks/useLogout';
import { ROUTES } from '../../../constants/appConstants';

/**
 * Left navigation sidebar with account section.
 */
export default function Navbar({ themeMode, effectiveTheme, onThemeModeChange }) {
  const { user, isAuthenticated } = useAuthStore();
  const { logout, isLoading }     = useLogout();

  return (
    <aside className="sidebar" role="navigation" aria-label="Primary">
      <div className="sidebar__brand">
        <Bot size={18} strokeWidth={1.5} />
        <span>Auth Console</span>
      </div>

      <div className="divider" />

      <div className="sidebar__section-title">Workspace</div>
      <nav className="sidebar__nav" aria-label="Navigation links">
        <NavLink
          to={ROUTES.DASHBOARD}
          className={({ isActive }) => `sidebar__item ${isActive ? 'is-active' : ''}`}
        >
          <LayoutDashboard size={18} strokeWidth={1.5} />
          <span>Dashboard</span>
        </NavLink>

        <NavLink
          to={ROUTES.COURSES}
          className={({ isActive }) => `sidebar__item ${isActive ? 'is-active' : ''}`}
        >
          <BookOpen size={18} strokeWidth={1.5} />
          <span>Exams</span>
        </NavLink>
      </nav>

      <div className="sidebar__theme-group" role="group" aria-label="Theme mode">
        <button
          className={`sidebar__theme-item ${themeMode === 'system' ? 'is-active' : ''}`}
          type="button"
          onClick={() => onThemeModeChange('system')}
          aria-pressed={themeMode === 'system'}
        >
          <Monitor size={18} strokeWidth={1.5} />
          <span>System</span>
          <span className="sidebar__theme-note">{effectiveTheme}</span>
        </button>

        <button
          className={`sidebar__theme-item ${themeMode === 'dark' ? 'is-active' : ''}`}
          type="button"
          onClick={() => onThemeModeChange('dark')}
          aria-pressed={themeMode === 'dark'}
        >
          <MoonStar size={18} strokeWidth={1.5} />
          <span>Dark</span>
        </button>

        <button
          className={`sidebar__theme-item ${themeMode === 'light' ? 'is-active' : ''}`}
          type="button"
          onClick={() => onThemeModeChange('light')}
          aria-pressed={themeMode === 'light'}
        >
          <SunMedium size={18} strokeWidth={1.5} />
          <span>Light</span>
        </button>
      </div>

      <div className="sidebar__spacer" />

      {isAuthenticated && user ? (
        <div className="sidebar__account">
          <div className="sidebar__userline">
            <div className="sidebar__avatar" aria-hidden="true">
              {user.name?.[0]?.toUpperCase() ?? '?'}
            </div>
            <div>
              <div className="sidebar__username">{user.name}</div>
              <div className="sidebar__useremail">{user.email}</div>
            </div>
          </div>

          <button
            className="btn btn-secondary"
            onClick={logout}
            disabled={isLoading}
            aria-label="Sign out"
          >
            <LogOut size={16} strokeWidth={1.5} />
            {isLoading ? 'Signing out…' : 'Sign out'}
          </button>
        </div>
      ) : (
        <div className="sidebar__account sidebar__account--empty">
          <UserCircle2 size={18} strokeWidth={1.5} />
          <span>Not signed in</span>
        </div>
      )}
    </aside>
  );
}
