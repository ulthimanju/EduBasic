import React from 'react';
import { useTheme } from '../../context/useTheme';

export function SelectField({ label, value, onChange, options, className = '' }) {
  const { bgInput, textMain, accentSolid } = useTheme();
  
  return (
    <label className={`flex flex-col flex-1 gap-1.5 ${className}`}>
      <span className="text-xs font-semibold ml-1 opacity-60">{label}</span>
      <select
        className="px-4 py-3 rounded-xl outline-none focus:ring-2 focus:ring-opacity-50 transition-all font-medium text-sm cursor-pointer appearance-none bg-no-repeat bg-right pr-10"
        value={value}
        onChange={onChange}
        style={{
          backgroundColor: bgInput,
          color: textMain,
          '--tw-ring-color': accentSolid,
          backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='${encodeURIComponent(textMain)}'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M19 9l-7 7-7-7'/%3E%3C/svg%3E")`,
          backgroundSize: '20px',
          backgroundPosition: 'right 0.75rem center',
        }}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </label>
  );
}

export default SelectField;
