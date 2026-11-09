import type { DraftSplit } from './splits'
import {
  computeSplitStatus,
  evenSplit,
  isSplitStatusValid,
  nextUnusedCategory,
  totalCents,
} from './splits'

const part = (categoryId: number | null, amount = '', key = String(categoryId)): DraftSplit => ({
  key,
  categoryId,
  amount,
})

describe('evenSplit', () => {
  it('splits evenly when divisible', () => {
    expect(evenSplit(1000, 4)).toEqual([250, 250, 250, 250])
  })

  it('gives leftover cents to the last part', () => {
    expect(evenSplit(1000, 3)).toEqual([333, 333, 334])
    expect(evenSplit(1, 3)).toEqual([0, 0, 1])
  })

  it('always sums back to the total', () => {
    for (const [total, n] of [[9999, 7], [1, 2], [100_000, 9]] as const) {
      expect(evenSplit(total, n).reduce((a, b) => a + b, 0)).toBe(total)
    }
  })

  it('returns nothing for zero parts', () => {
    expect(evenSplit(100, 0)).toEqual([])
  })
})

describe('totalCents', () => {
  it('reads a comma amount in cents', () => {
    expect(totalCents('1000,5')).toBe(100_050)
  })

  it('treats empty or broken input as zero', () => {
    expect(totalCents('')).toBe(0)
    expect(totalCents('abc')).toBe(0)
  })
})

describe('computeSplitStatus', () => {
  it('asks for an amount first', () => {
    expect(computeSplitStatus('', [part(1)]).kind).toBe('no-amount')
    expect(computeSplitStatus('0', [part(1)]).kind).toBe('no-amount')
  })

  it('accepts a single part with a category — the whole amount goes there', () => {
    expect(computeSplitStatus('500', [part(7)])).toEqual({ kind: 'single', categoryId: 7 })
  })

  it('requires a category', () => {
    expect(computeSplitStatus('500', [part(null)]).kind).toBe('no-category')
    expect(computeSplitStatus('500', [part(1, '300'), part(null, '200')]).kind).toBe('no-category')
  })

  it('reports what is left to distribute', () => {
    expect(computeSplitStatus('1000', [part(1, '700'), part(2, '200')])).toEqual({
      kind: 'remaining',
      cents: 10_000,
    })
  })

  it('reports an excess', () => {
    expect(computeSplitStatus('1000', [part(1, '700'), part(2, '400')])).toEqual({
      kind: 'over',
      cents: 10_000,
    })
  })

  it('is balanced when the parts add up to the total exactly', () => {
    expect(computeSplitStatus('1000', [part(1, '700'), part(2, '300')]).kind).toBe('balanced')
    expect(computeSplitStatus('0,3', [part(1, '0,1'), part(2, '0,2')]).kind).toBe('balanced')
  })

  it('does not allow empty parts and tells how much is still unassigned', () => {
    expect(computeSplitStatus('1000', [part(1, '1000'), part(2, '')])).toEqual({
      kind: 'zero-part',
      remainingCents: 0,
    })
    expect(computeSplitStatus('1000', [part(1, '300'), part(2, '')])).toEqual({
      kind: 'zero-part',
      remainingCents: 70_000,
    })
  })

  it('marks only the finished states as valid', () => {
    expect(isSplitStatusValid({ kind: 'single', categoryId: 1 })).toBe(true)
    expect(isSplitStatusValid({ kind: 'balanced' })).toBe(true)
    expect(isSplitStatusValid({ kind: 'remaining', cents: 1 })).toBe(false)
    expect(isSplitStatusValid({ kind: 'no-amount' })).toBe(false)
  })
})

describe('nextUnusedCategory', () => {
  it('picks the first category that is not used yet', () => {
    expect(nextUnusedCategory([1, 2, 3], [part(1), part(2)])).toBe(3)
    expect(nextUnusedCategory([1, 2, 3], [part(2)])).toBe(1)
  })

  it('returns null when everything is used', () => {
    expect(nextUnusedCategory([1], [part(1)])).toBeNull()
  })
})
