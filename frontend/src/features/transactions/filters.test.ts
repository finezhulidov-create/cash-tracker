import {
  countActiveFilters,
  EMPTY_FILTERS,
  hasAnyFilter,
  parseFilters,
  toApiFilters,
  validateFilters,
} from './filters'

describe('parseFilters', () => {
  it('reads every field from the query string, empty by default', () => {
    expect(parseFilters(new URLSearchParams(''))).toEqual(EMPTY_FILTERS)
    expect(parseFilters(new URLSearchParams('q=кофе&cat=3&min=10&max=99,5&from=2026-10-01&to=2026-10-31'))).toEqual({
      q: 'кофе',
      cat: '3',
      min: '10',
      max: '99,5',
      from: '2026-10-01',
      to: '2026-10-31',
    })
  })
})

describe('countActiveFilters', () => {
  it('does not count the search box', () => {
    expect(countActiveFilters({ ...EMPTY_FILTERS, q: 'кофе' })).toBe(0)
  })

  it('counts every filled filter', () => {
    expect(countActiveFilters({ ...EMPTY_FILTERS, cat: '1', min: '5', to: '2026-10-31' })).toBe(3)
  })

  it('hasAnyFilter includes the search', () => {
    expect(hasAnyFilter(EMPTY_FILTERS)).toBe(false)
    expect(hasAnyFilter({ ...EMPTY_FILTERS, q: '  ' })).toBe(false)
    expect(hasAnyFilter({ ...EMPTY_FILTERS, q: 'кофе' })).toBe(true)
    expect(hasAnyFilter({ ...EMPTY_FILTERS, cat: '2' })).toBe(true)
  })
})

describe('validateFilters', () => {
  it('accepts empty and consistent filters', () => {
    expect(validateFilters(EMPTY_FILTERS)).toBeNull()
    expect(validateFilters({ ...EMPTY_FILTERS, from: '2026-10-01', to: '2026-10-01' })).toBeNull()
    expect(validateFilters({ ...EMPTY_FILTERS, min: '10', max: '10' })).toBeNull()
    expect(validateFilters({ ...EMPTY_FILTERS, min: '10' })).toBeNull()
  })

  it('rejects an inverted date range', () => {
    expect(validateFilters({ ...EMPTY_FILTERS, from: '2026-10-31', to: '2026-10-01' })).toMatch(/Дата/)
  })

  it('rejects an inverted amount range', () => {
    expect(validateFilters({ ...EMPTY_FILTERS, min: '100,5', max: '99' })).toMatch(/Сумма/)
  })
})

describe('toApiFilters', () => {
  it('drops empty values', () => {
    expect(toApiFilters(EMPTY_FILTERS)).toEqual({
      q: undefined,
      categoryId: undefined,
      minAmount: undefined,
      maxAmount: undefined,
      from: undefined,
      to: undefined,
    })
  })

  it('converts the form values for the API', () => {
    expect(
      toApiFilters({ q: '  кофе ', cat: '7', min: '10,5', max: '1000', from: '2026-10-01', to: '2026-10-31' }),
    ).toEqual({
      q: 'кофе',
      categoryId: 7,
      minAmount: 10.5,
      maxAmount: 1000,
      from: '2026-10-01T00:00:00',
      to: '2026-10-31T23:59:59.999999',
    })
  })

  it('ignores a garbage category id and unparsable amounts', () => {
    const api = toApiFilters({ ...EMPTY_FILTERS, cat: 'abc', min: 'x' })
    expect(api.categoryId).toBeUndefined()
    expect(api.minAmount).toBeUndefined()
  })
})
