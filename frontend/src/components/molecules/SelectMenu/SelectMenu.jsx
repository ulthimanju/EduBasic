import React, { useState, useEffect, useRef } from 'react';
import { useTheme } from '../../context/useTheme';

export function SelectMenu({ options, selected, onChange }) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef(null);
  const { bgInput, textMain, bgPanel, border } = useTheme();

  useEffect(() => {
    function handleClickOutside(event) {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <div className={`relative w-full ${isOpen ? 'z-50' : 'z-0'}`} ref={containerRef}>
      <div
        onClick={() => setIsOpen(!isOpen)}
        className="px-4 py-3 rounded-xl flex justify-between items-center cursor-pointer transition-all select-none w-full active:scale-[0.99] active:opacity-80"
        style={{ background: bgInput, color: textMain }}
      >
        <span className="text-sm font-medium">{selected}</span>
        <span
          className="text-xs opacity-40 transition-transform duration-200"
          style={{ transform: isOpen ? 'rotate(180deg)' : 'rotate(0deg)' }}
        >
          ▼
        </span>
      </div>

      {isOpen && (
        <div
          className="absolute top-full left-0 right-0 mt-2 p-1.5 rounded-xl shadow-2xl z-50 flex flex-col gap-0.5 backdrop-blur-xl border origin-top animate-in zoom-in-95 duration-100"
          style={{ background: bgPanel, borderColor: border }}
        >
          {options.map((opt) => (
            <div
              key={opt}
              onClick={() => {
                onChange(opt);
                setIsOpen(false);
              }}
              className="px-3 py-2 rounded-lg text-sm cursor-pointer transition-colors"
              style={{
                background: selected === opt ? '#007AFF' : 'transparent',
                color: selected === opt ? '#fff' : textMain,
                fontWeight: selected === opt ? 600 : 400,
              }}
            >
              {opt}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default SelectMenu;
