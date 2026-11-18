import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// The browser talks to http://localhost:5173 only. Vite forwards every /api call to the Spring Boot
// backend, so no CORS configuration is needed on the backend while developing.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.API_URL || 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
})
