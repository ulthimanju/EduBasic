import React, { useEffect, useRef } from 'react';
import { X } from 'lucide-react';
import styles from './ConfirmModal.module.css';

export default function ConfirmModal({
  title, message, confirmLabel = 'Confirm',
  onConfirm, onCancel, isLoading, isDangerous,
}) {
  const modalRef = useRef(null);

  useEffect(() => {
    // 1. Handle Escape key to close
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') onCancel();
    };
    window.addEventListener('keydown', handleKeyDown);

    // 2. Focus trap
    const modal = modalRef.current;
    if (modal) {
      const focusableElements = modal.querySelectorAll(
        'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
      );
      const firstElement = focusableElements[0];
      const lastElement = focusableElements[focusableElements.length - 1];

      // Focus first element on open
      firstElement?.focus();

      const handleTab = (e) => {
        if (e.key !== 'Tab') return;

        if (e.shiftKey) { // Shift + Tab
          if (document.activeElement === firstElement) {
            e.preventDefault();
            lastElement?.focus();
          }
        } else { // Tab
          if (document.activeElement === lastElement) {
            e.preventDefault();
            firstElement?.focus();
          }
        }
      };

      modal.addEventListener('keydown', handleTab);

      return () => {
        window.removeEventListener('keydown', handleKeyDown);
        modal.removeEventListener('keydown', handleTab);
      };
    }
  }, [onCancel]);

  return (
    <div className={styles.overlay} onClick={(e) => e.target === e.currentTarget && onCancel()}>
      <div 
        className={styles.modal}
        ref={modalRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
      >
        <button 
          className={styles.close} 
          onClick={onCancel}
          aria-label="Close modal"
        >
          <X size={18} />
        </button>
        <h3 className={styles.title} id="modal-title">{title}</h3>
        <p className={styles.message}>{message}</p>
        <div className={styles.actions}>
          <button className={styles.cancel} onClick={onCancel} disabled={isLoading}>
            Cancel
          </button>
          <button
            className={`${styles.confirm} ${isDangerous ? styles.danger : styles.primary}`}
            onClick={onConfirm}
            disabled={isLoading}
          >
            {isLoading ? 'Loading...' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
