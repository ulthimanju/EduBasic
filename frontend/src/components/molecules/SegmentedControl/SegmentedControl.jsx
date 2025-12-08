import React from 'react';
import { useTheme } from '../../context/useTheme';

export function SegmentedControl({ options, active, onChange }) {
  const { bgInput, textMain } = useTheme();
  
  return (
    <div className="p-1 rounded-xl flex relative" style={{ background: bgInput }}>
      {options.map((opt) => {
        const isActive = active === opt;
        return (
          <button
            key={opt}
            onClick={() => onChange(opt)}
            className={`flex-1 py-1.5 text-xs font-semibold rounded-lg transition-all duration-200 relative z-10 ${
              isActive ? 'shadow-sm' : 'hover:opacity-60'
            }`}
            style={{
              color: isActive ? '#000' : textMain,
              opacity: isActive ? 1 : 0.5,
            }}
          >
            {opt}
          </button>
        );
      })}
      <div
        className="absolute top-1 bottom-1 bg-white rounded-lg shadow-sm transition-all duration-300 ease-[cubic-bezier(0.175,0.885,0.32,1.275)]"
        style={{
          width: `calc(${100 / options.length}% - 8px)`,
          left: `calc(${options.indexOf(active) * (100 / options.length)}% + 4px)`,
        }}
      />
    </div>
  );
}

export default SegmentedControl;
