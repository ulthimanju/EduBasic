import React from 'react';
import { useTheme } from '../../context/useTheme';

export function Loader({ message = 'Loading...' }) {
  const { bgPanel, textMuted } = useTheme();
  
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/20 backdrop-blur-md transition-all">
      <div
        className="p-8 rounded-[24px] shadow-2xl flex flex-col items-center gap-4 animate-in zoom-in-95"
        style={{ background: bgPanel }}
      >
        <div
          className="w-10 h-10 rounded-full border-4 border-t-transparent animate-spin"
          style={{ borderColor: textMuted, borderTopColor: 'transparent' }}
        />
        <div className="text-sm font-medium opacity-70">{message}</div>
      </div>
    </div>
  );
}

export default Loader;
