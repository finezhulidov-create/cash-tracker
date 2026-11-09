import type { LucideIcon } from 'lucide-react'
import { CircleAlert, CircleCheck, TriangleAlert } from 'lucide-react'
import { formatMoney, fromCents } from '../../lib/money'
import type { SplitStatus } from '../../lib/splits'

export interface StatusView {
  Icon: LucideIcon
  /** Семантический токен цвета Chakra */
  color: 'fg.muted' | 'fg.success' | 'fg.warning' | 'fg.error'
  text: string
  /** Показывать кнопку «Добавить остаток» */
  canFill: boolean
}

/** Строка статуса под разбивкой: что мешает сохранить и что можно сделать. */
export function describeSplitStatus(
  status: SplitStatus,
  categoryName: (id: number) => string,
  hasDescription: boolean,
): StatusView {
  const money = (cents: number) => formatMoney(fromCents(cents))

  switch (status.kind) {
    case 'no-amount':
      return { Icon: CircleAlert, color: 'fg.muted', text: 'Введите сумму траты', canFill: false }
    case 'no-category':
      return { Icon: CircleAlert, color: 'fg.warning', text: 'Выберите категорию', canFill: false }
    case 'single':
      return hasDescription
        ? {
            Icon: CircleCheck,
            color: 'fg.success',
            text: `Вся сумма — в категории «${categoryName(status.categoryId)}»`,
            canFill: false,
          }
        : missingDescription()
    case 'zero-part':
      return {
        Icon: CircleAlert,
        color: 'fg.warning',
        text:
          status.remainingCents > 0
            ? `Укажите суммы частей · осталось ${money(status.remainingCents)}`
            : 'Укажите сумму для каждой части',
        canFill: status.remainingCents > 0,
      }
    case 'remaining':
      return {
        Icon: CircleAlert,
        color: 'fg.warning',
        text: `Осталось распределить ${money(status.cents)}`,
        canFill: true,
      }
    case 'over':
      return {
        Icon: TriangleAlert,
        color: 'fg.error',
        text: `Части больше суммы на ${money(status.cents)}`,
        canFill: false,
      }
    case 'balanced':
      return hasDescription
        ? {
            Icon: CircleCheck,
            color: 'fg.success',
            text: 'Сумма частей совпадает с суммой траты',
            canFill: false,
          }
        : missingDescription()
  }
}

function missingDescription(): StatusView {
  return { Icon: CircleAlert, color: 'fg.muted', text: 'Добавьте описание траты', canFill: false }
}
