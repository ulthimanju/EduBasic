import React from 'react';
import { useTheme } from '../../context/useTheme';

export function SideNavItem({ label, active, onClick, subdued = false }) {
  const { textMain, textMuted } = useTheme();
  
  return (
    <button
      onClick={onClick}
      className={`w-full text-left px-4 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 active:scale-[0.98] ${
        subdued ? 'opacity-50' : ''
      }`}
      style={{
        background: active
          ? textMain === '#FFFFFF'
            ? 'rgba(255,255,255,0.1)'
            : 'rgba(0,0,0,0.05)'
          : 'transparent',
        color: active ? textMain : textMuted,
      }}
    >
      {label}
    </button>
  );
}

export default SideNavItem;
