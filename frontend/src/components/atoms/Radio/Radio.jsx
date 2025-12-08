import React from 'react';

export function Radio({ label, checked, onChange, name }) {
  return (
    <div className="flex items-center gap-3 cursor-pointer select-none" onClick={onChange}>
      <div
        className="w-5 h-5 rounded-full flex items-center justify-center transition-all"
        style={{ border: `2px solid ${checked ? '#007AFF' : 'rgba(120,120,128,0.3)'}` }}
      >
        {checked && <div className="w-2.5 h-2.5 rounded-full bg-[#007AFF]" />}
      </div>
      <span className="text-sm font-medium">{label}</span>
    </div>
  );
}

export default Radio;
