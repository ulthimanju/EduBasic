import React from 'react';
import { useTheme } from '../../context/useTheme';

export function ThemeOption({ label, active, onClick, isDarkTheme }) {
  const { border, accentSolid } = useTheme();
  
  return (
    <button
      onClick={onClick}
      className={`w-full flex items-center gap-3 p-3 rounded-xl border transition-all duration-200 active:scale-[0.98] ${
        active ? 'ring-2 ring-offset-2 ring-offset-transparent' : 'opacity-60 hover:opacity-100'
      }`}
      style={{
        background: isDarkTheme ? '#000' : '#fff',
        borderColor: border,
        color: isDarkTheme ? '#fff' : '#000',
        '--tw-ring-color': accentSolid,
      }}
    >
      <div
        className="w-4 h-4 rounded-full"
        style={{ background: 'linear-gradient(135deg, #FF9F0A 0%, #FF375F 100%)' }}
      />
      <span className="text-xs font-medium">{label}</span>
    </button>
  );
}

export default ThemeOption;
