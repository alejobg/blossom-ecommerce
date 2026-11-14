import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export function GuestRoute() {
  const { isAuthenticated } = useAuth()
  return isAuthenticated ? <Navigate replace to="/" /> : <Outlet />
}
