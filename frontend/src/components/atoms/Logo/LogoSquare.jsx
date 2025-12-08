import React from 'react';
import { useTheme } from '../../context/useTheme';

export function LogoSquare({ letter = 'W' }) {
  const { accent } = useTheme();
  
  return (
    <div
      className="w-11 h-11 rounded-[12px] flex items-center justify-center text-white font-bold text-lg shadow-lg"
      style={{ background: accent }}
    >
      {letter}
    </div>
  );
}

export default LogoSquare;
