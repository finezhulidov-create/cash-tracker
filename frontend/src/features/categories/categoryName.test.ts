import type { CategoryDto } from '../../api/types'
import { findDuplicate } from './categoryName'

const categories: CategoryDto[] = [
  { id: 1, categoryName: 'Еда', userId: 1 },
  { id: 2, categoryName: 'Кафе ', userId: 1 },
]

describe('findDuplicate', () => {
  it('ignores case and surrounding spaces', () => {
    expect(findDuplicate(categories, ' еда ')?.id).toBe(1)
    expect(findDuplicate(categories, 'КАФЕ')?.id).toBe(2)
  })

  it('finds nothing for a new name', () => {
    expect(findDuplicate(categories, 'Подарки')).toBeUndefined()
  })

  it('does not count the category being renamed as its own duplicate', () => {
    expect(findDuplicate(categories, 'Еда', 1)).toBeUndefined()
    expect(findDuplicate(categories, 'Еда', 2)?.id).toBe(1)
  })
})
