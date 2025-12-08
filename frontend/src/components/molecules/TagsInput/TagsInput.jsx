import React, { useState } from 'react';
import { useTheme } from '../../context/useTheme';

export function TagsInput({ initialTags = ['React', 'UI', 'Design'] }) {
  const [tags, setTags] = useState(initialTags);
  const [input, setInput] = useState('');
  const { bgInput, accentDim, textMain } = useTheme();

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && input.trim()) {
      setTags([...tags, input.trim()]);
      setInput('');
    }
  };

  return (
    <div
      className="p-2 rounded-xl border flex flex-wrap gap-2 focus-within:ring-2 focus-within:ring-blue-500/50 transition-all"
      style={{ background: bgInput, borderColor: 'transparent' }}
    >
      {tags.map((tag, i) => (
        <span
          key={i}
          className="px-2.5 py-1 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-colors"
          style={{ background: accentDim, color: textMain }}
        >
          {tag}
          <button
            onClick={() => setTags(tags.filter((_, idx) => idx !== i))}
            className="hover:text-red-500 hover:scale-110 transition-transform"
          >
            ×
          </button>
        </span>
      ))}
      <input
        className="flex-1 min-w-[60px] bg-transparent outline-none text-sm px-1 py-1"
        placeholder="Add tag..."
        value={input}
        onChange={(e) => setInput(e.target.value)}
        onKeyDown={handleKeyDown}
        style={{ color: textMain }}
      />
    </div>
  );
}

export default TagsInput;
