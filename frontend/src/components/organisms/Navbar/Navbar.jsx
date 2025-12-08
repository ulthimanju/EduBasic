import React, { useState, useMemo } from 'react';
import { useTheme } from '../../context/useTheme';
import { ActionButton } from '../../atoms/Button';
import logoSvg from '../../../assets/logo.svg';
import { svgToDataUrlBase64, generateAvatarSVG } from '../../../utils/avatarGenerator';

function Navbar({ title, onLogout, onNavigate, isLoggingOut, user, onThemeChange, currentTheme = 'Midnight Sunset' }) {
  const colors = useTheme();
  const [showSidebar, setShowSidebar] = useState(false);
  const isDark = currentTheme === 'Midnight Sunset';

  // Generate avatar SVG data URL
  const avatarUrl = useMemo(() => {
    if (!user?.username || !user?.email) return null;
    const avatarSvg = generateAvatarSVG({
      username: user.username,
      email: user.email,
      size: 64,
      type: 'organic',
      isRounded: true
    });
    return svgToDataUrlBase64(avatarSvg);
  }, [user?.username, user?.email]);

  const handleNavigate = (page) => {
    onNavigate(page);
    setShowSidebar(false);
  };

  return (
    <>
      <div
        className="w-full rounded-none p-3 backdrop-blur-xl border-b shadow-lg relative z-40"
        style={{
          background: colors.bgApp,
          borderColor: colors.border,
          boxShadow: colors.shadow,
          color: colors.textMain,
        }}
      >
        <div className="flex items-center justify-between px-4">
          <div className="flex items-center gap-3">
            <img src={logoSvg} alt="edubas" className="w-8 h-8" />
            <span className="text-lg font-bold" style={{ color: colors.textMain }}>{title}</span>
          </div>
          
          <div className="flex items-center gap-3">
            {/* Profile Avatar Button */}
            <button
            onClick={() => setShowSidebar(!showSidebar)}
            className="w-10 h-10 rounded-full flex items-center justify-center transition-all duration-200 hover:scale-110 focus:outline-none focus:ring-2 focus:ring-offset-0 overflow-hidden"
            style={{
              background: colors.isDark ? colors.accent : colors.bgCard,
              border: `1px solid ${colors.border}`,
              color: colors.isDark ? '#fff' : colors.textMain,
            }}
            aria-expanded={showSidebar}
            aria-label="Toggle navigation"
          >
            {avatarUrl ? (
              <img src={avatarUrl} alt={user?.username} className="w-full h-full object-cover" />
            ) : (
              <svg
                className="w-6 h-6"
                fill="currentColor"
                viewBox="0 0 24 24"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
              </svg>
            )}
          </button>
          </div>
        </div>
      </div>

      {/* Overlay */}
      {showSidebar && (
        <div
          className="fixed inset-0 bg-black bg-opacity-50 z-40 transition-opacity duration-300"
          onClick={() => setShowSidebar(false)}
          style={{ animation: 'fadeIn 0.3s ease-in-out' }}
        />
      )}

      {/* Right Sidebar */}
      <div
        className={`fixed top-0 right-0 h-full w-80 shadow-2xl z-50 transition-transform duration-300 ease-in-out ${
          showSidebar ? 'translate-x-0' : 'translate-x-full'
        }`}
        style={{
          background: colors.bgApp,
          borderLeft: `1px solid ${colors.border}`,
        }}
      >
        {/* Sidebar Header */}
        <div
          className="flex items-center justify-between p-6 border-b"
          style={{ borderColor: colors.border }}
        >
          <div className="flex items-center gap-3">
            <div
              className="w-12 h-12 rounded-full flex items-center justify-center text-lg font-bold overflow-hidden flex-shrink-0"
              style={{
                background: colors.isDark ? colors.accent : colors.bgCard,
                border: `1px solid ${colors.border}`,
                color: colors.isDark ? '#fff' : colors.textMain,
              }}
            >
              {avatarUrl ? (
                <img src={avatarUrl} alt={user?.username} className="w-full h-full object-cover" />
              ) : (
                user?.username ? user.username.charAt(0).toUpperCase() : 'U'
              )}
            </div>
            <div>
              <p className="font-semibold" style={{ color: colors.textMain }}>{user?.username || 'User'}</p>
              <p className="text-xs opacity-60" style={{ color: colors.textMain }}>{user?.role || 'Guest'}</p>
            </div>
          </div>
        </div>

        {/* Navigation Items */}
        <div className="p-4 space-y-2">
          <button
            onClick={() => handleNavigate('dashboard')}
            className="w-full text-left px-4 py-3 rounded-lg text-sm transition-all duration-150 flex items-center gap-3"
            style={{
              background: colors.bgCard,
              color: colors.textMain,
            }}
            onMouseEnter={(e) => e.currentTarget.style.opacity = '0.8'}
            onMouseLeave={(e) => e.currentTarget.style.opacity = '1'}
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
            </svg>
            <span className="font-medium">Dashboard</span>
          </button>
          
          <button
            onClick={() => handleNavigate('upload')}
            className="w-full text-left px-4 py-3 rounded-lg text-sm transition-all duration-150 flex items-center gap-3"
            style={{
              background: colors.bgCard,
              color: colors.textMain,
            }}
            onMouseEnter={(e) => e.currentTarget.style.opacity = '0.8'}
            onMouseLeave={(e) => e.currentTarget.style.opacity = '1'}
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 4v16m8-8H4" />
            </svg>
            <span className="font-medium">Upload Content</span>
          </button>
          
          <button
            onClick={() => handleNavigate('view')}
            className="w-full text-left px-4 py-3 rounded-lg text-sm transition-all duration-150 flex items-center gap-3"
            style={{
              background: colors.bgCard,
              color: colors.textMain,
            }}
            onMouseEnter={(e) => e.currentTarget.style.opacity = '0.8'}
            onMouseLeave={(e) => e.currentTarget.style.opacity = '1'}
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
            </svg>
            <span className="font-medium">Courses</span>
          </button>

          <button
            onClick={() => handleNavigate('feed')}
            className="w-full text-left px-4 py-3 rounded-lg text-sm transition-all duration-150 flex items-center gap-3"
            style={{
              background: colors.bgCard,
              color: colors.textMain,
            }}
            onMouseEnter={(e) => e.currentTarget.style.opacity = '0.8'}
            onMouseLeave={(e) => e.currentTarget.style.opacity = '1'}
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
            </svg>
            <span className="font-medium">Feed</span>
          </button>

          {user?.role === 'ADMIN' && (
            <button
              onClick={() => handleNavigate('admin')}
              className="w-full text-left px-4 py-3 rounded-lg text-sm transition-all duration-150 flex items-center gap-3"
              style={{
                background: colors.bgCard,
                color: colors.textMain,
              }}
              onMouseEnter={(e) => e.currentTarget.style.opacity = '0.8'}
              onMouseLeave={(e) => e.currentTarget.style.opacity = '1'}
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
              </svg>
              <span className="font-medium">Admin Panel</span>
            </button>
          )}

          {/* Divider */}
          <div className="py-2">
            <div style={{ borderTop: `1px solid ${colors.border}` }}></div>
          </div>

          {/* Theme Toggle */}
          {onThemeChange && (
            <button
              onClick={() => onThemeChange(isDark ? 'Light Mode' : 'Midnight Sunset')}
              className="w-full text-left px-4 py-3 rounded-lg text-sm transition-all duration-150 flex items-center gap-3"
              style={{
                background: colors.bgCard,
                color: colors.textMain,
              }}
              onMouseEnter={(e) => e.currentTarget.style.opacity = '0.8'}
              onMouseLeave={(e) => e.currentTarget.style.opacity = '1'}
            >
              {isDark ? (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
                </svg>
              ) : (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
                </svg>
              )}
              <span className="font-medium">{isDark ? 'Light Mode' : 'Dark Mode'}</span>
            </button>
          )}

          {/* Logout Button */}
          <button
            onClick={() => {
              setShowSidebar(false);
              onLogout();
            }}
            disabled={isLoggingOut}
            className="w-full text-left px-4 py-3 rounded-lg text-sm transition-all duration-150 flex items-center gap-3"
            style={{
              background: colors.bgCard,
              color: isLoggingOut ? colors.textMain + '66' : colors.textMain,
              opacity: isLoggingOut ? 0.6 : 1,
              cursor: isLoggingOut ? 'not-allowed' : 'pointer',
            }}
            onMouseEnter={(e) => !isLoggingOut && (e.currentTarget.style.opacity = '0.8')}
            onMouseLeave={(e) => e.currentTarget.style.opacity = isLoggingOut ? '0.6' : '1'}
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
            <span className="font-medium">{isLoggingOut ? "Logging out..." : "Logout"}</span>
          </button>
        </div>
      </div>

      <style>{`
        @keyframes fadeIn {
          from {
            opacity: 0;
          }
          to {
            opacity: 1;
          }
        }
      `}</style>
    </>
  );
}

export default Navbar;
