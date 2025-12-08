import React from 'react';
import { useTheme } from '../../context/useTheme';

export function Slider({ value, onChange }) {
  const { bgInput, accentSolid } = useTheme();
  
  const handleClick = (e) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const percent = Math.max(0, Math.min(100, ((e.clientX - rect.left) / rect.width) * 100));
    onChange(Math.round(percent));
  };

  return (
    <div
      className="relative h-6 flex items-center group cursor-pointer"
      onClick={handleClick}
    >
      <div
        className="absolute w-full h-1.5 rounded-full overflow-hidden"
        style={{ background: bgInput }}
      >
        <div
          className="h-full rounded-full transition-all duration-100"
          style={{ width: `${value}%`, background: accentSolid }}
        />
      </div>
      <div
        className="absolute w-5 h-5 bg-white rounded-full shadow-md border transition-all duration-100"
        style={{ left: `calc(${value}% - 10px)`, borderColor: 'rgba(0,0,0,0.1)' }}
      />
    </div>
  );
}

export default Slider;
