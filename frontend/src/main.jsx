import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App.jsx';
import './index.css';

// Pre-render theme synchronization to prevent FCP flash
(function syncTheme() {
  try {
    const persistedMode = window.localStorage.getItem('ui-theme-mode') ?? 'system';
    const isLightOS = window.matchMedia('(prefers-color-scheme: light)').matches;
    const effectiveTheme = persistedMode === 'system' 
      ? (isLightOS ? 'light' : 'dark') 
      : persistedMode;
    
    document.documentElement.dataset.theme = effectiveTheme;
    document.documentElement.style.colorScheme = effectiveTheme;
  } catch (e) {
    // Ignore storage errors
  }
})();

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
