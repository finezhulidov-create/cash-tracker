import { budgetProgress, heroStats } from './budget'

const status = (assigned: number, spent: number) => ({ assigned, spent, available: assigned - spent })

describe('budgetProgress', () => {
  it('is fine below the warning threshold', () => {
    expect(budgetProgress(status(1000, 500))).toMatchObject({ percent: 50, fill: 50, tone: 'ok' })
  })

  it('warns from 85% up to the limit', () => {
    expect(budgetProgress(status(1000, 849)).tone).toBe('ok')
    expect(budgetProgress(status(1000, 850)).tone).toBe('warn')
    expect(budgetProgress(status(1000, 1000)).tone).toBe('warn')
  })

  it('is over when the available amount is negative, and the bar stops at 100%', () => {
    const progress = budgetProgress(status(1000, 1250))
    expect(progress).toMatchObject({ percent: 125, fill: 100, tone: 'over', available: -250 })
  })

  it('does not divide by zero', () => {
    expect(budgetProgress(status(0, 0))).toMatchObject({ percent: 0, fill: 0, tone: 'ok' })
  })
})

describe('heroStats', () => {
  const today = new Date(2026, 9, 28) // 4 дня до конца октября, считая сегодняшний

  it('computes remaining and per-day amounts from the overall budget', () => {
    const stats = heroStats(29_809, { assigned: 55_000, available: 25_191 }, today)
    expect(stats.limit).toBe(55_000)
    expect(stats.remaining).toBe(25_191)
    expect(stats.daysLeft).toBe(4)
    expect(stats.perDay).toBeCloseTo(6297.75)
    expect(Math.round(stats.percent)).toBe(54)
  })

  it('works without an overall budget', () => {
    expect(heroStats(1200, null, today)).toMatchObject({ spent: 1200, limit: null, remaining: 0, perDay: 0 })
  })

  it('reports an overspend and spends nothing per day', () => {
    const stats = heroStats(60_000, { assigned: 55_000, available: -5_000 }, today)
    expect(stats).toMatchObject({ remaining: 0, overspent: 5_000, perDay: 0, fill: 100 })
  })
})
