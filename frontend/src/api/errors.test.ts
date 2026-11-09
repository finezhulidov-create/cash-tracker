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

describe('errorMessage — импорт выписки', () => {
  it('explains that the uploaded file is too large', () => {
    const error = new ApiError(413, 'Maximum upload size exceeded', 'HTTP_413')

    expect(errorMessage(error)).toBe('Файл слишком большой: можно загрузить до 10 МБ')
  })

  it('explains that the parsing model is not reachable', () => {
    const error = new ApiError(503, 'Statement parsing model is unavailable', 'IMPORT_MODEL_UNAVAILABLE')

    expect(errorMessage(error)).toMatch(/LM Studio запущена/)
  })

  it('explains that the model could not read the statement', () => {
    const error = new ApiError(502, 'Statement parsing model returned an unusable answer', 'IMPORT_BAD_RESPONSE')

    expect(errorMessage(error)).toMatch(/не смогла разобрать выписку/)
  })

  it('shows the server explanation for a file that is not PDF', () => {
    const error = new ApiError(400, 'Поддержка только пдф файлов', 'INVALID_REQUEST')

    expect(errorMessage(error)).toBe('Поддержка только пдф файлов')
  })
})
