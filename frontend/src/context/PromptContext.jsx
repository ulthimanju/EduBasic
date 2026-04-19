import React, { createContext, useContext, useState, useCallback } from 'react';

const PromptContext = createContext(null);

/**
 * PromptProvider — Global state for custom confirmation and message dialogs.
 */
export const PromptProvider = ({ children }) => {
  const [prompt, setPrompt] = useState(null);

  /**
   * openPrompt — Displays a dialog.
   * @param {Object} config - { type, severity, title, description, confirmLabel, cancelLabel, onConfirm, onCancel, isBlocking }
   */
  const openPrompt = useCallback((config) => {
    setPrompt({
      ...config,
      id: Date.now(), // Ensure re-renders for same content
    });
  }, []);

  const closePrompt = useCallback(() => {
    setPrompt(null);
  }, []);

  return (
    <PromptContext.Provider value={{ openPrompt, closePrompt, prompt }}>
      {children}
    </PromptContext.Provider>
  );
};

export const usePrompt = () => {
  const context = useContext(PromptContext);
  if (!context) {
    throw new Error('usePrompt must be used within a PromptProvider');
  }
  return context;
};
