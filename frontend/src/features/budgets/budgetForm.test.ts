import type { CategoryDto } from '../../api/types'
import { unbudgetedCategories } from './budgetForm'

const cats: CategoryDto[] = [
  { id: 1, categoryName: 'Еда', userId: 1 },
  { id: 2, categoryName: 'Дом', userId: 1 },
  { id: 3, categoryName: 'Кафе', userId: 1 },
]

describe('unbudgetedCategories', () => {
  it('returns every category when there are no budgets', () => {
    expect(unbudgetedCategories(cats, [])).toEqual(cats)
  })

  it('leaves out categories that already have a budget', () => {
    expect(unbudgetedCategories(cats, [{ category: cats[0]! }, { category: cats[2]! }])).toEqual([cats[1]])
  })

  it('ignores the overall budget (no category)', () => {
    expect(unbudgetedCategories(cats, [{ category: null }])).toEqual(cats)
  })
})
