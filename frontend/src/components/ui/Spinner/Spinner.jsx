import React from 'react';

/**
 * Accessible loading spinner.
 *
 * @param {{ size?: 'sm' | 'md' | 'lg', label?: string }} props
 */
export default function Spinner({ size = 'md', label = 'Loading…' }) {
  return (
    <div className={`spinner spinner--${size}`} role="status" aria-label={label}>
      <div className="spinner__pulse" />
    </div>
  );
}
