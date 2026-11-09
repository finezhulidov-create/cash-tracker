import { isPreset, presetRange } from './range'

const today = new Date(2026, 9, 29)

describe('presetRange', () => {
  it('month is the current month', () => {
    expect(presetRange('month', today)).toEqual({ from: '2026-10-01', to: '2026-10-31' })
  })

  it('quarter is the current and two previous months', () => {
    expect(presetRange('quarter', today)).toEqual({ from: '2026-08-01', to: '2026-10-31' })
  })

  it('quarter crosses the year boundary', () => {
    expect(presetRange('quarter', new Date(2026, 0, 15))).toEqual({ from: '2025-11-01', to: '2026-01-31' })
  })

  it('year is the whole calendar year', () => {
    expect(presetRange('year', today)).toEqual({ from: '2026-01-01', to: '2026-12-31' })
  })

  it('knows leap Februaries', () => {
    expect(presetRange('month', new Date(2028, 1, 10)).to).toBe('2028-02-29')
  })
})

describe('isPreset', () => {
  it('accepts only known presets', () => {
    expect(isPreset('month')).toBe(true)
    expect(isPreset('week')).toBe(false)
    expect(isPreset(null)).toBe(false)
  })
})
