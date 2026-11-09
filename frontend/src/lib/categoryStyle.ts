import type { LucideIcon } from 'lucide-react'
import { Bus, Coffee, HeartPulse, House, Repeat, ShoppingBasket, Tag } from 'lucide-react'

/**
 * В API у категории нет ни цвета, ни иконки, поэтому они назначаются на клиенте
 * детерминированно: цвет по id, иконка по названию.
 */
export interface CategoryColors {
  /** Начало градиента */
  c1: string
  /** Конец градиента; им же подкрашивается плашка иконки */
  c2: string
}

export const CATEGORY_PALETTE: readonly CategoryColors[] = [
  { c1: '#fb923c', c2: '#ec4899' }, // orange.400 → pink.500
  { c1: '#67e8f9', c2: '#3b82f6' }, // cyan.300 → blue.500
  { c1: '#c084fc', c2: '#2563eb' }, // purple.400 → blue.600
  { c1: '#fde047', c2: '#f97316' }, // yellow.300 → orange.500
  { c1: '#f472b6', c2: '#9333ea' }, // pink.400 → purple.600
  { c1: '#5eead4', c2: '#22c55e' }, // teal.300 → green.500
  { c1: '#a3cfff', c2: '#a855f7' }, // blue.300 → purple.500
  { c1: '#86efac', c2: '#06b6d4' }, // green.300 → cyan.500
  { c1: '#f87171', c2: '#fdba74' }, // red.400 → orange.300
]

export function categoryPaletteIndex(categoryId: number): number {
  const size = CATEGORY_PALETTE.length
  return ((categoryId % size) + size) % size
}

export function categoryColors(categoryId: number): CategoryColors {
  return CATEGORY_PALETTE[categoryPaletteIndex(categoryId)]!
}

/** id SVG-градиента из GradientDefs — им закрашиваются иконки категории. */
export function categoryGradientId(categoryId: number): string {
  return `ct-cat-${categoryPaletteIndex(categoryId)}`
}

export function categoryGradient(categoryId: number): string {
  const { c1, c2 } = categoryColors(categoryId)
  return `linear-gradient(135deg, ${c1}, ${c2})`
}

/** Горизонтальный градиент для заливки полос прогресса. */
export function categoryBarGradient(categoryId: number): string {
  const { c1, c2 } = categoryColors(categoryId)
  return `linear-gradient(90deg, ${c1}, ${c2})`
}

/** Фон плашки под иконкой: конечный цвет категории с 14% непрозрачности. */
export function categoryTint(categoryId: number): string {
  return `${categoryColors(categoryId).c2}24`
}

/** Перерасход: красный градиент вместо цвета категории. */
export const OVERSPENT_GRADIENT = 'linear-gradient(90deg, #f87171, #dc2626)'

const ICON_RULES: readonly { icon: LucideIcon; keywords: readonly string[] }[] = [
  { icon: ShoppingBasket, keywords: ['еда', 'продукт', 'супермаркет', 'food', 'grocer'] },
  { icon: Bus, keywords: ['транспорт', 'такси', 'проезд', 'бензин', 'transport', 'taxi'] },
  { icon: House, keywords: ['квартплата', 'жиль', 'аренд', 'дом', 'home', 'rent'] },
  { icon: Coffee, keywords: ['кафе', 'ресторан', 'кофе', 'cafe', 'coffee'] },
  { icon: Repeat, keywords: ['подписк', 'subscr'] },
  { icon: HeartPulse, keywords: ['здоровье', 'аптек', 'врач', 'медиц', 'health'] },
]

export function categoryIcon(name: string): LucideIcon {
  const normalized = name.trim().toLowerCase().replaceAll('ё', 'е')
  const rule = ICON_RULES.find(({ keywords }) => keywords.some((word) => normalized.includes(word)))
  return rule?.icon ?? Tag
}
