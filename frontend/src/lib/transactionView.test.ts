import type { TransactionDto } from '../api/types'
import { groupByDay, sumAmounts } from './transactionView'

function tx(id: number, dateTime: string, amount: number): TransactionDto {
  return { id, userId: 1, amount, description: `t${id}`, dateTime, splits: [] }
}

describe('groupByDay', () => {
  it('returns nothing for an empty list', () => {
    expect(groupByDay([])).toEqual([])
  })

  it('groups consecutive expenses of the same day and sums them', () => {
    const groups = groupByDay([
      tx(1, '2026-10-28T18:00:00', 100),
      tx(2, '2026-10-28T09:30:00', 50.5),
      tx(3, '2026-10-27T12:00:00', 200),
    ])
    expect(groups.map((g) => [g.date, g.total, g.items.length])).toEqual([
      ['2026-10-28', 150.5, 2],
      ['2026-10-27', 200, 1],
    ])
  })

  it('keeps the incoming order of days', () => {
    const groups = groupByDay([tx(1, '2026-10-01T10:00:00', 1), tx(2, '2026-10-05T10:00:00', 2)])
    expect(groups.map((g) => g.date)).toEqual(['2026-10-01', '2026-10-05'])
  })
})

describe('sumAmounts', () => {
  it('sums amounts', () => {
    expect(sumAmounts([tx(1, '2026-10-28T10:00:00', 10), tx(2, '2026-10-28T11:00:00', 5)])).toBe(15)
    expect(sumAmounts([])).toBe(0)
  })
})
