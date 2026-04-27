import React from 'react';
import styles from './SkeletonCard.module.css';

export default function SkeletonCard() {
  return (
    <div className={styles.card}>
      <div className={`${styles.pulse} ${styles.thumb}`} />
      <div className={`${styles.pulse} ${styles.title}`} />
      <div className={`${styles.pulse} ${styles.sub}`} />
      <div className={`${styles.pulse} ${styles.footer}`} />
    </div>
  );
}
