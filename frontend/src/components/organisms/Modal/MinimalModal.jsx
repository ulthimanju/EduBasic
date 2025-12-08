import React, { useState, useEffect, useRef } from 'react';
import { useTheme } from '../../context/useTheme';

export function MinimalModal({ onClose, title, description, onSubmit }) {
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);
  const { bgPanel, bgInput, textMain, accentSolid } = useTheme();

  const isMounted = useRef(true);
  useEffect(() => {
    return () => {
      isMounted.current = false;
    };
  }, []);

  function handleSend() {
    if (!email.trim()) return;
    setSent(true);
    setTimeout(() => {
      if (isMounted.current) {
        setSent(false);
        onSubmit?.(email);
        onClose();
      }
    }, 900);
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div
        className="absolute inset-0 bg-black/40 backdrop-blur-sm transition-opacity"
        onClick={onClose}
      />
      <div
        className="relative p-6 rounded-[28px] shadow-2xl max-w-sm w-full animate-in zoom-in-95 duration-200"
        style={{ background: bgPanel }}
      >
        <div className="text-center mb-6">
          <div className="w-12 h-12 rounded-full mx-auto mb-4 flex items-center justify-center text-2xl bg-blue-500/10 text-blue-500">
            ✉️
          </div>
          <div className="text-xl font-bold mb-2">{title || 'Invite Teammates'}</div>
          <div className="text-sm opacity-60 px-4">
            {description || 'Share a short link or invite by email to collaborate.'}
          </div>
        </div>

        <div className="space-y-3">
          <input
            className="w-full px-4 py-3 rounded-xl outline-none focus:ring-2 focus:ring-opacity-50 transition-all text-center font-medium"
            placeholder="name@school.edu"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            style={{
              background: bgInput,
              color: textMain,
              '--tw-ring-color': accentSolid,
            }}
          />
          <button
            className="w-full py-3 rounded-xl text-white font-bold shadow-lg active:scale-[0.98] transition-transform"
            style={{ background: '#007AFF' }}
            onClick={handleSend}
          >
            {sent ? 'Sent!' : 'Send Invite'}
          </button>
        </div>
        <button
          className="absolute top-4 right-4 w-8 h-8 flex items-center justify-center rounded-full hover:bg-black/5 dark:hover:bg-white/10 transition-colors text-sm font-bold opacity-50"
          onClick={onClose}
        >
          ✕
        </button>
      </div>
    </div>
  );
}

export default MinimalModal;
