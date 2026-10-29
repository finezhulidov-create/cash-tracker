import type { BudgetStatusDto, CategoryDto } from '../../api/types'

/** Категории, на которые в выбранном месяце ещё нет бюджета. */
export function unbudgetedCategories(
  categories: readonly CategoryDto[],
  budgets: readonly Pick<BudgetStatusDto, 'category'>[],
): CategoryDto[] {
  const budgeted = new Set(budgets.map((budget) => budget.category?.id))
  return categories.filter((category) => !budgeted.has(category.id))
}
