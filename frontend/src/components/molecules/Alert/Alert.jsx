import React from 'react';
import { useTheme } from '../../context/useTheme';

export function Alert({ type, message }) {
  const colors = useTheme();
  
  let bg = colors.bgCard;
  let text = colors.textMain;

  if (type === 'success') {
    bg = colors.successBg;
    text = colors.success;
  }
  if (type === 'warning') {
    bg = colors.warningBg;
    text = colors.warning;
  }
  if (type === 'error') {
    bg = colors.errorBg;
    text = colors.error;
  }

  return (
    <div
      className="p-4 rounded-2xl text-sm font-medium flex items-center gap-3"
      style={{ background: bg, color: text }}
    >
      <div className="w-1.5 h-1.5 rounded-full" style={{ background: 'currentColor' }} />
      {message}
    </div>
  );
}

export default Alert;
