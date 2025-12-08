import React from 'react';
import { useTheme } from '../../context/useTheme';

export function OutlineButton({ label, onClick, className = '' }) {
  const { textMain, border } = useTheme();
  
  return (
    <button
      className={`px-6 py-3 rounded-xl text-sm font-semibold active:scale-[0.97] transition-all hover:bg-black/5 dark:hover:bg-white/10 ${className}`}
      style={{ color: textMain, border: `1px solid ${border}` }}
      onClick={onClick}
    >
      {label}
    </button>
  );
}

export default OutlineButton;
