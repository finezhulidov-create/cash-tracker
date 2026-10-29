import {
  addMonths,
  daysLeftInMonth,
  formatDayHeading,
  formatPeriod,
  formatShortDate,
  formatTime,
  lastDayOfMonth,
  monthDateRange,
  monthDateTimeRange,
  monthTitle,
  parseLocalDateTime,
  splitLocalDateTime,
  toIsoDate,
  toLocalDateTime,
  yearMonthOf,
} from './dates'

describe('local date-time', () => {
  it('builds LocalDateTime without a time zone', () => {
    expect(toLocalDateTime('2026-10-28', '14:30')).toBe('2026-10-28T14:30:00')
    expect(toLocalDateTime('2026-10-28', '')).toBe('2026-10-28T00:00:00')
  })

  it('parses the server value as wall-clock time, ignoring fractional seconds', () => {
    const d = parseLocalDateTime('2026-10-28T14:30:15.123456')
    expect([d.getFullYear(), d.getMonth(), d.getDate(), d.getHours(), d.getMinutes()]).toEqual([
      2026, 9, 28, 14, 30,
    ])
    expect(d.getSeconds()).toBe(15)
  })

  it('round-trips date and time parts', () => {
    expect(splitLocalDateTime('2026-01-05T09:07:00')).toEqual({ date: '2026-01-05', time: '09:07' })
    expect(formatTime('2026-01-05T09:07:00')).toBe('09:07')
  })

  it('formats the short date', () => {
    expect(formatShortDate('2026-10-28T14:30:00')).toBe('28 окт')
  })
})

describe('months', () => {
  it('shifts across year boundaries', () => {
    expect(addMonths('2026-12', 1)).toBe('2027-01')
    expect(addMonths('2026-01', -1)).toBe('2025-12')
    expect(addMonths('2026-10', 0)).toBe('2026-10')
  })

  it('titles the month in Russian', () => {
    expect(monthTitle('2026-10')).toBe('Октябрь 2026')
    expect(monthTitle('2027-03')).toBe('Март 2027')
  })

  it('knows the last day, including leap years', () => {
    expect(lastDayOfMonth('2026-10')).toBe(31)
    expect(lastDayOfMonth('2026-02')).toBe(28)
    expect(lastDayOfMonth('2028-02')).toBe(29)
  })

  it('returns inclusive ranges', () => {
    expect(monthDateRange('2026-02')).toEqual({ from: '2026-02-01', to: '2026-02-28' })
    expect(monthDateTimeRange('2026-10')).toEqual({
      from: '2026-10-01T00:00:00',
      to: '2026-10-31T23:59:59',
    })
  })

  it('counts days left including today', () => {
    expect(daysLeftInMonth(new Date(2026, 9, 28))).toBe(4)
    expect(daysLeftInMonth(new Date(2026, 9, 31))).toBe(1)
    expect(daysLeftInMonth(new Date(2026, 9, 1))).toBe(31)
  })

  it('gets the year-month of a date', () => {
    expect(yearMonthOf(new Date(2026, 0, 31))).toBe('2026-01')
    expect(toIsoDate(new Date(2026, 0, 5))).toBe('2026-01-05')
  })
})

describe('formatDayHeading', () => {
  const today = new Date(2026, 9, 28, 12, 0)

  it('names today and yesterday', () => {
    expect(formatDayHeading('2026-10-28', today)).toBe('Сегодня, 28 октября')
    expect(formatDayHeading('2026-10-27', today)).toBe('Вчера, 27 октября')
  })

  it('adds the weekday for older days', () => {
    expect(formatDayHeading('2026-10-24', today)).toBe('24 октября, суббота')
  })

  it('adds the year for other years', () => {
    expect(formatDayHeading('2025-12-31', today)).toBe('31 декабря 2025, среда')
  })
})

describe('formatPeriod', () => {
  it('collapses a single month', () => {
    expect(formatPeriod('2026-10-01', '2026-10-31')).toBe('1 — 31 октября 2026')
  })

  it('shows both months for longer ranges', () => {
    expect(formatPeriod('2026-08-01', '2026-10-31')).toBe('1 августа — 31 октября 2026')
  })

  it('shows both years when the range crosses a year', () => {
    expect(formatPeriod('2025-11-01', '2026-01-31')).toBe('1 ноября 2025 — 31 января 2026')
  })
})
