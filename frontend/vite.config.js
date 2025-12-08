import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    // Optimize chunk splitting to reduce main bundle size
    rollupOptions: {
      output: {
        manualChunks: {
          // Separate vendor chunks
          'react-vendor': ['react', 'react-dom'],
          // Heavy dependencies only used in ViewContentPage
          'editors': ['@monaco-editor/react', 'mermaid'],
        }
      }
    },
    // Increase chunk size warning limit since we're handling large dependencies
    chunkSizeWarningLimit: 1000,
  }
})
