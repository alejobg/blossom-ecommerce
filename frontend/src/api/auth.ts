import client from './client'
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  UserResponse,
} from '../types/auth'

export async function loginUser(request: LoginRequest): Promise<AuthResponse> {
  const response = await client.post<AuthResponse>('/auth/login', request)
  return response.data
}

export async function registerUser(request: RegisterRequest): Promise<UserResponse> {
  const response = await client.post<UserResponse>('/auth/register', request)
  return response.data
}
