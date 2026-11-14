import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { loginUser, registerUser } from '../api/auth'
import { AUTH_STORAGE_KEY } from '../api/sessionStorage'
import { AuthProvider } from '../context/AuthContext'
import { LoginPage } from './LoginPage'
import { RegisterPage } from './RegisterPage'
import type { AuthResponse, UserResponse } from '../types/auth'

vi.mock('../api/auth', () => ({
  loginUser: vi.fn(),
  registerUser: vi.fn(),
}))

const customer: UserResponse = {
  id: 7,
  name: 'Ana Flores',
  email: 'ana@example.com',
  role: 'CUSTOMER',
}

const authResponse: AuthResponse = {
  accessToken: 'jwt-token',
  tokenType: 'Bearer',
  expiresIn: 3600,
  user: customer,
}

function renderLoginPage() {
  return render(
    <MemoryRouter initialEntries={['/login']}>
      <AuthProvider>
        <LoginPage />
      </AuthProvider>
    </MemoryRouter>,
  )
}

function renderRegisterPage() {
  return render(
    <MemoryRouter initialEntries={['/register']}>
      <AuthProvider>
        <RegisterPage />
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('authentication pages', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('sends login credentials and persists the backend token response', async () => {
    const user = userEvent.setup()
    vi.mocked(loginUser).mockResolvedValue(authResponse)
    renderLoginPage()

    await user.type(screen.getByLabelText('Email address'), customer.email)
    await user.type(screen.getByLabelText('Password'), 'secret123')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(loginUser).toHaveBeenCalledWith({ email: customer.email, password: 'secret123' })
    expect(await screen.findByRole('status')).toHaveTextContent('You have signed in successfully.')
    await waitFor(() => {
      expect(JSON.parse(localStorage.getItem(AUTH_STORAGE_KEY) ?? 'null')).toEqual(authResponse)
    })
  })

  it('registers with the backend DTO fields without creating a local session', async () => {
    const user = userEvent.setup()
    vi.mocked(registerUser).mockResolvedValue(customer)
    renderRegisterPage()

    await user.type(screen.getByLabelText('Name'), customer.name)
    await user.type(screen.getByLabelText('Email address'), customer.email)
    await user.type(screen.getByLabelText('Password'), 'secret123')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(registerUser).toHaveBeenCalledWith({
      name: customer.name,
      email: customer.email,
      password: 'secret123',
    })
    expect(await screen.findByRole('status')).toHaveTextContent('Your account is ready!')
    expect(localStorage.getItem(AUTH_STORAGE_KEY)).toBeNull()
  })

  it('rejects passwords that exceed the backend UTF-8 byte limit', async () => {
    const user = userEvent.setup()
    renderRegisterPage()

    await user.type(screen.getByLabelText('Name'), customer.name)
    await user.type(screen.getByLabelText('Email address'), customer.email)
    await user.type(screen.getByLabelText('Password'), `${'ñ'.repeat(36)}1`)
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByText('Password cannot exceed 72 bytes.')).toBeInTheDocument()
    expect(registerUser).not.toHaveBeenCalled()
  })
})
