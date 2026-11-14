import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { authOrigin } from './authRedirect'

export function ProtectedRoute() {
  const { isAuthenticated } = useAuth()
  const location = useLocation()
  return isAuthenticated
    ? <Outlet />
    : <Navigate replace state={authOrigin(location)} to="/login" />
}
