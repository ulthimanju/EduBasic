import React from 'react';
import { useTheme } from '../../context/useTheme';

export function Skeleton({ width, height, borderRadius = '4px' }) {
  const { textMain } = useTheme();
  
  return (
    <div
      className="animate-pulse opacity-10"
      style={{ width, height, borderRadius, background: textMain }}
    />
  );
}

export default Skeleton;
