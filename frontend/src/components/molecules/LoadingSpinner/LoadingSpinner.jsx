import React from 'react';
import { useTheme } from '../../context/useTheme';

export function LoadingSpinner({ size = 40, message = null }) {
  const { accentSolid, textMain } = useTheme();

  return (
    <div className="flex flex-col items-center justify-center gap-3">
      <div
        className="relative flex items-center justify-center"
        style={{ width: size, height: size }}
      >
        {/* Background circle */}
        <svg
          width={size}
          height={size}
          className="absolute"
          style={{ opacity: 0.1 }}
        >
          <circle
            cx={size / 2}
            cy={size / 2}
            r={(size - 8) / 2}
            stroke={accentSolid}
            strokeWidth="3"
            fill="transparent"
          />
        </svg>

        {/* Animated circle */}
        <svg
          width={size}
          height={size}
          className="absolute animate-spin"
          style={{ animationDuration: '1s' }}
        >
          <circle
            cx={size / 2}
            cy={size / 2}
            r={(size - 8) / 2}
            stroke={accentSolid}
            strokeWidth="3"
            fill="transparent"
            strokeDasharray={`${((size - 8) / 2) * Math.PI * 0.4} ${((size - 8) / 2) * Math.PI * 2.6}`}
            strokeLinecap="round"
          />
        </svg>
      </div>

      {message && (
        <p className="text-sm opacity-60" style={{ color: textMain }}>
          {message}
        </p>
      )}
    </div>
  );
}

export default LoadingSpinner;
