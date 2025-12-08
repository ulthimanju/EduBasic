import React, { useRef, useEffect } from 'react';
import { useTheme } from '../../context/useTheme';

export function InputField({ label, placeholder, value, onChange, className = '' }) {
  const { bgInput, textMain, accentSolid } = useTheme();
  const inputRef = useRef(null);

  useEffect(() => {
    const input = inputRef.current;
    if (!input) return;

    const style = document.createElement('style');
    style.innerHTML = `
      input[data-theme-input]:-webkit-autofill {
        -webkit-box-shadow: 0 0 0 1000px ${bgInput} inset !important;
        -webkit-text-fill-color: ${textMain} !important;
      }
      input[data-theme-input]:-moz-autofill {
        background-color: ${bgInput} !important;
        color: ${textMain} !important;
      }
    `;
    document.head.appendChild(style);
    
    return () => {
      document.head.removeChild(style);
    };
  }, [bgInput, textMain]);
  
  return (
    <label className={`flex flex-col flex-1 gap-1.5 ${className}`}>
      <span className="text-xs font-semibold ml-1 opacity-60">{label}</span>
      <input
        ref={inputRef}
        data-theme-input="true"
        className="px-4 py-3 rounded-xl outline-none focus:ring-2 focus:ring-opacity-50 transition-all font-medium text-sm placeholder-opacity-40"
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        style={{
          backgroundColor: bgInput,
          color: textMain,
          '--tw-ring-color': accentSolid,
        }}
      />
    </label>
  );
}

export default InputField;
