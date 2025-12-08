import React, { useState } from 'react';
import { ThemeProvider, Navbar } from '../components';
import { useThemeState } from '../hooks/useThemeState';
import apiFetch, { apiEndpoints } from '../utils/apiClient';

function AppLayout({ children, user, onLogout, onNavigate, title = 'EduBas' }) {
  const [selectedTheme, handleThemeChange] = useThemeState();
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  const handleLogoutWrapper = async () => {
    setIsLoggingOut(true);
    try {
      const response = await apiFetch(apiEndpoints.auth.logout, {
        method: 'POST',
        body: JSON.stringify({
          userId: user?.userId,
        }),
      });

      if (response.ok) {
        console.log('Logout successful');
        onLogout();
      } else {
        const errorData = await response.json().catch(() => ({ message: 'Logout failed' }));
        console.error('Logout failed:', response.status, errorData);
        alert(`Logout failed: ${errorData?.message || 'Please try again.'}`);
      }
    } catch (error) {
      console.error('Error during logout:', error);
      alert(`Error during logout: ${error?.message || 'An unexpected error occurred.'}`);
    } finally {
      setIsLoggingOut(false);
    }
  };

  return (
    <ThemeProvider selectedTheme={selectedTheme}>
      <div className="flex flex-col h-screen">
        <Navbar
          title={title}
          onLogout={handleLogoutWrapper}
          onNavigate={onNavigate}
          isLoggingOut={isLoggingOut}
          user={user}
          onThemeChange={handleThemeChange}
          currentTheme={selectedTheme}
        />
        {children}
      </div>
    </ThemeProvider>
  );
}

export default AppLayout;
