import { useState } from 'react';

export function useThemeState() {
  const [selectedTheme, setSelectedTheme] = useState(() => {
    return localStorage.getItem('theme') || 'Midnight Sunset';
  });

  const handleThemeChange = (newTheme) => {
    setSelectedTheme(newTheme);
    localStorage.setItem('theme', newTheme);
  };

  return [selectedTheme, handleThemeChange];
}
