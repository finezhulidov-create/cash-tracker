import { Bus, Coffee, HeartPulse, House, Repeat, ShoppingBasket, Tag } from 'lucide-react'
import { categoryColors, categoryGradient, categoryIcon, categoryTint } from './categoryStyle'

describe('categoryColors', () => {
  it('is deterministic and cycles every nine ids', () => {
    expect(categoryColors(3)).toEqual(categoryColors(12))
    expect(categoryColors(0)).not.toEqual(categoryColors(1))
  })

  it('does not break on odd ids', () => {
    expect(categoryColors(0)).toBeDefined()
    expect(categoryColors(-4)).toBeDefined()
  })

  it('builds a gradient and a 14% tint from the end colour', () => {
    expect(categoryGradient(0)).toBe('linear-gradient(135deg, #fb923c, #ec4899)')
    expect(categoryTint(0)).toBe('#ec489924')
  })
})

describe('categoryIcon', () => {
  it.each([
    ['Еда', ShoppingBasket],
    ['Транспорт', Bus],
    ['Квартплата', House],
    ['Кафе', Coffee],
    ['Подписки', Repeat],
    ['Здоровье', HeartPulse],
    ['Food', ShoppingBasket],
    ['  ЕДА  ', ShoppingBasket],
  ])('%s', (name, icon) => {
    expect(categoryIcon(name)).toBe(icon)
  })

  it('falls back to a tag', () => {
    expect(categoryIcon('Подарки')).toBe(Tag)
    expect(categoryIcon('')).toBe(Tag)
  })
})
