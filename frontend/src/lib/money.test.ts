import {
  centsToInput,
  formatAmount,
  formatCompact,
  formatMoney,
  fromCents,
  NBSP,
  parseAmount,
  sanitizeAmountInput,
  toCents,
} from './money'

describe('formatMoney', () => {
  it('groups digits with non-breaking spaces and appends the ruble sign', () => {
    expect(formatMoney(3240)).toBe(`3${NBSP}240${NBSP}₽`)
    expect(formatMoney(1_234_567.5)).toBe(`1${NBSP}234${NBSP}567,5${NBSP}₽`)
  })

  it('shows at most two decimals', () => {
    expect(formatAmount(10.456)).toBe('10,46')
    expect(formatAmount(10)).toBe('10')
  })

  it('never renders negative zero', () => {
    expect(formatAmount(-0)).toBe('0')
    expect(formatAmount(-0.001)).toBe('0')
  })

  it('keeps the minus for real overspending', () => {
    expect(formatAmount(-1500)).toMatch(/1.500$/)
    expect(formatAmount(-1500).charAt(0)).not.toBe('1')
  })
})

describe('formatCompact', () => {
  it.each([
    [0, '0'],
    [999, '999'],
    [1000, '1к'],
    [15_400, '15,4к'],
    [15_049, '15к'],
    [1_200_000, '1,2м'],
  ])('%d → %s', (value, expected) => {
    expect(formatCompact(value)).toBe(expected)
  })
})

describe('sanitizeAmountInput', () => {
  it.each([
    ['12.5', '12,5'],
    ['12,5', '12,5'],
    ['1 000', '1000'],
    ['abc', ''],
    ['1,2,3', '1,23'],
    ['10,999', '10,99'],
    ['007', '7'],
    ['0', '0'],
    [',5', '0,5'],
    ['', ''],
  ])('%j → %j', (raw, expected) => {
    expect(sanitizeAmountInput(raw)).toBe(expected)
  })
})

describe('parseAmount', () => {
  it('accepts comma and dot as the decimal separator', () => {
    expect(parseAmount('12,5')).toBe(12.5)
    expect(parseAmount('12.5')).toBe(12.5)
    expect(parseAmount('1 000')).toBe(1000)
    expect(parseAmount('5,')).toBe(5)
  })

  it('rejects empty and malformed values', () => {
    expect(parseAmount('')).toBeNull()
    expect(parseAmount(',')).toBeNull()
    expect(parseAmount('abc')).toBeNull()
    expect(parseAmount('1,234')).toBeNull()
    expect(parseAmount('-5')).toBeNull()
  })
})

describe('cents', () => {
  it('converts without floating point drift', () => {
    expect(toCents(0.1 + 0.2)).toBe(30)
    expect(toCents(19.99)).toBe(1999)
    expect(fromCents(1999)).toBe(19.99)
  })

  it('turns cents back into an input string', () => {
    expect(centsToInput(1250)).toBe('12,5')
    expect(centsToInput(100_000)).toBe('1000')
    expect(centsToInput(7)).toBe('0,07')
  })
})
