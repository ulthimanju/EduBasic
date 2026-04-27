import React from 'react';
import styles from './ProgressBar.module.css';

export default function ProgressBar({ percent, showLabel = false, size = 'md' }) {
  return (
    <div className={`${styles.track} ${styles[size]}`}>
      <div
        className={styles.fill}
        style={{ width: `${Math.min(100, Math.max(0, percent))}%` }}
      />
      {showLabel && (
        <span className={styles.label}>{percent}%</span>
      )}
    </div>
  );
}
