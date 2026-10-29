import type { TransactionFilters } from '../../api/types'
import { parseAmount } from '../../lib/money'

/** Фильтры экрана «Операции» в том виде, как они лежат в URL (все значения — строки). */
export interface FilterFields {
  q: string
  cat: string
  min: string
  max: string
  from: string
  to: string
}

export const FILTER_KEYS = ['q', 'cat', 'min', 'max', 'from', 'to'] as const satisfies readonly (keyof FilterFields)[]

export const EMPTY_FILTERS: FilterFields = { q: '', cat: '', min: '', max: '', from: '', to: '' }

/** Обратное преобразование: в URL попадают только заполненные поля. */
export function filtersToParams(fields: FilterFields): URLSearchParams {
  const params = new URLSearchParams()
  for (const key of FILTER_KEYS) {
    if (fields[key] !== '') params.set(key, fields[key])
  }
  return params
}

export function parseFilters(params: URLSearchParams): FilterFields {
  return {
    q: params.get('q') ?? '',
    cat: params.get('cat') ?? '',
    min: params.get('min') ?? '',
    max: params.get('max') ?? '',
    from: params.get('from') ?? '',
    to: params.get('to') ?? '',
  }
}

/** Сколько фильтров включено, не считая строки поиска (она всегда на виду). */
export function countActiveFilters(fields: FilterFields): number {
  return (['cat', 'min', 'max', 'from', 'to'] as const).filter((key) => fields[key] !== '').length
}

export function hasAnyFilter(fields: FilterFields): boolean {
  return fields.q.trim() !== '' || countActiveFilters(fields) > 0
}

/**
 * Ошибка в сочетании фильтров, из-за которой бэкенд ответил бы 400: «с» позже «по» или «от» больше «до».
 * Возвращает текст для пользователя либо null.
 */
export function validateFilters(fields: FilterFields): string | null {
  if (fields.from && fields.to && fields.from > fields.to) {
    return 'Дата «с» не может быть позже даты «по»'
  }
  const min = parseAmount(fields.min)
  const max = parseAmount(fields.max)
  if (min !== null && max !== null && min > max) {
    return 'Сумма «от» не может быть больше суммы «до»'
  }
  return null
}

/** Параметры запроса к API. Дата «по» включает весь день до последней микросекунды. */
export function toApiFilters(fields: FilterFields): TransactionFilters {
  const q = fields.q.trim()
  const categoryId = Number(fields.cat)
  const min = parseAmount(fields.min)
  const max = parseAmount(fields.max)
  return {
    q: q || undefined,
    categoryId: Number.isInteger(categoryId) && categoryId > 0 ? categoryId : undefined,
    minAmount: min ?? undefined,
    maxAmount: max ?? undefined,
    from: fields.from ? `${fields.from}T00:00:00` : undefined,
    to: fields.to ? `${fields.to}T23:59:59.999999` : undefined,
  }
}
