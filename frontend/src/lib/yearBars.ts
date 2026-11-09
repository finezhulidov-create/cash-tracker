import type { MonthlySpendingDto } from '../api/types'

export interface YearBar {
  /** 1–12 */
  month: number
  value: number
  /** Высота столбца в процентах от самого высокого */
  heightPct: number
  /** Текущий месяц */
  current: boolean
  /** Будущий месяц без трат — рисуется пустым */
  future: boolean
}

/**
 * 12 столбцов за год. Бэкенд всегда отдаёт 12 записей, но порядок и полноту не гарантируем:
 * недостающие месяцы считаются нулевыми.
 */
export function buildYearBars(data: readonly MonthlySpendingDto[], year: number, today: Date): YearBar[] {
  const byMonth = new Map<number, number>()
  for (const item of data) {
    const [itemYear, itemMonth] = item.month.split('-').map(Number)
    if (itemYear === year && itemMonth) byMonth.set(itemMonth, item.spent)
  }
  const values = Array.from({ length: 12 }, (_, index) => byMonth.get(index + 1) ?? 0)
  const max = Math.max(1, ...values)
  const isCurrentYear = today.getFullYear() === year
  const currentMonth = today.getMonth() + 1

  return values.map((value, index) => {
    const month = index + 1
    const future = isCurrentYear && month > currentMonth && value === 0
    return {
      month,
      value,
      heightPct: future ? 0 : (value / max) * 100,
      current: isCurrentYear && month === currentMonth,
      future,
    }
  })
}

export interface YearStats {
  total: number
  /** Среднее по месяцам, в которых были траты */
  average: number
  /** Самый затратный месяц (1–12) или null, если трат не было */
  peakMonth: number | null
  peakValue: number
}

export function yearStats(bars: readonly YearBar[]): YearStats {
  const total = bars.reduce((sum, bar) => sum + bar.value, 0)
  const nonZero = bars.filter((bar) => bar.value > 0)
  const peak = nonZero.reduce<YearBar | null>((best, bar) => (best && best.value >= bar.value ? best : bar), null)
  return {
    total,
    average: nonZero.length ? total / nonZero.length : 0,
    peakMonth: peak?.month ?? null,
    peakValue: peak?.value ?? 0,
  }
}

const HUES = ['pink', 'purple', 'blue', 'cyan', 'teal', 'green', 'yellow', 'orange', 'pink', 'purple', 'blue', 'cyan'] as const
type Hue = (typeof HUES)[number]

const HUE_500: Record<Hue, string> = {
  pink: '#ec4899',
  purple: '#a855f7',
  blue: '#3b82f6',
  cyan: '#06b6d4',
  teal: '#14b8a6',
  green: '#22c55e',
  yellow: '#eab308',
  orange: '#f97316',
}
const HUE_300: Record<Hue, string> = {
  pink: '#f9a8d4',
  purple: '#d8b4fe',
  blue: '#a3cfff',
  cyan: '#67e8f9',
  teal: '#5eead4',
  green: '#86efac',
  yellow: '#fde047',
  orange: '#fdba74',
}

/** Градиент прошедшего месяца: оттенок месяца (500) → оттенок через два месяца (300). */
export function pastBarGradient(month: number): string {
  const index = month - 1
  const from = HUES[index % 12]!
  const to = HUES[(index + 2) % 12]!
  return `linear-gradient(0deg, ${HUE_500[from]}, ${HUE_300[to]})`
}

/** Текущий месяц — полный iridescent-градиент снизу вверх. */
export const CURRENT_BAR_GRADIENT =
  'linear-gradient(0deg, #f472b6, #c084fc 35%, #67e8f9 70%, #fef08a)'
