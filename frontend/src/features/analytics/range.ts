import { addMonths, lastDayOfMonth, yearMonthOf } from '../../lib/dates'

export type Preset = 'month' | 'quarter' | 'year'

export const PRESETS: readonly { value: Preset; label: string }[] = [
  { value: 'month', label: 'Месяц' },
  { value: 'quarter', label: '3 месяца' },
  { value: 'year', label: 'Год' },
]

export function isPreset(value: string | null | undefined): value is Preset {
  return value === 'month' || value === 'quarter' || value === 'year'
}

/** Минимальный год, который принимает бэкенд (`@Min(2000)` на параметре year). */
export const MIN_YEAR = 2000

/**
 * Период для «Траты по категориям» (обе даты включительно):
 * месяц — текущий; 3 месяца — текущий и два предыдущих; год — текущий календарный год.
 */
export function presetRange(preset: Preset, today: Date): { from: string; to: string } {
  const current = yearMonthOf(today)
  if (preset === 'year') {
    return { from: `${today.getFullYear()}-01-01`, to: `${today.getFullYear()}-12-31` }
  }
  const first = preset === 'quarter' ? addMonths(current, -2) : current
  return { from: `${first}-01`, to: `${current}-${String(lastDayOfMonth(current)).padStart(2, '0')}` }
}
