import React from 'react';
import { useTheme } from '../../context/useTheme';
import { Checkbox } from '../../atoms';

/**
 * Sidebar Component - Fixed width sidebar with theme support
 * 
 * @param {Object} props
 * @param {string} props.title - Sidebar title
 * @param {Array} props.items - Array of items with { id, label, checked, onChange }
 * @param {React.ReactNode} props.children - Custom content (overrides items if provided)
 * @param {string} props.width - Sidebar width class (default: 'w-64')
 */
export function Sidebar({ title, items = [], children, width = 'w-64' }) {
  const colors = useTheme();

  return (
    <div
      className={`${width} border-r p-6 overflow-y-auto`}
      style={{ background: colors.bgCard, borderColor: colors.border }}
    >
      {title && (
        <h2 className="text-lg font-semibold mb-6" style={{ color: colors.textMain }}>
          {title}
        </h2>
      )}
      
      {children ? (
        children
      ) : (
        <div className="space-y-3">
          {items.map((item) => (
            <Checkbox
              key={item.id}
              label={item.label}
              checked={item.checked}
              onChange={item.onChange}
            />
          ))}
        </div>
      )}
    </div>
  );
}

export default Sidebar;
