import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Runs beside the grocery front end (5173) against the same API.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5174,
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
})
