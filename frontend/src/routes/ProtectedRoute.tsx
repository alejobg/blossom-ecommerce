import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { AccessDenied } from '../components/AccessDenied'
import { useAuth } from '../hooks/useAuth'
import type { UserResponse } from '../types/auth'
import { authOrigin } from './authRedirect'

export function ProtectedRoute({ allowedRole }: { allowedRole?: UserResponse['role'] }) {
  const { session } = useAuth()
  const location = useLocation()
  if (!session) return <Navigate replace state={authOrigin(location)} to="/login" />
  if (allowedRole && session.user.role !== allowedRole) {
    return <AccessDenied message="Only customer accounts can view orders." />
  }
  return <Outlet />
}
