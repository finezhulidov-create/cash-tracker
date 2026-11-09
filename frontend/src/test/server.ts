import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'

/** Адрес API в тестах (задан в vite.config.ts через VITE_API_URL). */
export const API = 'http://api.test'

/**
 * Сетевой мок для тестов: каждый тест сам регистрирует нужные ответы через server.use(...).
 * Заранее задан один ответ: ИИ-советник выключен, как на сервере по умолчанию.
 */
export const server = setupServer(http.get(`${API}/v1/ai/status`, () => HttpResponse.json({ enabled: false })))
