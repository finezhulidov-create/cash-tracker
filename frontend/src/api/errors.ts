import { plural } from '../lib/plural'
import { ApiError } from './client'

/** «через минуту» / «через 25 минут» / «через 2 часа» — по заголовку Retry-After. */
function waitHint(seconds?: number): string {
  if (!seconds || seconds <= 60) return 'через минуту'
  const minutes = Math.ceil(seconds / 60)
  if (minutes < 60) return `через ${minutes} ${plural(minutes, ['минуту', 'минуты', 'минут'])}`
  const hours = Math.ceil(minutes / 60)
  return `через ${hours} ${plural(hours, ['час', 'часа', 'часов'])}`
}

/** Понятный пользователю текст ошибки на русском. */
export function errorMessage(error: unknown): string {
  if (!(error instanceof ApiError)) return 'Что-то пошло не так. Попробуйте ещё раз.'

  switch (error.errorCode) {
    case 'NETWORK_ERROR':
      return 'Нет связи с сервером. Проверьте подключение и попробуйте ещё раз.'
    case 'RATE_LIMIT_EXCEEDED':
      return 'Слишком много попыток, попробуйте через минуту'
    case 'INVALID_CREDENTIALS':
      return 'Неверный email или пароль'
    case 'EMAIL_ALREADY_EXISTS':
      return 'Этот email уже зарегистрирован'
    case 'RESOURCE_ALREADY_EXISTS':
      return 'Такая запись уже есть'
    case 'NOT_VALID_TRANSACTION_SPLIT_SUM':
      return 'Сумма частей должна совпадать с суммой траты'
    case 'CATEGORY_IN_USE':
      return 'К категории привязаны траты — удалить её нельзя'
    case 'AI_DISABLED':
      return 'ИИ-советник не настроен на этом сервере'
    case 'AI_BUSY':
      return `Модель сейчас отвечает на другой запрос. Попробуйте ${waitHint(error.retryAfter)}.`
    case 'AI_RATE_LIMIT_EXCEEDED':
      return `Лимит анализов исчерпан. Новый можно запросить ${waitHint(error.retryAfter)}.`
    case 'AI_UNAVAILABLE':
      return 'Сервис ИИ временно недоступен. Попробуйте позже.'
    case 'AI_AUTH_FAILED':
      return 'Сервер не смог войти в сервис ИИ. Администратору нужно проверить ключ.'
    case 'AI_QUOTA_EXHAUSTED':
      return 'У сервиса ИИ закончились токены. Сообщите администратору.'
    case 'AI_BAD_RESPONSE':
      return 'Модель вернула непригодный ответ. Попробуйте ещё раз.'
    case 'IMPORT_MODEL_UNAVAILABLE':
      return 'Модель для разбора выписок недоступна. Проверьте, что LM Studio запущена и её сервер включён.'
    case 'IMPORT_BAD_RESPONSE':
      return 'Модель не смогла разобрать выписку. Попробуйте ещё раз или выберите модель крупнее.'
  }

  if (error.status === 413) return 'Файл слишком большой: можно загрузить до 10 МБ'
  if (error.status === 429) return 'Слишком много попыток, попробуйте через минуту'
  if (error.status >= 500) return 'Ошибка на сервере. Попробуйте позже.'
  if (error.status === 404) return 'Не найдено — возможно, запись уже удалена'
  return error.message || 'Не удалось выполнить действие'
}

export function isApiError(error: unknown, errorCode: string): boolean {
  return error instanceof ApiError && error.errorCode === errorCode
}
