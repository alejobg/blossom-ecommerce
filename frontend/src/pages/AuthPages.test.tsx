import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, useLocation } from 'react-router-dom'
import { loginUser, registerUser } from '../api/auth'
import { getProductById, searchProducts } from '../api/products'
import { AUTH_STORAGE_KEY } from '../api/sessionStorage'
import { AuthProvider } from '../context/AuthContext'
import { AppRouteTree } from '../routes/AppRoutes'
import type { AuthResponse, UserResponse } from '../types/auth'

vi.mock('../api/auth', () => ({
  loginUser: vi.fn(),
  registerUser: vi.fn(),
}))

vi.mock('../api/products', () => ({
  getProductById: vi.fn(),
  searchProducts: vi.fn(),
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

function LocationProbe() {
  const { pathname, search, hash } = useLocation()
  return <div data-testid="location">{pathname}{search}{hash}</div>
}

function renderApp(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider>
        <AppRouteTree />
        <LocationProbe />
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('authentication pages', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    vi.mocked(searchProducts).mockResolvedValue({
      content: [], page: 0, size: 20, totalElements: 0, totalPages: 0,
    })
    vi.mocked(getProductById).mockResolvedValue({
      id: 12, name: 'Ceramic mug', description: 'A mug', price: 18.5, stock: 6, category: 'HOME',
    })
  })

  it('sends login credentials, persists the token and redirects to the public catalog', async () => {
    const user = userEvent.setup()
    vi.mocked(loginUser).mockResolvedValue(authResponse)
    renderApp('/login')

    await user.type(screen.getByLabelText('Email address'), customer.email)
    await user.type(screen.getByLabelText('Password'), 'secret123')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(loginUser).toHaveBeenCalledWith({ email: customer.email, password: 'secret123' })
    expect(await screen.findByRole('heading', { name: 'The collection' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent(/^\/$/)
    expect(screen.getByText(customer.name)).toBeInTheDocument()
    await waitFor(() => {
      expect(JSON.parse(localStorage.getItem(AUTH_STORAGE_KEY) ?? 'null')).toEqual(authResponse)
    })
  })

  it('registers and redirects to the catalog without creating a local session', async () => {
    const user = userEvent.setup()
    vi.mocked(registerUser).mockResolvedValue(customer)
    renderApp('/register')

    await user.type(screen.getByLabelText('Name'), customer.name)
    await user.type(screen.getByLabelText('Email address'), customer.email)
    await user.type(screen.getByLabelText('Password'), 'secret123')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(registerUser).toHaveBeenCalledWith({
      name: customer.name,
      email: customer.email,
      password: 'secret123',
    })
    expect(await screen.findByRole('heading', { name: 'The collection' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent(/^\/$/)
    expect(screen.getByRole('link', { name: 'Sign in' })).toBeInTheDocument()
    expect(localStorage.getItem(AUTH_STORAGE_KEY)).toBeNull()
  })

  it('returns to the originating detail URL after signing in', async () => {
    const user = userEvent.setup()
    vi.mocked(loginUser).mockResolvedValue(authResponse)
    renderApp('/products/12?ref=home#details')

    await user.click(screen.getByRole('link', { name: 'Sign in' }))
    expect(screen.queryByRole('navigation', { name: 'Main navigation' })).not.toBeInTheDocument()
    await user.type(screen.getByLabelText('Email address'), customer.email)
    await user.type(screen.getByLabelText('Password'), 'secret123')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('heading', { name: 'Ceramic mug' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/products/12?ref=home#details')
  })

  it('returns to the original URL after a session-expiry redirect', async () => {
    const user = userEvent.setup()
    vi.mocked(loginUser).mockResolvedValue(authResponse)
    renderApp(`/login?${new URLSearchParams({ returnTo: '/products/12?ref=home#details' })}`)

    await user.type(screen.getByLabelText('Email address'), customer.email)
    await user.type(screen.getByLabelText('Password'), 'secret123')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('heading', { name: 'Ceramic mug' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/products/12?ref=home#details')
  })

  it('does not redirect to an external return URL', async () => {
    const user = userEvent.setup()
    vi.mocked(loginUser).mockResolvedValue(authResponse)
    renderApp(`/login?${new URLSearchParams({ returnTo: '//example.com/' })}`)

    await user.type(screen.getByLabelText('Email address'), customer.email)
    await user.type(screen.getByLabelText('Password'), 'secret123')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('heading', { name: 'The collection' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent(/^\/$/)
  })

  it('preserves the originating URL when switching to registration', async () => {
    const user = userEvent.setup()
    vi.mocked(registerUser).mockResolvedValue(customer)
    renderApp('/products/12?ref=home')

    await user.click(screen.getByRole('link', { name: 'Sign in' }))
    await user.click(screen.getByRole('link', { name: 'Create an account' }))
    await user.type(screen.getByLabelText('Name'), customer.name)
    await user.type(screen.getByLabelText('Email address'), customer.email)
    await user.type(screen.getByLabelText('Password'), 'secret123')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByRole('heading', { name: 'Ceramic mug' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/products/12?ref=home')
    expect(localStorage.getItem(AUTH_STORAGE_KEY)).toBeNull()
  })

  it.each(['/login', '/register'])('redirects an existing session away from %s', async (path) => {
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(authResponse))
    renderApp(path)

    expect(await screen.findByRole('heading', { name: 'The collection' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent(/^\/$/)
    expect(screen.getByText(customer.name)).toBeInTheDocument()
  })

  it('shows the public catalog header and updates it when signing out', async () => {
    const user = userEvent.setup()
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(authResponse))
    renderApp('/')

    expect(screen.getByRole('link', { name: 'Products' })).toHaveAttribute('href', '/')
    expect(screen.getByRole('link', { name: 'blossom.' })).toHaveAttribute('href', '/')
    expect(screen.getByText(customer.name)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Sign out' }))
    expect(screen.getByRole('link', { name: 'Sign in' })).toBeInTheDocument()
    expect(localStorage.getItem(AUTH_STORAGE_KEY)).toBeNull()
    expect(screen.getByTestId('location')).toHaveTextContent(/^\/$/)
  })

  it('rejects passwords that exceed the backend UTF-8 byte limit', async () => {
    const user = userEvent.setup()
    renderApp('/register')

    await user.type(screen.getByLabelText('Name'), customer.name)
    await user.type(screen.getByLabelText('Email address'), customer.email)
    await user.type(screen.getByLabelText('Password'), `${'ñ'.repeat(36)}1`)
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByText('Password cannot exceed 72 bytes.')).toBeInTheDocument()
    expect(registerUser).not.toHaveBeenCalled()
  })
})
