export const NBSP = String.fromCharCode(0xa0)

const numberFormat = new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 2 })

/** «3 240,5» — группы разрядов через неразрывный пробел, до двух знаков после запятой. */
export function formatAmount(value: number): string {
  // «−0» и копеечные хвосты после вычитаний не должны попадать на экран
  const safe = Math.abs(value) < 0.005 ? 0 : value
  return numberFormat.format(safe)
}

/** «3 240 ₽» */
export function formatMoney(value: number): string {
  return `${formatAmount(value)}${NBSP}₽`
}

/** Короткая подпись для столбцов графика: 15400 → «15,4к», 1 200 000 → «1,2м». */
export function formatCompact(value: number): string {
  const abs = Math.abs(value)
  if (abs >= 1_000_000) return `${trimZero(value / 1_000_000)}м`
  if (abs >= 1_000) return `${trimZero(value / 1_000)}к`
  return String(Math.round(value))
}

function trimZero(n: number): string {
  return n.toFixed(1).replace('.', ',').replace(/,0$/, '')
}

/**
 * Приводит ввод к виду «123,45»: принимает и запятую, и точку, отбрасывает всё лишнее,
 * оставляет один разделитель и не больше двух знаков после него.
 */
export function sanitizeAmountInput(raw: string): string {
  const cleaned = raw.replace(/\./g, ',').replace(/[^\d,]/g, '')
  const comma = cleaned.indexOf(',')
  if (comma === -1) return stripLeadingZeros(cleaned)
  const integer = stripLeadingZeros(cleaned.slice(0, comma)) || '0'
  const fraction = cleaned.slice(comma + 1).replace(/,/g, '').slice(0, 2)
  return `${integer},${fraction}`
}

function stripLeadingZeros(digits: string): string {
  return digits.replace(/^0+(?=\d)/, '')
}

/** Число из ввода пользователя; null, если это не сумма с копейками (максимум два знака). */
export function parseAmount(text: string): number | null {
  const normalized = text.replace(/\s/g, '').replace(',', '.')
  if (normalized === '' || normalized === '.') return null
  if (!/^\d*\.?\d{0,2}$/.test(normalized)) return null
  const value = Number(normalized)
  return Number.isFinite(value) ? value : null
}

/** Деньги в копейках — целые числа, чтобы сумма частей сходилась без ошибок плавающей точки. */
export function toCents(value: number): number {
  return Math.round(value * 100)
}

export function fromCents(cents: number): number {
  return cents / 100
}

/** Копейки обратно в строку для поля ввода: 1250 → «12,5», 100000 → «1000». */
export function centsToInput(cents: number): string {
  return String(fromCents(cents)).replace('.', ',')
}
