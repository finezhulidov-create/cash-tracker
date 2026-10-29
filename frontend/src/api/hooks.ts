import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { monthDateTimeRange } from '../lib/dates'
import {
  analyticsApi,
  budgetsApi,
  categoriesApi,
  transactionsApi,
  usersApi,
} from './endpoints'
import type {
  CreateBudgetRequest,
  TransactionFilters,
  TransactionRequest,
  UpdateBudgetRequest,
} from './types'

export const keys = {
  me: ['me'] as const,
  categories: ['categories'] as const,
  categorySplitCount: (id: number) => ['categories', 'split-count', id] as const,
  transactions: ['transactions'] as const,
  transactionList: (filters: TransactionFilters) => ['transactions', 'list', filters] as const,
  recentTransactions: (size: number) => ['transactions', 'recent', size] as const,
  monthTransactionCount: (yearMonth: string) => ['transactions', 'month-count', yearMonth] as const,
  analytics: ['analytics'] as const,
  summary: (yearMonth: string) => ['analytics', 'summary', yearMonth] as const,
  byCategory: (from: string, to: string) => ['analytics', 'by-category', from, to] as const,
  monthly: (year: number) => ['analytics', 'monthly', year] as const,
}

// ---------- профиль ----------

export function useMe() {
  return useQuery({ queryKey: keys.me, queryFn: usersApi.me })
}

export function useUpdateBalance() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (balance: number) => usersApi.updateBalance(balance),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.me }),
  })
}

// ---------- категории ----------

export function useCategories() {
  return useQuery({ queryKey: keys.categories, queryFn: categoriesApi.mine, staleTime: 60_000 })
}

export function useCategorySplitCount(id: number) {
  return useQuery({
    queryKey: keys.categorySplitCount(id),
    queryFn: () => categoriesApi.splitCount(id),
  })
}

export function useCreateCategory() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (name: string) => categoriesApi.create(name),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.categories }),
  })
}

export function useRenameCategory() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, name }: { id: number; name: string }) => categoriesApi.rename(id, name),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: keys.categories }),
        // имена категорий показываются в операциях, бюджетах и аналитике
        queryClient.invalidateQueries({ queryKey: keys.transactions }),
        queryClient.invalidateQueries({ queryKey: keys.analytics }),
      ]),
  })
}

export function useDeleteCategory() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => categoriesApi.remove(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.categories }),
  })
}

// ---------- траты ----------

export function useTransactionsInfinite(filters: TransactionFilters, enabled = true) {
  return useInfiniteQuery({
    queryKey: keys.transactionList(filters),
    queryFn: ({ pageParam }) => transactionsApi.list(filters, pageParam),
    initialPageParam: 0,
    enabled,
    getNextPageParam: (last) =>
      last.page.number + 1 < last.page.totalPages ? last.page.number + 1 : undefined,
  })
}

/** Сколько трат в месяце — для подзаголовка экрана «Операции». */
export function useMonthTransactionCount(yearMonth: string, enabled: boolean) {
  const { from, to } = monthDateTimeRange(yearMonth)
  return useQuery({
    queryKey: keys.monthTransactionCount(yearMonth),
    queryFn: () => transactionsApi.list({ from, to }, 0, 1),
    select: (page) => page.page.totalElements,
    enabled,
  })
}

export function useRecentTransactions(size = 6) {
  return useQuery({
    queryKey: keys.recentTransactions(size),
    queryFn: () => transactionsApi.list({}, 0, size),
    select: (page) => page.content,
  })
}

/** Любое изменение трат меняет списки, сводки, аналитику и счётчики частей у категорий. */
function useInvalidateAfterTransactionChange() {
  const queryClient = useQueryClient()
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: keys.transactions }),
      queryClient.invalidateQueries({ queryKey: keys.analytics }),
      queryClient.invalidateQueries({ queryKey: ['categories', 'split-count'] }),
    ])
}

export function useCreateTransaction() {
  const invalidate = useInvalidateAfterTransactionChange()
  return useMutation({
    mutationFn: (body: TransactionRequest) => transactionsApi.create(body),
    onSuccess: invalidate,
  })
}

export function useUpdateTransaction() {
  const invalidate = useInvalidateAfterTransactionChange()
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: TransactionRequest }) =>
      transactionsApi.update(id, body),
    onSuccess: invalidate,
  })
}

export function useDeleteTransaction() {
  const invalidate = useInvalidateAfterTransactionChange()
  return useMutation({
    mutationFn: (id: number) => transactionsApi.remove(id),
    onSuccess: invalidate,
  })
}

// ---------- бюджеты ----------

export function useCreateBudget() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: CreateBudgetRequest) => budgetsApi.create(body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.analytics }),
  })
}

export function useUpdateBudget() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: UpdateBudgetRequest }) =>
      budgetsApi.update(id, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.analytics }),
  })
}

export function useDeleteBudget() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => budgetsApi.remove(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.analytics }),
  })
}

// ---------- аналитика ----------

export function useMonthSummary(yearMonth: string) {
  return useQuery({
    queryKey: keys.summary(yearMonth),
    queryFn: () => analyticsApi.summary(yearMonth),
  })
}

export function useSpendingByCategory(from: string, to: string) {
  return useQuery({
    queryKey: keys.byCategory(from, to),
    queryFn: () => analyticsApi.byCategory(from, to),
  })
}

export function useMonthlySpending(year: number) {
  return useQuery({
    queryKey: keys.monthly(year),
    queryFn: () => analyticsApi.monthly(year),
  })
}
