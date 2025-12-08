import React, { useRef, useEffect } from 'react';
import { useTheme } from '../../context/useTheme';

export function PasswordField({ label, value, visible, onChange, onToggle, className = '' }) {
  const { bgInput, textMain, accentSolid } = useTheme();
  const inputRef = useRef(null);

  useEffect(() => {
    const input = inputRef.current;
    if (!input) return;

    const style = document.createElement('style');
    style.innerHTML = `
      input[data-theme-password]:-webkit-autofill {
        -webkit-box-shadow: 0 0 0 1000px ${bgInput} inset !important;
        -webkit-text-fill-color: ${textMain} !important;
      }
      input[data-theme-password]:-moz-autofill {
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
      <div className="relative">
        <input
          ref={inputRef}
          data-theme-password="true"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          type={visible ? 'text' : 'password'}
          className="w-full px-4 py-3 rounded-xl outline-none focus:ring-2 focus:ring-opacity-50 transition-all font-medium text-sm"
          placeholder="••••••"
          style={{
            backgroundColor: bgInput,
            color: textMain,
            '--tw-ring-color': accentSolid,
          }}
        />
        <button
          type="button"
          onClick={(e) => {
            e.preventDefault();
            onToggle();
          }}
          className="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-medium text-blue-500 hover:opacity-70 transition-opacity"
        >
          {visible ? 'Hide' : 'Show'}
        </button>
      </div>
    </label>
  );
}

export default PasswordField;
