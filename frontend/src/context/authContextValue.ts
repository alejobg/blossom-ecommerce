import { createContext } from 'react'
import type { AuthSession, LoginRequest } from '../types/auth'

export interface AuthContextValue {
  session: AuthSession | null
  isAuthenticated: boolean
  login: (credentials: LoginRequest) => Promise<void>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)
