import React from 'react';
import { AlertCircle } from 'lucide-react';

/**
 * Inline error message for API / hook error states.
 *
 * @param {{ message?: string }} props
 */
export default function ErrorMessage({ message = 'Something went wrong.' }) {
  return (
    <div className="error-message" role="alert" aria-live="polite">
      <AlertCircle className="error-message__icon" size={16} strokeWidth={1.5} aria-hidden="true" />
      <span>{message}</span>
    </div>
  );
}
