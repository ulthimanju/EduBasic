import React from 'react';
import { useTheme } from '../../context/useTheme';
import { ActionButton } from '../../atoms/Button/ActionButton';
import { Badge } from '../../atoms/Badge/Badge';

export function SuccessModal({ isOpen, onClose, title, username, email, role, buttonText = 'Okay' }) {
  const colors = useTheme();

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div
        className="absolute inset-0 bg-black/40 backdrop-blur-sm transition-opacity"
        onClick={onClose}
      />
      <div
        className="relative p-8 rounded-[28px] shadow-2xl max-w-md w-full animate-in zoom-in-95 duration-200"
        style={{ background: colors.bgPanel }}
      >
        <div className="text-center mb-6">
          <div 
            className="w-16 h-16 rounded-full mx-auto mb-4 flex items-center justify-center text-4xl" 
            style={{ background: colors.successBg, color: colors.success }}
          >
            ✓
          </div>
          <div className="text-2xl font-bold mb-2">{title}</div>
          <div className="text-sm opacity-70 px-4 space-y-2">
            <p>Your account has been created successfully.</p>
            {username && (
              <div className="mt-4 p-4 rounded-xl space-y-2" style={{ background: colors.bgInput }}>
                <div className="font-semibold text-base">{username}</div>
                <div className="text-xs opacity-60">{email}</div>
                <div className="flex justify-center mt-2">
                  <Badge label={role} active={true} />
                </div>
              </div>
            )}
          </div>
        </div>

        <div className="flex justify-center">
          <ActionButton label={buttonText} onClick={onClose} className="px-12" />
        </div>
      </div>
    </div>
  );
}

export default SuccessModal;
