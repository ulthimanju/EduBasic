import React from 'react';
import styles from './FormField.module.css';

/**
 * Standard form field wrapper with label and error display.
 * 
 * @param {object} props
 * @param {string} props.label - Field label
 * @param {string} props.error - Error message
 * @param {boolean} props.required - Whether the field is required
 * @param {React.ReactNode} props.children - Input element
 * @param {string} props.className - Additional class names
 */
export default function FormField({ 
  label, 
  error, 
  required, 
  children, 
  className = '' 
}) {
  return (
    <div className={`${styles.field} ${className}`}>
      {label && (
        <label className={styles.label}>
          {label}
          {required && <span className={styles.required}>*</span>}
        </label>
      )}
      
      <div className={styles.inputWrapper}>
        {children}
      </div>
      
      {error && (
        <p className={styles.error} role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
