import type { ComponentType } from 'react'
import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { PublicOnly, RequireAuth } from './auth/guards'
import { AppShell } from './layout/AppShell'

/** Экран подгружается отдельным файлом при первом заходе на маршрут. */
function lazyPage<Name extends string>(load: () => Promise<Record<Name, ComponentType>>, name: Name) {
  return lazy(() => load().then((module) => ({ default: module[name] })))
}

const LoginPage = lazyPage(() => import('./features/auth/LoginPage'), 'LoginPage')
const DashboardPage = lazyPage(() => import('./features/dashboard/DashboardPage'), 'DashboardPage')
const TransactionsPage = lazyPage(() => import('./features/transactions/TransactionsPage'), 'TransactionsPage')
const BudgetsPage = lazyPage(() => import('./features/budgets/BudgetsPage'), 'BudgetsPage')
const AnalyticsPage = lazyPage(() => import('./features/analytics/AnalyticsPage'), 'AnalyticsPage')
const CategoriesPage = lazyPage(() => import('./features/categories/CategoriesPage'), 'CategoriesPage')
const ProfilePage = lazyPage(() => import('./features/profile/ProfilePage'), 'ProfilePage')

export function App() {
  return (
    <Suspense fallback={null}>
      <Routes>
        <Route element={<PublicOnly />}>
          <Route path="/login" element={<LoginPage />} />
        </Route>
        <Route element={<RequireAuth />}>
          <Route element={<AppShell />}>
            <Route index element={<DashboardPage />} />
            <Route path="transactions" element={<TransactionsPage />} />
            <Route path="budgets" element={<BudgetsPage />} />
            <Route path="analytics" element={<AnalyticsPage />} />
            <Route path="categories" element={<CategoriesPage />} />
            <Route path="profile" element={<ProfilePage />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  )
}
