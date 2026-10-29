import type { LucideIcon } from 'lucide-react'
import { ArrowLeftRight, ChartPie, LayoutDashboard, PiggyBank, Tags, User } from 'lucide-react'

export interface NavItem {
  to: string
  label: string
  Icon: LucideIcon
}

export const NAV_ITEMS: readonly NavItem[] = [
  { to: '/', label: 'Обзор', Icon: LayoutDashboard },
  { to: '/transactions', label: 'Операции', Icon: ArrowLeftRight },
  { to: '/budgets', label: 'Бюджеты', Icon: PiggyBank },
  { to: '/analytics', label: 'Аналитика', Icon: ChartPie },
  { to: '/categories', label: 'Категории', Icon: Tags },
  { to: '/profile', label: 'Профиль', Icon: User },
]

/** На телефоне в таб-баре четыре раздела и «+» посередине; профиль — по аватару, категории — из профиля. */
export const TAB_LEFT: readonly NavItem[] = NAV_ITEMS.slice(0, 2)
export const TAB_RIGHT: readonly NavItem[] = NAV_ITEMS.slice(2, 4)

export function isActivePath(pathname: string, to: string): boolean {
  return to === '/' ? pathname === '/' : pathname === to || pathname.startsWith(`${to}/`)
}
