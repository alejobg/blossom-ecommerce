import type { AuthSession } from '../types/auth'

export function canPurchase(session: AuthSession | null): boolean {
  return session === null || session.user.role === 'CUSTOMER'
}
