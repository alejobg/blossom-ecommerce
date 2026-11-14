import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { getAuthRedirect } from './authRedirect'

export function GuestRoute() {
  const { isAuthenticated } = useAuth()
  const location = useLocation()
  return isAuthenticated
    ? <Navigate replace to={getAuthRedirect(location.state, location.search)} />
    : <Outlet />
}
