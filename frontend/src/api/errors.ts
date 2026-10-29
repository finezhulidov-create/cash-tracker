import { ApiError } from './client'

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
  }

  if (error.status === 429) return 'Слишком много попыток, попробуйте через минуту'
  if (error.status >= 500) return 'Ошибка на сервере. Попробуйте позже.'
  if (error.status === 404) return 'Не найдено — возможно, запись уже удалена'
  return error.message || 'Не удалось выполнить действие'
}

export function isApiError(error: unknown, errorCode: string): boolean {
  return error instanceof ApiError && error.errorCode === errorCode
}
