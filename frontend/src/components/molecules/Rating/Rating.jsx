import React from 'react';
import { useTheme } from '../../context/useTheme';

export function Rating({ value, onChange }) {
  const { bgInput } = useTheme();
  
  return (
    <div className="flex gap-1">
      {[1, 2, 3, 4, 5].map((star) => (
        <span
          key={star}
          className="text-xl cursor-pointer hover:scale-110 transition-transform"
          style={{ color: star <= value ? '#FFD60A' : bgInput }}
          onClick={() => onChange?.(star)}
        >
          ★
        </span>
      ))}
    </div>
  );
}

export default Rating;
