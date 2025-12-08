import React, { useState } from 'react';
import { useTheme } from '../../context/useTheme';

export function Accordion({ title, content, defaultOpen = false }) {
  const [isOpen, setIsOpen] = useState(defaultOpen);
  const { border } = useTheme();

  return (
    <div className="border-b last:border-0" style={{ borderColor: border }}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="w-full flex justify-between items-center py-3 text-left font-medium text-sm hover:opacity-70 transition-opacity"
      >
        {title}
        <span className={`transform transition-transform ${isOpen ? 'rotate-180' : ''}`}>
          ▼
        </span>
      </button>
      {isOpen && (
        <div className="pb-4 text-sm opacity-60 leading-relaxed animate-in slide-in-from-top-1">
          {content}
        </div>
      )}
    </div>
  );
}

export default Accordion;
