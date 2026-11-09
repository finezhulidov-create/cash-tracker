import { request } from './client'
import type {
  AiAdviceResponse,
  AiStatusResponse,
  BudgetDto,
  CategoryDto,
  CategorySpendingDto,
  CreateBudgetRequest,
  ImportPreviewItem,
  LoginRequest,
  MonthlySpendingDto,
  MonthSummaryDto,
  PagedModel,
  RegisterRequest,
  TokenResponse,
  TransactionDto,
  TransactionFilters,
  TransactionRequest,
  TransactionSplitDto,
  UpdateBudgetRequest,
  UserDto,
} from './types'

export const TRANSACTIONS_PAGE_SIZE = 20

export const authApi = {
  login: (body: LoginRequest) => request<TokenResponse>('/auth/login', { method: 'POST', body }),
  register: (body: RegisterRequest) =>
    request<TokenResponse>('/auth/register', { method: 'POST', body }),
}

export const usersApi = {
  me: () => request<UserDto>('/v1/users/me'),
  updateBalance: (balance: number) =>
    request<{ balance: number }>('/v1/users/me/balance', { method: 'PUT', body: { balance } }),
}

export const categoriesApi = {
  /** Все категории пользователя одной страницей: их немного, а выпадающим спискам нужен полный набор. */
  mine: () =>
    request<PagedModel<CategoryDto>>('/v1/categories/mine', { query: { size: 200 } }).then(
      (page) => page.content,
    ),
  create: (categoryName: string) =>
    request<CategoryDto>('/v1/categories', { method: 'POST', body: { categoryName } }),
  rename: (id: number, categoryName: string) =>
    request<CategoryDto>(`/v1/categories/${id}`, { method: 'PUT', body: { categoryName } }),
  remove: (id: number) => request<void>(`/v1/categories/${id}`, { method: 'DELETE' }),
  /** Сколько частей трат привязано к категории (totalElements страницы из одного элемента). */
  splitCount: (id: number) =>
    request<PagedModel<TransactionSplitDto>>(`/v1/categories/${id}/splits`, {
      query: { size: 1 },
    }).then((page) => page.page.totalElements),
}

export const transactionsApi = {
  list: (filters: TransactionFilters, page: number, size = TRANSACTIONS_PAGE_SIZE) =>
    request<PagedModel<TransactionDto>>('/v1/transactions', {
      query: { ...filters, page, size, sort: 'dateTime,desc' },
    }),
  create: (body: TransactionRequest) =>
    request<TransactionDto>('/v1/transactions', { method: 'POST', body }),
  update: (id: number, body: TransactionRequest) =>
    request<TransactionDto>(`/v1/transactions/${id}`, { method: 'PUT', body }),
  remove: (id: number) => request<void>(`/v1/transactions/${id}`, { method: 'DELETE' }),
}

export const statementImportApi = {
  /** Разбор PDF-выписки моделью: идёт десятки секунд и дольше, поэтому запрос можно отменить. */
  parse: (file: File, signal?: AbortSignal) => {
    const body = new FormData()
    body.append('file', file)
    return request<ImportPreviewItem[]>('/v1/parsing', { method: 'POST', body, signal })
  },
  /** Сохраняет выбранные операции одной пачкой: либо все, либо ни одной. */
  save: (transactions: TransactionRequest[]) =>
    request<TransactionDto[]>('/v1/parsing/save', { method: 'POST', body: { transactions } }),
}

export const budgetsApi = {
  create: (body: CreateBudgetRequest) =>
    request<BudgetDto>('/v1/budgets', { method: 'POST', body }),
  update: (id: number, body: UpdateBudgetRequest) =>
    request<BudgetDto>(`/v1/budgets/${id}`, { method: 'PUT', body }),
  remove: (id: number) => request<void>(`/v1/budgets/${id}`, { method: 'DELETE' }),
}

export const aiApi = {
  /** Включён ли ИИ-советник на сервере. */
  status: () => request<AiStatusResponse>('/v1/ai/status'),
  /** Ответ модели идёт до минуты и дольше, поэтому запрос можно отменить через signal. */
  advice: (yearMonth: string, signal?: AbortSignal) =>
    request<AiAdviceResponse>('/v1/ai/advice', { method: 'POST', body: { yearMonth }, signal }),
}

export const analyticsApi = {
  summary: (yearMonth: string) =>
    request<MonthSummaryDto>('/v1/analytics/summary', { query: { yearMonth } }),
  byCategory: (from: string, to: string) =>
    request<CategorySpendingDto[]>('/v1/analytics/by-category', { query: { from, to } }),
  monthly: (year: number) =>
    request<MonthlySpendingDto[]>('/v1/analytics/monthly', { query: { year } }),
}
