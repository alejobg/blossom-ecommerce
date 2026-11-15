import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { AccessDenied } from '../components/AccessDenied'
import { useAuth } from '../hooks/useAuth'
import type { UserResponse } from '../types/auth'
import { authOrigin } from './authRedirect'

export function ProtectedRoute({
  allowedRole,
  deniedMessage,
}: {
  allowedRole?: UserResponse['role']
  deniedMessage: string
}) {
  const { session } = useAuth()
  const location = useLocation()
  if (!session) return <Navigate replace state={authOrigin(location)} to="/login" />
  if (allowedRole && session.user.role !== allowedRole) {
    return <AccessDenied message={deniedMessage} />
  }
  return <Outlet />
}
