// Theme color constants and utilities
// Separated from ThemeContext to support Fast Refresh

export const getThemeColors = (selectedTheme) => {
  const isDark = selectedTheme === 'Midnight Sunset';
  
  return {
    isDark,
    bgApp: isDark ? '#000000' : '#F5F5F7',
    bgPanel: isDark ? 'rgba(28, 28, 30, 0.75)' : 'rgba(255, 255, 255, 0.75)',
    bgCard: isDark ? 'rgba(44, 44, 46, 0.6)' : 'rgba(255, 255, 255, 0.6)',
    bgInput: isDark ? 'rgba(118, 118, 128, 0.24)' : 'rgba(118, 118, 128, 0.12)',
    textMain: isDark ? '#FFFFFF' : '#000000',
    textMuted: isDark ? '#EBEBF5' : '#1D1D1F',
    textMutedDim: isDark ? 'rgba(235, 235, 245, 0.6)' : 'rgba(60, 60, 67, 0.6)',
    border: isDark ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.05)',
    shadow: isDark ? 'none' : '0 4px 24px rgba(0,0,0,0.04)',
    accent: 'linear-gradient(135deg, #FF9F0A 0%, #FF375F 100%)',
    accentSolid: '#FF375F',
    accentDim: isDark ? 'rgba(255, 55, 95, 0.25)' : 'rgba(255, 55, 95, 0.15)',
    success: isDark ? '#30D158' : '#34C759',
    successBg: isDark ? 'rgba(48, 209, 88, 0.1)' : 'rgba(52, 199, 89, 0.1)',
    warning: isDark ? '#FFD60A' : '#FFCC00',
    warningBg: isDark ? 'rgba(255, 214, 10, 0.1)' : 'rgba(255, 204, 0, 0.1)',
    error: isDark ? '#FF453A' : '#FF3B30',
    errorBg: isDark ? 'rgba(255, 69, 58, 0.1)' : 'rgba(255, 59, 48, 0.1)',
  };
};
