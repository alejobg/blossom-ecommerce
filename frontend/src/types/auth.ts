export interface UserResponse {
  id: number
  name: string
  email: string
  role: 'CUSTOMER' | 'ADMIN'
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: UserResponse
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest extends LoginRequest {
  name: string
}

export type AuthSession = AuthResponse

export interface ApiProblem {
  title?: string
  detail?: string
  status?: number
  errors?: Record<string, string>
}
