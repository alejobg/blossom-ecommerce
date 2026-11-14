import type { AuthSession } from '../types/auth'

export const AUTH_STORAGE_KEY = 'blossom.auth'
export const AUTH_SESSION_CLEARED_EVENT = 'blossom:auth-session-cleared'

function isAuthSession(value: unknown): value is AuthSession {
  if (typeof value !== 'object' || value === null) return false

  const session = value as Partial<AuthSession>
  const user = session.user

  return (
    typeof session.accessToken === 'string' &&
    typeof session.tokenType === 'string' &&
    typeof session.expiresIn === 'number' &&
    typeof user === 'object' &&
    user !== null &&
    typeof user.id === 'number' &&
    typeof user.name === 'string' &&
    typeof user.email === 'string' &&
    (user.role === 'CUSTOMER' || user.role === 'ADMIN')
  )
}

export function getStoredAuthSession(): AuthSession | null {
  const storedSession = localStorage.getItem(AUTH_STORAGE_KEY)
  if (!storedSession) return null

  try {
    const parsed: unknown = JSON.parse(storedSession)
    if (isAuthSession(parsed)) return parsed
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY)
    return null
  }

  localStorage.removeItem(AUTH_STORAGE_KEY)
  return null
}

export function saveAuthSession(session: AuthSession): void {
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session))
}

export function clearAuthSession(): void {
  localStorage.removeItem(AUTH_STORAGE_KEY)
  window.dispatchEvent(new Event(AUTH_SESSION_CLEARED_EVENT))
}
