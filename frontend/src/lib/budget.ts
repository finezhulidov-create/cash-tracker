import type { BudgetStatusDto } from '../api/types'
import { daysLeftInMonth } from './dates'

export type BudgetTone = 'ok' | 'warn' | 'over'

/** С какого процента расхода бюджет считается «на исходе». */
export const WARN_THRESHOLD = 85

export interface BudgetProgress {
  /** Доля потраченного в процентах, может быть больше 100 */
  percent: number
  /** Для ширины бара: не больше 100 */
  fill: number
  tone: BudgetTone
  /** Остаток; отрицательный при перерасходе */
  available: number
}

export function budgetProgress(status: Pick<BudgetStatusDto, 'assigned' | 'spent' | 'available'>): BudgetProgress {
  const percent = status.assigned > 0 ? (status.spent / status.assigned) * 100 : 0
  const tone: BudgetTone = status.available < 0 ? 'over' : percent >= WARN_THRESHOLD ? 'warn' : 'ok'
  return {
    percent,
    fill: Math.min(100, Math.max(0, percent)),
    tone,
    available: status.available,
  }
}

export interface HeroStats {
  spent: number
  /** Общий бюджет месяца; null, если он не задан */
  limit: number | null
  /** Остаток, не меньше нуля */
  remaining: number
  /** Перерасход над общим бюджетом (положительное число) или 0 */
  overspent: number
  percent: number
  fill: number
  /** Сколько можно тратить в день до конца месяца */
  perDay: number
  daysLeft: number
}

/**
 * Цифры карточки «Потрачено в месяце». В день — остаток, делённый на оставшиеся дни месяца
 * (считая сегодняшний). Для прошлых и будущих месяцев «сегодня» не имеет смысла, поэтому дни считаются по today.
 */
export function heroStats(
  totalSpent: number,
  overall: Pick<BudgetStatusDto, 'assigned' | 'available'> | null,
  today: Date,
): HeroStats {
  const daysLeft = daysLeftInMonth(today)
  if (!overall) {
    return { spent: totalSpent, limit: null, remaining: 0, overspent: 0, percent: 0, fill: 0, perDay: 0, daysLeft }
  }
  const percent = overall.assigned > 0 ? (totalSpent / overall.assigned) * 100 : 0
  const remaining = Math.max(overall.available, 0)
  return {
    spent: totalSpent,
    limit: overall.assigned,
    remaining,
    overspent: Math.max(-overall.available, 0),
    percent,
    fill: Math.min(100, Math.max(0, percent)),
    perDay: remaining / daysLeft,
    daysLeft,
  }
}
