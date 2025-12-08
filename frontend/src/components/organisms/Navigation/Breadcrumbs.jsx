import React from 'react';

export function Breadcrumbs({ items }) {
  return (
    <div className="flex items-center gap-2 text-sm">
      {items.map((item, i) => (
        <React.Fragment key={i}>
          <span
            className={`font-medium ${
              i === items.length - 1 ? '' : 'opacity-50 cursor-pointer hover:underline'
            }`}
          >
            {item}
          </span>
          {i < items.length - 1 && <span className="opacity-30">/</span>}
        </React.Fragment>
      ))}
    </div>
  );
}

export default Breadcrumbs;
