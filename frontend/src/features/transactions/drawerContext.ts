import { createContext, useContext } from 'react'
import type { TransactionDto } from '../../api/types'

export interface TransactionDrawerApi {
  /** Открыть шторку для новой траты */
  openNew: () => void
  /** Открыть шторку редактирования существующей траты */
  openEdit: (transaction: TransactionDto) => void
}

export const TransactionDrawerContext = createContext<TransactionDrawerApi | null>(null)

export function useTransactionDrawer(): TransactionDrawerApi {
  const api = useContext(TransactionDrawerContext)
  if (!api) throw new Error('useTransactionDrawer нужно вызывать внутри TransactionDrawerProvider')
  return api
}
