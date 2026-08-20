import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const rootDir = path.dirname(fileURLToPath(import.meta.url))
const apiUser = process.env.VITE_API_USER || process.env.LOCAL_ANALYST_USERNAME || 'analyst'
const apiPassword = process.env.VITE_API_PASSWORD || process.env.LOCAL_ANALYST_PASSWORD || 'analyst_change_me'
const apiProxyTarget = process.env.API_PROXY_TARGET || 'http://127.0.0.1:8080'

export default defineConfig({
  envDir: path.resolve(rootDir, '../..'),
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(rootDir, './src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: apiProxyTarget,
        changeOrigin: true,
        auth: `${apiUser}:${apiPassword}`,
      },
      '/actuator': {
        target: apiProxyTarget,
        changeOrigin: true,
      },
    },
  },
})
