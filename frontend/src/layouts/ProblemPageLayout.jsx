import React from 'react';
import { ThemeProvider, AuthProvider } from '../components';

// Shared layout to wrap pages with core contexts; intentionally no navbar
function ProblemPageLayout({ children, selectedTheme = 'Midnight Sunset', className = '' }) {
  return (
    <ThemeProvider selectedTheme={selectedTheme}>
      <AuthProvider>
        <div className={className}>
          {children}
        </div>
      </AuthProvider>
    </ThemeProvider>
  );
}

export default ProblemPageLayout;
