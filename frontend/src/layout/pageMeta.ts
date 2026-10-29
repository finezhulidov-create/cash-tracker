import { useMemo } from 'react'
import type { UserDto } from '../api/types'
import { useMonthTransactionCount } from '../api/hooks'
import { monthPrepositional, monthTitle, yearMonthOf } from '../lib/dates'
import { FORMS, plural } from '../lib/plural'

export interface PageMeta {
  title: string
  subtitle: string
}

/** Заголовок и подзаголовок экрана — как в прототипе. */
export function usePageMeta(pathname: string, user?: UserDto): PageMeta {
  const currentMonth = useMemo(() => yearMonthOf(new Date()), [])
  const onTransactions = pathname.startsWith('/transactions')
  const count = useMonthTransactionCount(currentMonth, onTransactions)

  if (onTransactions) {
    const total = count.data
    return {
      title: 'Операции',
      subtitle:
        total === undefined
          ? 'Все траты'
          : `${total} ${plural(total, FORMS.transaction)} в ${monthPrepositional(currentMonth)}`,
    }
  }
  if (pathname.startsWith('/budgets')) return { title: 'Бюджеты', subtitle: 'Лимиты трат на месяц' }
  if (pathname.startsWith('/analytics')) return { title: 'Аналитика', subtitle: 'Куда уходят деньги' }
  if (pathname.startsWith('/categories')) return { title: 'Категории', subtitle: 'Для разбивки трат' }
  if (pathname.startsWith('/profile')) return { title: 'Профиль', subtitle: user?.email ?? '' }
  return {
    title: 'Обзор',
    subtitle: user ? `${monthTitle(currentMonth)} · привет, ${user.name}` : monthTitle(currentMonth),
  }
}
