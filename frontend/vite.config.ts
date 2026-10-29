/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  server: {
    // Порт 5173 разрешён в app.cors.allowed-origins бэкенда
    port: 5173,
    strictPort: true,
  },
  build: {
    // Основной чанк — React, Chakra UI и TanStack Query (~585 КБ); экраны уже вынесены в отдельные файлы
    chunkSizeWarningLimit: 650,
    // Не вшивать мелкие файлы (подмножества шрифтов) в CSS как data:-ссылки: строгая CSP
    // в deploy/Caddyfile разрешает шрифты только с собственного домена (font-src 'self')
    assetsInlineLimit: 0,
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
    css: false,
    testTimeout: 15_000,
    // Относительные URL Node-овский fetch не принимает, поэтому в тестах API живёт на выдуманном хосте
    env: { VITE_API_URL: 'http://api.test' },
  },
})
