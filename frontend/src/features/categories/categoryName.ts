import type { CategoryDto } from '../../api/types'

/** Ограничение бэкенда на название категории. */
export const MAX_CATEGORY_NAME = 50

/**
 * Категория с таким же названием (без учёта регистра и пробелов по краям).
 * Бэкенд дубликаты не запрещает, поэтому проверяем на клиенте. exceptId — переименовываемая категория.
 */
export function findDuplicate(
  categories: readonly CategoryDto[],
  name: string,
  exceptId?: number,
): CategoryDto | undefined {
  const normalized = name.trim().toLocaleLowerCase('ru')
  return categories.find(
    (category) =>
      category.id !== exceptId && category.categoryName.trim().toLocaleLowerCase('ru') === normalized,
  )
}
