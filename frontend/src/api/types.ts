/** Контракты бэкенда (Spring Boot). Суммы приходят числами, даты — ISO-строками без часового пояса. */

export interface TokenResponse {
  accessToken: string
  tokenType: string
  /** Время жизни токена, секунды */
  expiresIn: number
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  username: string
  password: string
}

export interface UserDto {
  name: string
  email: string
  balance: number
}

export interface CategoryDto {
  id: number
  categoryName: string
  userId: number
}

export interface TransactionSplitDto {
  id: number
  transactionId: number
  amount: number
  category: CategoryDto
}

export interface TransactionDto {
  id: number
  userId: number
  amount: number
  description: string
  /** «2026-10-28T14:30:00» */
  dateTime: string
  splits: TransactionSplitDto[]
}

export interface SplitRequest {
  categoryId: number
  amount: number
}

export interface TransactionRequest {
  description: string
  amount: number
  dateTime: string
  splits: SplitRequest[]
}

export interface TransactionFilters {
  q?: string
  categoryId?: number
  minAmount?: number
  maxAmount?: number
  /** LocalDateTime, включительно */
  from?: string
  to?: string
}

export interface PagedModel<T> {
  content: T[]
  page: {
    size: number
    number: number
    totalElements: number
    totalPages: number
  }
}

export interface BudgetDto {
  id: number
  limitAmount: number
  /** Первое число месяца */
  period: string
  category: CategoryDto | null
}

export interface BudgetStatusDto {
  budgetId: number
  period: string
  category: CategoryDto | null
  assigned: number
  spent: number
  /** Остаток; отрицательный при перерасходе */
  available: number
}

export interface MonthSummaryDto {
  /** «2026-10» */
  yearMonth: string
  totalSpent: number
  overall: BudgetStatusDto | null
  categories: BudgetStatusDto[]
}

export interface CategorySpendingDto {
  categoryId: number
  categoryName: string
  spent: number
}

export interface MonthlySpendingDto {
  /** «2026-10» */
  month: string
  spent: number
}

export interface CreateBudgetRequest {
  amount: number
  /** Любая дата месяца, например «2026-10-01» */
  period: string
  categoryId: number | null
}

export interface UpdateBudgetRequest {
  limitAmount: number
  period: string
}

export interface ErrorResponse {
  status: number
  message: string
  timestamp: string
  errorCode: string
  errors?: Record<string, string> | null
}
