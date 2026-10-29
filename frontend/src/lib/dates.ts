/**
 * Все даты здесь «настенные»: бэкенд хранит LocalDateTime без часового пояса,
 * поэтому строки разбираются вручную и не проходят через UTC.
 */

const MONTHS_NOM = [
  'январь', 'февраль', 'март', 'апрель', 'май', 'июнь',
  'июль', 'август', 'сентябрь', 'октябрь', 'ноябрь', 'декабрь',
] as const
const MONTHS_GEN = [
  'января', 'февраля', 'марта', 'апреля', 'мая', 'июня',
  'июля', 'августа', 'сентября', 'октября', 'ноября', 'декабря',
] as const
const MONTHS_PREP = [
  'январе', 'феврале', 'марте', 'апреле', 'мае', 'июне',
  'июле', 'августе', 'сентябре', 'октябре', 'ноябре', 'декабре',
] as const
const MONTHS_SHORT = [
  'янв', 'фев', 'мар', 'апр', 'май', 'июн', 'июл', 'авг', 'сен', 'окт', 'ноя', 'дек',
] as const
const WEEKDAYS = [
  'воскресенье', 'понедельник', 'вторник', 'среда', 'четверг', 'пятница', 'суббота',
] as const

const pad2 = (n: number) => String(n).padStart(2, '0')

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1)
}

/** «2026-10-28» в локальном часовом поясе. */
export function toIsoDate(d: Date): string {
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
}

/** «14:30» в локальном часовом поясе. */
export function toTimeString(d: Date): string {
  return `${pad2(d.getHours())}:${pad2(d.getMinutes())}`
}

/** «2026-10-28» → полночь этого дня по местному времени. */
export function parseIsoDate(iso: string): Date {
  const [y = 0, m = 1, d = 1] = iso.slice(0, 10).split('-').map(Number)
  return new Date(y, m - 1, d)
}

/** «2026-10-28T14:30:00[.123456]» → Date по местному времени. */
export function parseLocalDateTime(iso: string): Date {
  const [datePart = '', timePart = '00:00:00'] = iso.split('T')
  const [y = 0, mo = 1, d = 1] = datePart.split('-').map(Number)
  const [h = 0, mi = 0, s = 0] = timePart.split(':').map((part) => Number.parseFloat(part))
  return new Date(y, mo - 1, d, h, mi, Math.floor(s))
}

/** Дата и время из полей формы → LocalDateTime без часового пояса для API. */
export function toLocalDateTime(date: string, time: string): string {
  return `${date}T${time || '00:00'}:00`
}

export function splitLocalDateTime(iso: string): { date: string; time: string } {
  const d = parseLocalDateTime(iso)
  return { date: toIsoDate(d), time: toTimeString(d) }
}

// ---------- месяцы вида «2026-10» ----------

export function yearMonthOf(d: Date): string {
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}`
}

export function parseYearMonth(ym: string): { year: number; month: number } {
  const [year = 0, month = 1] = ym.split('-').map(Number)
  return { year, month }
}

export function addMonths(ym: string, delta: number): string {
  const { year, month } = parseYearMonth(ym)
  return yearMonthOf(new Date(year, month - 1 + delta, 1))
}

/** «Октябрь 2026» */
export function monthTitle(ym: string): string {
  const { year, month } = parseYearMonth(ym)
  return `${capitalize(MONTHS_NOM[month - 1] ?? '')} ${year}`
}

/** «октябрь» */
export function monthName(month: number): string {
  return MONTHS_NOM[month - 1] ?? ''
}

/** «в октябре» — предложный падеж. */
export function monthPrepositional(ym: string): string {
  return MONTHS_PREP[parseYearMonth(ym).month - 1] ?? ''
}

/** «окт» — для подписей под столбцами. */
export function monthShort(month: number): string {
  return MONTHS_SHORT[month - 1] ?? ''
}

export function lastDayOfMonth(ym: string): number {
  const { year, month } = parseYearMonth(ym)
  return new Date(year, month, 0).getDate()
}

/** Границы месяца для аналитики (даты включительно). */
export function monthDateRange(ym: string): { from: string; to: string } {
  return { from: `${ym}-01`, to: `${ym}-${pad2(lastDayOfMonth(ym))}` }
}

/** Границы месяца для фильтра трат (LocalDateTime включительно). */
export function monthDateTimeRange(ym: string): { from: string; to: string } {
  return { from: `${ym}-01T00:00:00`, to: `${ym}-${pad2(lastDayOfMonth(ym))}T23:59:59` }
}

/** Сколько дней осталось в месяце, считая сегодняшний. */
export function daysLeftInMonth(today: Date): number {
  return lastDayOfMonth(yearMonthOf(today)) - today.getDate() + 1
}

// ---------- подписи ----------

/** «Сегодня, 28 октября» / «Вчера, 27 октября» / «24 октября, суббота». */
export function formatDayHeading(iso: string, today: Date): string {
  const day = parseIsoDate(iso)
  const base = `${day.getDate()} ${MONTHS_GEN[day.getMonth()]}`
  const withYear = day.getFullYear() === today.getFullYear() ? base : `${base} ${day.getFullYear()}`
  const diffDays = Math.round((startOfDay(today).getTime() - day.getTime()) / 86_400_000)
  if (diffDays === 0) return `Сегодня, ${base}`
  if (diffDays === 1) return `Вчера, ${base}`
  return `${withYear}, ${WEEKDAYS[day.getDay()]}`
}

function startOfDay(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate())
}

/** «28 окт» — дата справа в строке операции. */
export function formatShortDate(dateTime: string): string {
  const d = parseLocalDateTime(dateTime)
  return `${d.getDate()} ${MONTHS_SHORT[d.getMonth()]}`
}

export function formatTime(dateTime: string): string {
  return toTimeString(parseLocalDateTime(dateTime))
}

/** «1 — 31 октября 2026» или «1 августа — 31 октября 2026». */
export function formatPeriod(fromIso: string, toIso: string): string {
  const from = parseIsoDate(fromIso)
  const to = parseIsoDate(toIso)
  const sameMonth = from.getMonth() === to.getMonth() && from.getFullYear() === to.getFullYear()
  const left = sameMonth ? `${from.getDate()}` : `${from.getDate()} ${MONTHS_GEN[from.getMonth()]}`
  const leftWithYear =
    from.getFullYear() === to.getFullYear() ? left : `${left} ${from.getFullYear()}`
  return `${leftWithYear} — ${to.getDate()} ${MONTHS_GEN[to.getMonth()]} ${to.getFullYear()}`
}
