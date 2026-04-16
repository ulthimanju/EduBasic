import { useState, useEffect } from 'react';

/**
 * Hook to manage theme consistency avoiding the need for redundant state inside App.js.
 * Provides system integration and sets local storage and document metadata automatically.
 */
export default function useThemeMode() {
  const [themeMode, setThemeMode] = useState(() => {
    if (typeof window === 'undefined') {
      return 'system';
    }
    return window.localStorage.getItem('ui-theme-mode') ?? 'system';
  });

  const [systemTheme, setSystemTheme] = useState(() => {
    if (typeof window === 'undefined') {
      return 'dark';
    }
    return window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
  });

  useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: light)');

    const handleChange = (event) => {
      setSystemTheme(event.matches ? 'light' : 'dark');
    };

    handleChange(mediaQuery);

    if (mediaQuery.addEventListener) {
      mediaQuery.addEventListener('change', handleChange);
      return () => mediaQuery.removeEventListener('change', handleChange);
    } else {
      mediaQuery.addListener(handleChange);
      return () => mediaQuery.removeListener(handleChange);
    }
  }, []);

  const effectiveTheme = themeMode === 'system' ? systemTheme : themeMode;

  useEffect(() => {
    const root = document.documentElement;
    root.dataset.theme = effectiveTheme;
    // Align DOM color scheme property synchronously to prevent background styling flash
    root.style.colorScheme = effectiveTheme;
    
    window.localStorage.setItem('ui-theme-mode', themeMode);
  }, [effectiveTheme, themeMode]);

  return { themeMode, setThemeMode, effectiveTheme };
}
