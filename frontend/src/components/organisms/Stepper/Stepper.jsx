import React from 'react';
import { useTheme } from '../../context/useTheme';

export function Stepper({ currentStep, steps, onStepClick }) {
  const { accentSolid, bgInput, bgPanel, textMain, textMuted } = useTheme();
  
  return (
    <div className="flex items-start w-full">
      {steps.map((step, i) => {
        const isActive = i + 1 <= currentStep;
        const isCompleted = i + 1 < currentStep;
        const isLast = i === steps.length - 1;

        return (
          <React.Fragment key={i}>
            <div
              className={`flex flex-col items-center gap-2 relative z-10 ${onStepClick ? 'cursor-pointer' : ''}`}
              onClick={onStepClick ? () => onStepClick(i) : undefined}
              role={onStepClick ? 'button' : undefined}
              tabIndex={onStepClick ? 0 : undefined}
              onKeyDown={onStepClick ? (e) => { if (e.key === 'Enter' || e.key === ' ') onStepClick(i); } : undefined}
            >
              <div
                className="w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold transition-all border-[3px]"
                style={{
                  background: isActive ? accentSolid : bgInput,
                  borderColor: isActive ? accentSolid : bgInput,
                  color: isActive ? '#fff' : textMuted,
                  boxShadow: isActive ? `0 0 0 3px ${bgPanel}` : 'none',
                }}
              >
                {isCompleted ? '✓' : i + 1}
              </div>
              <span
                className={`text-[10px] font-bold uppercase tracking-wider text-center w-max ${
                  isActive ? '' : 'opacity-40'
                }`}
                style={{ color: textMain }}
              >
                {step}
              </span>
            </div>

            {!isLast && (
              <div
                className="flex-1 h-[2px] mt-4 mx-2 rounded-full transition-all duration-500"
                style={{ background: isCompleted ? accentSolid : bgInput }}
              />
            )}
          </React.Fragment>
        );
      })}
    </div>
  );
}

export default Stepper;
