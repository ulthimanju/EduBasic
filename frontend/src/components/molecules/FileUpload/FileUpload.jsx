import React from 'react';
import { useTheme } from '../../context/useTheme';

export function FileUpload({ onUpload }) {
  const { border } = useTheme();
  
  return (
    <div
      className="h-full min-h-[140px] rounded-xl border-2 border-dashed flex flex-col items-center justify-center gap-3 transition-colors hover:bg-black/5 dark:hover:bg-white/5 cursor-pointer"
      style={{ borderColor: border }}
    >
      <div className="w-10 h-10 rounded-full bg-blue-500/10 flex items-center justify-center text-blue-500 text-xl">
        ☁️
      </div>
      <div className="text-center">
        <div className="text-sm font-semibold">Click to Upload</div>
        <div className="text-xs opacity-50">SVG, PNG, JPG</div>
      </div>
    </div>
  );
}

export default FileUpload;
