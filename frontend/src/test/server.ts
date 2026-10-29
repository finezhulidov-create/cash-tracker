import { setupServer } from 'msw/node'

/** Сетевой мок для тестов: каждый тест сам регистрирует нужные ответы через server.use(...). */
export const server = setupServer()

/** Адрес API в тестах (задан в vite.config.ts через VITE_API_URL). */
export const API = 'http://api.test'
