import type { ReactNode } from 'react'
import { useCallback, useMemo, useState } from 'react'
import type { TransactionDto } from '../../api/types'
import { TransactionDrawerContext } from './drawerContext'
import { TransactionDrawer } from './TransactionDrawer'

interface DrawerState {
  open: boolean
  /** Редактируемая трата; null — создание новой */
  transaction: TransactionDto | null
}

/** Одна на всё приложение шторка траты; открывается из шапки, таб-бара и клика по операции. */
export function TransactionDrawerProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<DrawerState>({ open: false, transaction: null })

  const openNew = useCallback(() => setState({ open: true, transaction: null }), [])
  const openEdit = useCallback(
    (transaction: TransactionDto) => setState({ open: true, transaction }),
    [],
  )
  const close = useCallback(() => setState((prev) => ({ ...prev, open: false })), [])

  const api = useMemo(() => ({ openNew, openEdit }), [openNew, openEdit])

  return (
    <TransactionDrawerContext value={api}>
      {children}
      <TransactionDrawer open={state.open} transaction={state.transaction} onClose={close} />
    </TransactionDrawerContext>
  )
}
