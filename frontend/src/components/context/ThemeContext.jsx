import React, { createContext, useMemo } from 'react';
import { getThemeColors } from './themeConstants';

const ThemeContext = createContext(null);

export function ThemeProvider({ children, selectedTheme = 'Midnight Sunset' }) {
  const colors = useMemo(() => {
    return getThemeColors(selectedTheme);
  }, [selectedTheme]);

  return (
    <ThemeContext.Provider value={colors}>
      {children}
    </ThemeContext.Provider>
  );
}

export default ThemeContext;
