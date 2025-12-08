import React from 'react';
import { useTheme } from '../../context/useTheme';
import { Badge } from '../../atoms';

export function MediaCard({ title, description, readTime, isNew = true }) {
  const { border, bgCard } = useTheme();
  
  return (
    <div
      className="rounded-2xl overflow-hidden border transition-all duration-300 hover:shadow-lg"
      style={{ borderColor: border, background: bgCard }}
    >
      <div className="h-32 w-full relative bg-gradient-to-br from-blue-500 to-purple-600">
        <div className="absolute inset-0 bg-black/10" />
      </div>
      <div className="p-4">
        <div className="flex gap-2 mb-3">
          {isNew && <Badge label="New" active={true} />}
          <div className="text-xs opacity-50 font-medium py-1">{readTime || '5 min read'}</div>
        </div>
        <div className="text-lg font-bold mb-1">{title || 'Advanced Hooks Pattern'}</div>
        <div className="text-sm opacity-60 mb-4">
          {description || 'Master useReducer and useContext for state management.'}
        </div>
        <button className="text-sm font-semibold text-blue-500 hover:underline">
          Read Article →
        </button>
      </div>
    </div>
  );
}

export default MediaCard;
