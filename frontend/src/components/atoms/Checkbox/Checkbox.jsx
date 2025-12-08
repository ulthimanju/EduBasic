import React from 'react';
import { useTheme } from '../../context/useTheme';

export function Checkbox({ label, checked, onChange }) {
  const colors = useTheme();
  
  return (
    <div className="flex items-center gap-3 cursor-pointer select-none group" onClick={onChange}>
      <div
        className="w-5 h-5 rounded-[6px] flex items-center justify-center transition-all duration-200"
        style={{
          background: checked ? colors.accent : 'transparent',
          border: checked ? 'none' : '2px solid rgba(120,120,128,0.3)',
        }}
      >
        {checked && <span className="text-white text-xs font-bold">✓</span>}
      </div>
      <span className="text-sm font-medium" style={{ color: colors.textMain }}>{label}</span>
    </div>
  );
}

export default Checkbox;
