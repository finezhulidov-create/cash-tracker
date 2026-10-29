import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useSession } from './useAuth'

/** Защищённые экраны: без сессии — на вход, с возвратом на исходную страницу после входа. */
export function RequireAuth() {
  const session = useSession()
  const location = useLocation()
  if (!session) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  }
  return <Outlet />
}

/** Экран входа недоступен авторизованным пользователям. */
export function PublicOnly() {
  const session = useSession()
  if (session) return <Navigate to="/" replace />
  return <Outlet />
}
