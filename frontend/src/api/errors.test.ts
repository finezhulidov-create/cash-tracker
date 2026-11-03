import { ApiError } from './client'
import { errorMessage } from './errors'

const limited = (retryAfter?: number) =>
  errorMessage(new ApiError(429, 'x', 'AI_RATE_LIMIT_EXCEEDED', {}, retryAfter))

describe('errorMessage — ИИ-советник', () => {
  it('names the wait in the right Russian form', () => {
    expect(limited(undefined)).toBe('Лимит анализов исчерпан. Новый можно запросить через минуту.')
    expect(limited(45)).toContain('через минуту')
    expect(limited(61)).toContain('через 2 минуты')
    expect(limited(60 * 21)).toContain('через 21 минуту')
    expect(limited(60 * 25)).toContain('через 25 минут')
    expect(limited(3600)).toContain('через 1 час')
    expect(limited(3601)).toContain('через 2 часа')
    expect(limited(3600 * 5 + 1)).toContain('через 6 часов')
  })

  it('does not leak the technical text of the server', () => {
    const message = errorMessage(new ApiError(503, 'AI service is temporarily unavailable', 'AI_UNAVAILABLE'))

    expect(message).toBe('Сервис ИИ временно недоступен. Попробуйте позже.')
  })

  it('keeps the usual messages working', () => {
    expect(errorMessage(new ApiError(0, 'x', 'NETWORK_ERROR'))).toContain('Нет связи с сервером')
    expect(errorMessage(new ApiError(500, 'x', 'INTERNAL_ERROR'))).toBe('Ошибка на сервере. Попробуйте позже.')
    expect(errorMessage(new Error('boom'))).toBe('Что-то пошло не так. Попробуйте ещё раз.')
  })
})
