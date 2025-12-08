import React from 'react';
import { useTheme } from '../../context/useTheme';

/**
 * Table Component - Matches app theme and provides flexible column rendering
 * 
 * @param {Object} props
 * @param {Array} props.data - Array of row objects
 * @param {Array} props.columns - Array of column definitions: { key, label, render (optional) }
 * @param {Function} props.onRowClick - Optional callback when row is clicked
 * @param {string} props.keyField - Field to use as unique key (default: 'id')
 * @param {boolean} props.hoverable - Enable hover effect on rows (default: true)
 * @param {boolean} props.striped - Alternate row colors (default: true)
 * @param {React.ReactNode} props.emptyState - Custom empty state component
 */
export function Table({
  data = [],
  columns = [],
  onRowClick,
  keyField = 'id',
  hoverable = true,
  striped = true,
  emptyState
}) {
  const colors = useTheme();

  if (data.length === 0) {
    return emptyState || (
      <div
        className="text-center py-8 p-6 rounded-xl"
        style={{ background: colors.bgCard, color: colors.textMain }}
      >
        <p className="opacity-60">No data available</p>
      </div>
    );
  }

  return (
    <div
      className="overflow-x-auto rounded-xl border"
      style={{ borderColor: colors.border }}
    >
      <table className="w-full" style={{ background: colors.bgCard }}>
        <thead>
          <tr style={{ background: colors.accent, borderBottom: `2px solid ${colors.border}` }}>
            {columns.map((column) => (
              <th
                key={column.key}
                className="text-center px-4 py-3 font-semibold text-white"
                style={{ color: '#ffffff' }}
              >
                {column.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.map((row, index) => (
            <tr
              key={row[keyField] || index}
              onClick={() => onRowClick?.(row)}
              className={`${
                hoverable ? 'hover:opacity-80 transition-opacity cursor-pointer' : ''
              } ${striped && index % 2 === 1 ? 'opacity-70' : ''}`}
              style={{
                borderBottom:
                  index !== data.length - 1 ? `1px solid ${colors.border}` : 'none',
                color: colors.textMain,
              }}
            >
              {columns.map((column) => (
                <td key={`${row[keyField]}-${column.key}`} className="px-4 py-3 text-center">
                  {column.render
                    ? column.render(row[column.key], row, index)
                    : row[column.key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
