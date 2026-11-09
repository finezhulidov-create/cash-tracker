import type {
  BudgetStatusDto,
  CategoryDto,
  MonthSummaryDto,
  PagedModel,
  TransactionDto,
  UserDto,
} from '../api/types'

export const user: UserDto = { name: 'Алекс', email: 'alex@example.com', balance: 152400 }

export const food: CategoryDto = { id: 1, categoryName: 'Еда', userId: 1 }
export const transport: CategoryDto = { id: 2, categoryName: 'Транспорт', userId: 1 }
export const cafe: CategoryDto = { id: 4, categoryName: 'Кафе', userId: 1 }
export const categories: CategoryDto[] = [food, transport, cafe]

export function page<T>(content: T[], number = 0, totalPages = 1, totalElements = content.length): PagedModel<T> {
  return { content, page: { size: 20, number, totalElements, totalPages } }
}

export function transaction(
  id: number,
  description: string,
  dateTime: string,
  parts: [CategoryDto, number][],
): TransactionDto {
  return {
    id,
    userId: 1,
    amount: parts.reduce((sum, [, amount]) => sum + amount, 0),
    description,
    dateTime,
    splits: parts.map(([category, amount], index) => ({
      id: id * 10 + index,
      transactionId: id,
      amount,
      category,
    })),
  }
}

export function budgetStatus(
  budgetId: number,
  category: CategoryDto | null,
  assigned: number,
  spent: number,
): BudgetStatusDto {
  return { budgetId, period: '2026-10-01', category, assigned, spent, available: assigned - spent }
}

export function summary(overrides: Partial<MonthSummaryDto> = {}): MonthSummaryDto {
  return {
    yearMonth: '2026-10',
    totalSpent: 29809,
    overall: budgetStatus(1, null, 55000, 29809),
    categories: [budgetStatus(2, food, 15000, 8160), budgetStatus(3, cafe, 4000, 3900)],
    ...overrides,
  }
}
