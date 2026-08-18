import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      '/login': {
        target: 'http://localhost:8080',
        bypass: (req) => (req.method === 'GET' ? '/index.html' : undefined),
      },
      '/logout': 'http://localhost:8080',
    },
  },
})
