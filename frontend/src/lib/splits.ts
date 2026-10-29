import { parseAmount, toCents } from './money'

/** Часть траты в форме: сумма хранится строкой, как её ввёл пользователь. */
export interface DraftSplit {
  /** Устойчивый ключ строки для React */
  key: string
  categoryId: number | null
  amount: string
}

export type SplitStatus =
  | { kind: 'no-amount' }
  | { kind: 'no-category' }
  | { kind: 'single'; categoryId: number }
  /** Есть часть без суммы; remainingCents — сколько ещё не распределено (может быть ≤ 0) */
  | { kind: 'zero-part'; remainingCents: number }
  | { kind: 'remaining'; cents: number }
  | { kind: 'over'; cents: number }
  | { kind: 'balanced' }

/** Можно ли отправлять разбивку на сервер. */
export function isSplitStatusValid(status: SplitStatus): boolean {
  return status.kind === 'single' || status.kind === 'balanced'
}

/** Сумма траты в копейках; 0, если поле пустое или некорректное. */
export function totalCents(amountText: string): number {
  const value = parseAmount(amountText)
  return value === null ? 0 : toCents(value)
}

/**
 * Делит сумму на n частей поровну: копейки, которые не делятся нацело, уходят в последнюю часть.
 */
export function evenSplit(total: number, n: number): number[] {
  if (n <= 0) return []
  const base = Math.floor(total / n)
  const parts = Array<number>(n).fill(base)
  parts[n - 1] = total - base * (n - 1)
  return parts
}

/** Состояние разбивки, которое показывается под блоком «Разбивка по категориям». */
export function computeSplitStatus(amountText: string, splits: readonly DraftSplit[]): SplitStatus {
  const total = totalCents(amountText)
  if (total <= 0) return { kind: 'no-amount' }

  if (splits.length === 1) {
    const only = splits[0]!
    return only.categoryId === null
      ? { kind: 'no-category' }
      : { kind: 'single', categoryId: only.categoryId }
  }

  if (splits.some((split) => split.categoryId === null)) return { kind: 'no-category' }

  const cents = splits.map((split) => totalCents(split.amount))
  const diff = total - cents.reduce((sum, c) => sum + c, 0)
  if (cents.some((c) => c <= 0)) return { kind: 'zero-part', remainingCents: diff }

  if (diff > 0) return { kind: 'remaining', cents: diff }
  if (diff < 0) return { kind: 'over', cents: -diff }
  return { kind: 'balanced' }
}

/** Следующая категория, которой ещё нет среди частей; null, если все заняты. */
export function nextUnusedCategory(
  categoryIds: readonly number[],
  splits: readonly DraftSplit[],
): number | null {
  const used = new Set(splits.map((split) => split.categoryId))
  return categoryIds.find((id) => !used.has(id)) ?? null
}
