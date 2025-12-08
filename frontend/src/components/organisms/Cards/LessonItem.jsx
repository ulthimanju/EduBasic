import React from 'react';
import { useTheme } from '../../context/useTheme';

export function LessonItem({ title, meta, open, onToggle }) {
  const { bgCard, bgInput, border } = useTheme();
  
  return (
    <div
      className="p-4 rounded-2xl transition-all duration-300 cursor-pointer active:scale-[0.99]"
      style={{ background: bgCard }}
      onClick={onToggle}
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <div className="w-10 h-10 rounded-full flex items-center justify-center bg-black/5 dark:bg-white/10 text-lg">
            {open ? '📖' : '📘'}
          </div>
          <div>
            <div className="font-semibold text-sm">{title}</div>
            <div className="text-xs font-medium opacity-50">{meta}</div>
          </div>
        </div>
        <div
          className={`w-8 h-8 rounded-full flex items-center justify-center transition-transform duration-300 ${
            open ? 'rotate-90' : ''
          }`}
          style={{ background: bgInput }}
        >
          <span className="opacity-50 text-xs">▶</span>
        </div>
      </div>

      {open && (
        <div
          className="mt-4 pt-4 border-t text-sm opacity-70 leading-relaxed"
          style={{ borderColor: border }}
        >
          This lesson includes example exercises, a short quiz, and a downloadable cheat sheet.
        </div>
      )}
    </div>
  );
}

export default LessonItem;
