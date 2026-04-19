import React, { useEffect, useRef } from 'react';
import { AlertCircle, CheckCircle2, Info, AlertTriangle, X } from 'lucide-react';
import { usePrompt } from '../../../context/PromptContext';

/**
 * PromptDialog — A reusable modal for confirmations and system messages.
 *
 * Types:
 *   'confirm' — Primary + Secondary action
 *   'message' — Primary action only (info)
 *
 * Severities:
 *   'info'    — Neutral (primary color)
 *   'success' — Green-toned
 *   'warning' — Yellow-toned
 *   'danger'  — Red/Brand-toned
 */
const PromptDialog = () => {
  const { prompt, closePrompt } = usePrompt();
  const modalRef = useRef(null);

  useEffect(() => {
    if (prompt) {
      // Focus the confirm button if exists or dialog itself
      const firstBtn = modalRef.current?.querySelector('button');
      firstBtn?.focus();

      const handleEscape = (e) => {
        if (e.key === 'Escape' && !prompt.isBlocking) {
          handleCancel();
        }
      };
      window.addEventListener('keydown', handleEscape);
      return () => window.removeEventListener('keydown', handleEscape);
    }
  }, [prompt]);

  if (!prompt) return null;

  const {
    type = 'confirm',
    severity = 'info',
    title,
    description,
    confirmLabel = 'Confirm',
    cancelLabel = 'Cancel',
    onConfirm,
    onCancel,
    isBlocking = false,
  } = prompt;

  const handleConfirm = () => {
    onConfirm?.();
    closePrompt();
  };

  const handleCancel = () => {
    onCancel?.();
    closePrompt();
  };

  const getIcon = () => {
    switch (severity) {
      case 'success': return <CheckCircle2 size={32} className="text-emerald-500" />;
      case 'warning': return <AlertTriangle size={32} className="text-amber-500" />;
      case 'danger':  return <AlertCircle size={32} className="text-accent" />;
      default:        return <Info size={32} className="text-blue-500" />;
    }
  };

  const getSeverityClass = () => {
    switch (severity) {
      case 'danger': return 'prompt-dialog--danger';
      case 'warning': return 'prompt-dialog--warning';
      case 'success': return 'prompt-dialog--success';
      default: return '';
    }
  };

  return (
    <div 
      className="fixed inset-0 z-[200] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-page-enter"
      onClick={!isBlocking ? handleCancel : undefined}
    >
      <div 
        ref={modalRef}
        className={`panel max-w-sm w-full p-8 text-center shadow-2xl ${getSeverityClass()}`}
        onClick={(e) => e.stopPropagation()}
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="prompt-title"
        aria-describedby="prompt-desc"
      >
        <div className="mb-4 flex justify-center">
          {getIcon()}
        </div>

        <h2 id="prompt-title" className="text-xl font-bold mb-2">
          {title}
        </h2>
        
        <p id="prompt-desc" className="text-text-secondary text-sm mb-8 leading-relaxed">
          {description}
        </p>

        <div className="flex gap-3">
          {type === 'confirm' && (
            <button 
              onClick={handleCancel}
              className="btn btn-secondary flex-1 py-3"
            >
              {cancelLabel}
            </button>
          )}
          <button 
            onClick={handleConfirm}
            className={`btn btn-primary flex-1 py-3 ${severity === 'danger' ? 'bg-accent hover:bg-accent-hover' : ''}`}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
};

export default PromptDialog;
