import React from 'react';
import { useTheme } from '../../context/useTheme';

export function Badge({ label, active, variant }) {
  const { successBg, success, bgInput, textMutedDim } = useTheme();
  
  // Determine colors based on variant
  const getColors = () => {
    if (!active) {
      return {
        background: bgInput,
        color: textMutedDim,
      };
    }

    // Check variant or label to determine type
    const type = variant || label.toLowerCase();
    
    if (type.includes('announcement')) {
      // Blue color for announcements
      return {
        background: 'rgba(59, 130, 246, 0.15)',
        color: 'rgb(59, 130, 246)',
      };
    } else if (type.includes('update')) {
      // Green color for updates
      return {
        background: successBg,
        color: success,
      };
    }
    
    // Default to success colors
    return {
      background: successBg,
      color: success,
    };
  };

  const colors = getColors();
  
  return (
    <span
      className="px-2.5 py-1 rounded-full text-[11px] font-bold uppercase tracking-wide"
      style={{
        background: colors.background,
        color: colors.color,
      }}
    >
      {label}
    </span>
  );
}

export default Badge;
