import React from 'react';

function Arrow({ direction = 'up', size = 'md', color, className = '', animate = false, curved = false }) {
  const sizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-6 h-6',
    lg: 'w-8 h-8',
    xl: 'w-10 h-10',
  };

  const rotationClasses = {
    up: 'rotate-180',
    down: 'rotate-0',
    left: 'rotate-90',
    right: '-rotate-90',
  };

  if (curved) {
    return (
      <svg 
        width={80} 
        height={80} 
        viewBox="0 0 24 24" 
        fill="none" 
        stroke={color || '#ffffff'} 
        strokeWidth="1.2" 
        strokeLinecap="round" 
        strokeLinejoin="round"
        className={`transform rotate-180 ${animate ? 'animate-pulse' : ''}`}
        style={{ filter: 'drop-shadow(0 2px 4px rgba(0,0,0,0.3))' }}
      >
        <path d="M 7 6 C 6 15 19 14 19 8 C 19 4 13 4 13 9 S 15 17 15 21" />
        <path d="M 12 18 L 15 21 L 18 18" />
      </svg>
    );
  }

  return (
    <svg
      className={`${sizeClasses[size]} ${rotationClasses[direction]} ${animate ? 'animate-pulse' : ''} ${className}`}
      fill="none"
      stroke={color || 'currentColor'}
      strokeWidth="2"
      viewBox="0 0 24 24"
    >
      <path strokeLinecap="round" strokeLinejoin="round" d="M19 14l-7 7m0 0l-7-7m7 7V3" />
    </svg>
  );
}

export default Arrow;
