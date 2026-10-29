import type { CategoryDto, TransactionDto } from '../../api/types'
import type { FormState } from './formState'
import { buildRequest, formReducer, initialFormState, isFormValid } from './formState'

const categories: CategoryDto[] = [
  { id: 1, categoryName: 'Еда', userId: 7 },
  { id: 2, categoryName: 'Дом', userId: 7 },
  { id: 3, categoryName: 'Кафе', userId: 7 },
]
const NOW = new Date(2026, 9, 28, 14, 5)
const ids = categories.map((c) => c.id)

function fresh(): FormState {
  return initialFormState(null, categories, NOW)
}

function filled(overrides: Partial<FormState> = {}): FormState {
  return { ...fresh(), amount: '1000', description: 'Супермаркет', ...overrides }
}

describe('initialFormState', () => {
  it('starts a new expense today with the first category', () => {
    const state = fresh()
    expect(state.date).toBe('2026-10-28')
    expect(state.time).toBe('14:05')
    expect(state.splits).toHaveLength(1)
    expect(state.splits[0]?.categoryId).toBe(1)
    expect(state.amount).toBe('')
  })

  it('fills the form from an existing expense', () => {
    const tx: TransactionDto = {
      id: 5,
      userId: 7,
      amount: 1000.5,
      description: 'Ужин',
      dateTime: '2026-10-20T19:30:00',
      splits: [
        { id: 11, transactionId: 5, amount: 700.5, category: categories[0]! },
        { id: 12, transactionId: 5, amount: 300, category: categories[2]! },
      ],
    }
    const state = initialFormState(tx, categories, NOW)
    expect(state).toMatchObject({
      amount: '1000,5',
      description: 'Ужин',
      date: '2026-10-20',
      time: '19:30',
    })
    expect(state.splits.map((s) => [s.categoryId, s.amount])).toEqual([
      [1, '700,5'],
      [3, '300'],
    ])
  })
})

describe('formReducer', () => {
  it('accepts a comma or a dot in the amount', () => {
    expect(formReducer(fresh(), { type: 'amount', value: '12.5' }).amount).toBe('12,5')
  })

  it('splitting hands the whole amount to the first part and adds the next free category', () => {
    const next = formReducer(filled(), { type: 'addSplit', key: 'b', categoryIds: ids })
    expect(next.splits.map((s) => [s.categoryId, s.amount])).toEqual([
      [1, '1000'],
      [2, ''],
    ])
  })

  it('does not add a part when every category is already used', () => {
    let state = filled()
    state = formReducer(state, { type: 'addSplit', key: 'b', categoryIds: ids })
    state = formReducer(state, { type: 'addSplit', key: 'c', categoryIds: ids })
    const same = formReducer(state, { type: 'addSplit', key: 'd', categoryIds: ids })
    expect(same.splits).toHaveLength(3)
    expect(same).toBe(state)
  })

  it('removing down to one part returns to the "whole amount" mode', () => {
    let state = filled()
    state = formReducer(state, { type: 'addSplit', key: 'b', categoryIds: ids })
    state = formReducer(state, { type: 'removeSplit', key: 'b' })
    expect(state.splits).toHaveLength(1)
    expect(state.splits[0]?.amount).toBe('')
  })

  it('never removes the last part', () => {
    const state = filled()
    expect(formReducer(state, { type: 'removeSplit', key: state.splits[0]!.key })).toBe(state)
  })

  it('splits evenly and gives the odd kopecks to the last part', () => {
    let state = filled({ amount: '10' })
    state = formReducer(state, { type: 'addSplit', key: 'b', categoryIds: ids })
    state = formReducer(state, { type: 'addSplit', key: 'c', categoryIds: ids })
    state = formReducer(state, { type: 'evenSplit' })
    expect(state.splits.map((s) => s.amount)).toEqual(['3,33', '3,33', '3,34'])
  })

  it('adds the remainder to the part that is still empty', () => {
    let state = filled()
    state = formReducer(state, { type: 'addSplit', key: 'b', categoryIds: ids })
    state = formReducer(state, { type: 'splitAmount', key: state.splits[0]!.key, value: '600' })
    state = formReducer(state, { type: 'fillRemainder' })
    expect(state.splits.map((s) => s.amount)).toEqual(['600', '400'])
  })

  it('adds the remainder to the last part when all parts have amounts', () => {
    let state = filled()
    state = formReducer(state, { type: 'addSplit', key: 'b', categoryIds: ids })
    state = formReducer(state, { type: 'splitAmount', key: state.splits[0]!.key, value: '600' })
    state = formReducer(state, { type: 'splitAmount', key: 'b', value: '100' })
    state = formReducer(state, { type: 'fillRemainder' })
    expect(state.splits.map((s) => s.amount)).toEqual(['600', '400'])
  })

  it('changes the category of one part only', () => {
    let state = filled()
    state = formReducer(state, { type: 'addSplit', key: 'b', categoryIds: ids })
    state = formReducer(state, { type: 'category', key: 'b', categoryId: 3 })
    expect(state.splits.map((s) => s.categoryId)).toEqual([1, 3])
  })
})

describe('validity and request', () => {
  it('requires amount, category, description and date', () => {
    expect(isFormValid(fresh())).toBe(false)
    expect(isFormValid(filled())).toBe(true)
    expect(isFormValid(filled({ description: '   ' }))).toBe(false)
    expect(isFormValid(filled({ date: '' }))).toBe(false)
    expect(isFormValid(filled({ amount: '0' }))).toBe(false)
  })

  it('builds a request for a single part with the whole amount', () => {
    expect(buildRequest(filled({ amount: '1000,5', description: '  Супермаркет ' }))).toEqual({
      description: 'Супермаркет',
      amount: 1000.5,
      dateTime: '2026-10-28T14:05:00',
      splits: [{ categoryId: 1, amount: 1000.5 }],
    })
  })

  it('builds a request for balanced parts', () => {
    let state = filled()
    state = formReducer(state, { type: 'addSplit', key: 'b', categoryIds: ids })
    state = formReducer(state, { type: 'splitAmount', key: state.splits[0]!.key, value: '700' })
    state = formReducer(state, { type: 'splitAmount', key: 'b', value: '300' })
    expect(buildRequest(state)?.splits).toEqual([
      { categoryId: 1, amount: 700 },
      { categoryId: 2, amount: 300 },
    ])
  })

  it('refuses to build while the parts do not add up', () => {
    let state = filled()
    state = formReducer(state, { type: 'addSplit', key: 'b', categoryIds: ids })
    state = formReducer(state, { type: 'splitAmount', key: state.splits[0]!.key, value: '700' })
    state = formReducer(state, { type: 'splitAmount', key: 'b', value: '200' })
    expect(buildRequest(state)).toBeNull()
  })

  it('uses midnight when the time is empty', () => {
    expect(buildRequest(filled({ time: '' }))?.dateTime).toBe('2026-10-28T00:00:00')
  })
})
