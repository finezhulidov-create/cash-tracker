import type { MonthlySpendingDto } from '../api/types'
import { buildYearBars, pastBarGradient, yearStats } from './yearBars'

function year(values: Record<number, number>, y = 2026): MonthlySpendingDto[] {
  return Array.from({ length: 12 }, (_, i) => ({
    month: `${y}-${String(i + 1).padStart(2, '0')}`,
    spent: values[i + 1] ?? 0,
  }))
}

describe('buildYearBars', () => {
  const today = new Date(2026, 9, 29) // октябрь 2026

  it('always returns 12 bars scaled to the highest month', () => {
    const bars = buildYearBars(year({ 8: 100, 9: 200, 10: 400 }), 2026, today)
    expect(bars).toHaveLength(12)
    expect(bars[9]?.heightPct).toBe(100)
    expect(bars[8]?.heightPct).toBe(50)
    expect(bars[7]?.heightPct).toBe(25)
  })

  it('marks the current month and empty future months', () => {
    const bars = buildYearBars(year({ 10: 400 }), 2026, today)
    expect(bars[9]).toMatchObject({ current: true, future: false })
    expect(bars[10]).toMatchObject({ current: false, future: true, heightPct: 0 })
    expect(bars[11]?.future).toBe(true)
  })

  it('treats months of another year as ordinary past months', () => {
    const bars = buildYearBars(year({ 3: 50 }, 2025), 2025, today)
    expect(bars.some((bar) => bar.current || bar.future)).toBe(false)
  })

  it('survives missing months', () => {
    const bars = buildYearBars([{ month: '2026-02', spent: 10 }], 2026, today)
    expect(bars).toHaveLength(12)
    expect(bars[1]?.value).toBe(10)
    expect(bars[0]?.value).toBe(0)
  })

  it('does not divide by zero when there is no spending', () => {
    const bars = buildYearBars(year({}), 2026, today)
    expect(bars.every((bar) => bar.heightPct === 0)).toBe(true)
  })
})

describe('yearStats', () => {
  it('sums, averages over months with spending and finds the peak', () => {
    const bars = buildYearBars(year({ 1: 100, 2: 300, 3: 200 }), 2026, new Date(2026, 5, 1))
    expect(yearStats(bars)).toEqual({ total: 600, average: 200, peakMonth: 2, peakValue: 300 })
  })

  it('is empty without spending', () => {
    expect(yearStats(buildYearBars(year({}), 2026, new Date(2026, 5, 1)))).toEqual({
      total: 0,
      average: 0,
      peakMonth: null,
      peakValue: 0,
    })
  })

  it('takes the first month on a tie', () => {
    const bars = buildYearBars(year({ 4: 500, 7: 500 }), 2026, new Date(2026, 11, 1))
    expect(yearStats(bars).peakMonth).toBe(4)
  })
})

describe('pastBarGradient', () => {
  it('uses the month hue and the hue two months ahead', () => {
    expect(pastBarGradient(1)).toBe('linear-gradient(0deg, #ec4899, #a3cfff)')
  })
})
