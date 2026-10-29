export type PluralForms = readonly [one: string, few: string, many: string]

/** Русские склонения: 1 трата, 2 траты, 5 трат, 11 трат, 21 трата. */
export function plural(n: number, forms: PluralForms): string {
  const abs = Math.abs(n) % 100
  const last = abs % 10
  if (abs > 10 && abs < 20) return forms[2]
  if (last > 1 && last < 5) return forms[1]
  if (last === 1) return forms[0]
  return forms[2]
}

/** «12 трат» — число с неразрывным пробелом в разрядах и правильной формой слова. */
export function countLabel(n: number, forms: PluralForms): string {
  return `${n.toLocaleString('ru-RU')} ${plural(n, forms)}`
}

export const FORMS = {
  transaction: ['трата', 'траты', 'трат'],
  operation: ['операция', 'операции', 'операций'],
  part: ['часть', 'части', 'частей'],
  /** «1 часть траты», «2 части трат», «5 частей трат» — счётчик у категории */
  splitPart: ['часть траты', 'части трат', 'частей трат'],
  day: ['день', 'дня', 'дней'],
  category: ['категория', 'категории', 'категорий'],
  month: ['месяц', 'месяца', 'месяцев'],
} as const satisfies Record<string, PluralForms>
