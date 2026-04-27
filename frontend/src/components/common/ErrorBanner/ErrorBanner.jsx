import React from 'react';
import { AlertCircle, RefreshCw } from 'lucide-react';
import styles from './ErrorBanner.module.css';

export default function ErrorBanner({ message, onRetry }) {
  return (
    <div className={styles.banner}>
      <AlertCircle size={18} />
      <span>{message ?? 'Something went wrong'}</span>
      {onRetry && (
        <button className={styles.retry} onClick={onRetry}>
          <RefreshCw size={14} /> Retry
        </button>
      )}
    </div>
  );
}
