import { NBSP } from '../../lib/money'
import { describeSplitStatus } from './splitStatusView'

const name = (id: number) => (id === 1 ? 'Еда' : 'Дом')

describe('describeSplitStatus', () => {
  it('asks for an amount first', () => {
    expect(describeSplitStatus({ kind: 'no-amount' }, name, true).text).toBe('Введите сумму траты')
  })

  it('confirms where the whole amount goes', () => {
    const view = describeSplitStatus({ kind: 'single', categoryId: 1 }, name, true)
    expect(view.text).toBe('Вся сумма — в категории «Еда»')
    expect(view.color).toBe('fg.success')
  })

  it('nudges for a description once the split itself is fine', () => {
    const view = describeSplitStatus({ kind: 'balanced' }, name, false)
    expect(view.text).toBe('Добавьте описание траты')
    expect(view.color).toBe('fg.muted')
  })

  it('offers to fill the remainder when something is left', () => {
    const view = describeSplitStatus({ kind: 'remaining', cents: 30_000 }, name, true)
    expect(view.text).toBe(`Осталось распределить 300${NBSP}₽`)
    expect(view.canFill).toBe(true)
  })

  it('does not offer to fill when the parts are over the total', () => {
    const view = describeSplitStatus({ kind: 'over', cents: 5_000 }, name, true)
    expect(view.text).toBe(`Части больше суммы на 50${NBSP}₽`)
    expect(view.color).toBe('fg.error')
    expect(view.canFill).toBe(false)
  })

  it('explains an empty part, with and without a remainder', () => {
    expect(describeSplitStatus({ kind: 'zero-part', remainingCents: 70_000 }, name, true)).toMatchObject({
      text: `Укажите суммы частей · осталось 700${NBSP}₽`,
      canFill: true,
    })
    expect(describeSplitStatus({ kind: 'zero-part', remainingCents: 0 }, name, true)).toMatchObject({
      text: 'Укажите сумму для каждой части',
      canFill: false,
    })
  })
})
