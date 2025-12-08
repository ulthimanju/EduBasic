import React from 'react';
import { useTheme } from '../../context/useTheme';

export function Toggle({ label, active, onToggle }) {
  const { accent, border } = useTheme();

  return (
    <div className="flex items-center gap-3 cursor-pointer select-none group" onClick={onToggle}>
      <div
        className="w-[51px] h-[31px] rounded-full p-0.5 transition-colors duration-300 ease-in-out"
        style={{ background: active ? accent : border }}
      >
        <div
          className={`w-[27px] h-[27px] rounded-full bg-white shadow-sm transition-transform duration-300 ease-[cubic-bezier(0.175,0.885,0.32,1.275)] ${
            active ? 'translate-x-[22px]' : 'translate-x-0'
          }`}
        />
      </div>
      {label && <span className="text-sm font-medium">{label}</span>}
    </div>
  );
}

export default Toggle;
