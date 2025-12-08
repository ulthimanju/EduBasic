import React from 'react';
import { useTheme } from '../../context/useTheme';

export function Calendar({ month = 'October', year = 2023, selectedDate = 14 }) {
  const { border, bgCard, accentSolid } = useTheme();
  const days = ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'];
  const dates = Array.from({ length: 31 }, (_, i) => i + 1);
  const startOffset = 2;

  return (
    <div
      className="p-4 rounded-2xl border backdrop-blur-sm"
      style={{ borderColor: border, background: bgCard }}
    >
      <div className="flex justify-between items-center mb-4">
        <span className="font-bold">{`${month} ${year}`}</span>
        <div className="flex gap-2">
          <button className="w-6 h-6 flex items-center justify-center rounded-full hover:bg-white/10 opacity-50">
            ←
          </button>
          <button className="w-6 h-6 flex items-center justify-center rounded-full hover:bg-white/10 opacity-50">
            →
          </button>
        </div>
      </div>
      <div className="grid grid-cols-7 gap-1 text-center text-xs mb-2 opacity-50 font-medium">
        {days.map((d) => (
          <div key={d}>{d}</div>
        ))}
      </div>
      <div className="grid grid-cols-7 gap-1 text-center text-sm">
        {Array(startOffset)
          .fill(null)
          .map((_, i) => (
            <div key={`empty-${i}`} />
          ))}
        {dates.map((d) => (
          <div
            key={d}
            className={`w-8 h-8 flex items-center justify-center rounded-full cursor-pointer transition-all ${
              d === selectedDate ? 'shadow-md font-bold text-white' : 'hover:bg-white/10'
            }`}
            style={{ background: d === selectedDate ? accentSolid : 'transparent' }}
          >
            {d}
          </div>
        ))}
      </div>
    </div>
  );
}

export default Calendar;
