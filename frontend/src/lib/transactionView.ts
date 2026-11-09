import type { TransactionDto } from '../api/types'

export interface DayGroup {
  /** YYYY-MM-DD */
  date: string
  total: number
  items: TransactionDto[]
}

/**
 * Группирует траты по дням. Порядок сохраняется: бэкенд уже отдаёт траты от новых к старым,
 * поэтому подряд идущие траты одного дня попадают в одну группу.
 */
export function groupByDay(transactions: readonly TransactionDto[]): DayGroup[] {
  const groups: DayGroup[] = []
  for (const transaction of transactions) {
    const date = transaction.dateTime.slice(0, 10)
    const last = groups[groups.length - 1]
    if (last && last.date === date) {
      last.items.push(transaction)
      last.total += transaction.amount
    } else {
      groups.push({ date, total: transaction.amount, items: [transaction] })
    }
  }
  return groups
}

export function sumAmounts(transactions: readonly TransactionDto[]): number {
  return transactions.reduce((sum, transaction) => sum + transaction.amount, 0)
}
