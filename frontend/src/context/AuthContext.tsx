import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { loginUser } from '../api/auth'
import {
  AUTH_SESSION_CLEARED_EVENT,
  clearAuthSession,
  getStoredAuthSession,
  saveAuthSession,
} from '../api/sessionStorage'
import type { AuthSession, LoginRequest } from '../types/auth'
import { AuthContext } from './authContextValue'

interface AuthProviderProps {
  children: ReactNode
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [session, setSession] = useState<AuthSession | null>(getStoredAuthSession)

  useEffect(() => {
    const handleSessionCleared = () => setSession(null)
    window.addEventListener(AUTH_SESSION_CLEARED_EVENT, handleSessionCleared)
    return () => window.removeEventListener(AUTH_SESSION_CLEARED_EVENT, handleSessionCleared)
  }, [])

  const login = useCallback(async (credentials: LoginRequest) => {
    const nextSession = await loginUser(credentials)
    saveAuthSession(nextSession)
    setSession(nextSession)
  }, [])

  const logout = useCallback(() => {
    clearAuthSession()
    setSession(null)
  }, [])

  const value = useMemo(
    () => ({
      session,
      isAuthenticated: session !== null,
      login,
      logout,
    }),
    [session, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
