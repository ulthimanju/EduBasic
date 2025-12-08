import React from 'react';
import { useTheme } from '../../context/useTheme';

export function Metric({ label, value }) {
  const { bgCard } = useTheme();
  
  return (
    <div
      className="p-4 rounded-2xl transition-all duration-300 backdrop-blur-md"
      style={{ background: bgCard }}
    >
      <div className="text-[11px] font-bold uppercase tracking-wider opacity-50 mb-1">
        {label}
      </div>
      <div className="text-2xl font-bold tracking-tight">{value}</div>
    </div>
  );
}

export default Metric;
