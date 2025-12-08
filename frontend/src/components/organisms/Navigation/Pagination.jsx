import React from 'react';

export function Pagination({ currentPage, totalPages, onPageChange }) {
  const pages = [1, 2, 3, '...', totalPages];
  
  return (
    <div className="flex items-center gap-2">
      <button className="w-8 h-8 rounded-lg flex items-center justify-center text-sm font-bold opacity-50 hover:bg-black/5 dark:hover:bg-white/10 transition-colors">
        ←
      </button>
      {pages.map((p, i) => (
        <button
          key={i}
          onClick={() => typeof p === 'number' && onPageChange?.(p)}
          className={`w-8 h-8 rounded-lg flex items-center justify-center text-sm font-bold transition-all ${
            p === currentPage
              ? 'shadow-sm'
              : 'opacity-50 hover:bg-black/5 dark:hover:bg-white/10'
          }`}
          style={{
            background: p === currentPage ? '#fff' : 'transparent',
            color: p === currentPage ? '#000' : 'inherit',
          }}
        >
          {p}
        </button>
      ))}
      <button className="w-8 h-8 rounded-lg flex items-center justify-center text-sm font-bold opacity-50 hover:bg-black/5 dark:hover:bg-white/10 transition-colors">
        →
      </button>
    </div>
  );
}

export default Pagination;
