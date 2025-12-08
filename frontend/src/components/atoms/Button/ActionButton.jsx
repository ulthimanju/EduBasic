import React from 'react';
import { useTheme } from '../../context/useTheme';

export function ActionButton({ label, onClick, className = '' }) {
  const { accent } = useTheme();
  
  return (
    <button
      className={`px-6 py-3 rounded-xl text-sm font-bold text-white shadow-lg active:scale-[0.97] transition-all hover:brightness-110 ${className}`}
      style={{ background: accent }}
      onClick={onClick}
    >
      {label}
    </button>
  );
}

export default ActionButton;
