import React from 'react';
import { X } from 'lucide-react';
import styles from './ConfirmModal.module.css';

export default function ConfirmModal({
  title, message, confirmLabel = 'Confirm',
  onConfirm, onCancel, isLoading, isDangerous,
}) {
  return (
    <div className={styles.overlay}>
      <div className={styles.modal}>
        <button className={styles.close} onClick={onCancel}><X size={18} /></button>
        <h3 className={styles.title}>{title}</h3>
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
