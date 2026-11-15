import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, useLocation, useNavigate } from 'react-router-dom'
import { loginUser, registerUser } from '../api/auth'
import { getOrderById } from '../api/orders'
import { getProductById, searchProducts } from '../api/products'
import { AUTH_STORAGE_KEY } from '../api/sessionStorage'
import { AuthProvider } from '../context/AuthContext'
import { CartProvider } from '../context/CartContext'
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

vi.mock('../api/orders', () => ({
  getOrderById: vi.fn(),
  listOrders: vi.fn(),
  placeOrder: vi.fn(),
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
  const { pathname, search, hash, state } = useLocation()
  const navigate = useNavigate()
  return (
    <>
      <div data-testid="location">{pathname}{search}{hash}</div>
      <div data-testid="route-state">{JSON.stringify(state)}</div>
      <button onClick={() => navigate(-1)}>Go back</button>
    </>
  )
}

function renderApp(path: string | { pathname: string; state: { from: string } }) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider>
        <CartProvider>
          <AppRouteTree />
          <LocationProbe />
        </CartProvider>
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
    vi.mocked(getOrderById).mockResolvedValue({
      id: 99, items: [], total: 0, createdAt: '2026-01-01T12:00:00Z',
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

  it('registers, signs in, and redirects to the catalog', async () => {
    const user = userEvent.setup()
    vi.mocked(registerUser).mockResolvedValue(customer)
    vi.mocked(loginUser).mockResolvedValue(authResponse)
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
    expect(loginUser).toHaveBeenCalledWith({ email: customer.email, password: 'secret123' })
    expect(await screen.findByRole('heading', { name: 'The collection' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent(/^\/$/)
    expect(screen.getByText(customer.name)).toBeInTheDocument()
    expect(JSON.parse(localStorage.getItem(AUTH_STORAGE_KEY) ?? 'null')).toEqual(authResponse)
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
    vi.mocked(loginUser).mockResolvedValue(authResponse)
    renderApp('/products/12?ref=home')

    await user.click(screen.getByRole('link', { name: 'Sign in' }))
    await user.click(screen.getByRole('link', { name: 'Create an account' }))
    await user.type(screen.getByLabelText('Name'), customer.name)
    await user.type(screen.getByLabelText('Email address'), customer.email)
    await user.type(screen.getByLabelText('Password'), 'secret123')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByRole('heading', { name: 'Ceramic mug' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/products/12?ref=home')
    expect(JSON.parse(localStorage.getItem(AUTH_STORAGE_KEY) ?? 'null')).toEqual(authResponse)
  })

  it('redirects to a string origin in router state after login', async () => {
    const user = userEvent.setup()
    vi.mocked(loginUser).mockResolvedValue(authResponse)
    renderApp({ pathname: '/login', state: { from: '/products/12?ref=home#details' } })

    await user.type(screen.getByLabelText('Email address'), customer.email)
    await user.type(screen.getByLabelText('Password'), 'secret123')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('heading', { name: 'Ceramic mug' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/products/12?ref=home#details')
  })

  it('replaces the login history entry after signing in', async () => {
    const user = userEvent.setup()
    vi.mocked(loginUser).mockResolvedValue(authResponse)
    render(
      <MemoryRouter initialEntries={['/', { pathname: '/login', state: { from: '/cart' } }]} initialIndex={1}>
        <AuthProvider><CartProvider><AppRouteTree /><LocationProbe /></CartProvider></AuthProvider>
      </MemoryRouter>,
    )
    await user.type(screen.getByLabelText('Email address'), customer.email)
    await user.type(screen.getByLabelText('Password'), 'secret123')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(await screen.findByRole('heading', { name: 'Your cart' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Go back' }))
    expect(await screen.findByRole('heading', { name: 'The collection' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent(/^\/$/)
  })

  it('returns to a protected order URL after logging in', async () => {
    const user = userEvent.setup()
    vi.mocked(loginUser).mockResolvedValue(authResponse)
    renderApp('/orders/99?ref=cart#summary')

    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
    expect(JSON.parse(screen.getByTestId('route-state').textContent ?? '{}')).toEqual({
      from: '/orders/99?ref=cart#summary',
    })
    await user.type(screen.getByLabelText('Email address'), customer.email)
    await user.type(screen.getByLabelText('Password'), 'secret123')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('heading', { name: 'Order #99' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/orders/99?ref=cart#summary')
  })

  it('returns to a protected order URL after registering', async () => {
    const user = userEvent.setup()
    vi.mocked(registerUser).mockResolvedValue(customer)
    vi.mocked(loginUser).mockResolvedValue(authResponse)
    renderApp('/orders/99?ref=cart#summary')

    await user.click(screen.getByRole('link', { name: 'Create an account' }))
    await user.type(screen.getByLabelText('Name'), customer.name)
    await user.type(screen.getByLabelText('Email address'), customer.email)
    await user.type(screen.getByLabelText('Password'), 'secret123')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByRole('heading', { name: 'Order #99' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/orders/99?ref=cart#summary')
  })

  it('allows manual sign-in when automatic login after registration fails', async () => {
    const user = userEvent.setup()
    vi.mocked(registerUser).mockResolvedValue(customer)
    vi.mocked(loginUser).mockRejectedValue(new Error('Login failed'))
    renderApp({ pathname: '/register', state: { from: '/cart' } })

    await user.type(screen.getByLabelText('Name'), customer.name)
    await user.type(screen.getByLabelText('Email address'), customer.email)
    await user.type(screen.getByLabelText('Password'), 'secret123')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Your account was created, but sign-in failed')
    expect(screen.queryByRole('button', { name: 'Create account' })).not.toBeInTheDocument()
    await user.click(screen.getAllByRole('link', { name: 'Sign in' })[0])
    vi.mocked(loginUser).mockResolvedValue(authResponse)
    await user.type(screen.getByLabelText('Email address'), customer.email)
    await user.type(screen.getByLabelText('Password'), 'secret123')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(await screen.findByRole('heading', { name: 'Your cart' })).toBeInTheDocument()
  })

  it.each(['/login', '/register'])('redirects an existing session away from %s', async (path) => {
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(authResponse))
    renderApp(path)

    expect(await screen.findByRole('heading', { name: 'The collection' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent(/^\/$/)
    expect(screen.getByText(customer.name)).toBeInTheDocument()
  })

  it.each(['/login', '/register'])('preserves the origin for an existing session on %s', async (path) => {
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(authResponse))
    renderApp({ pathname: path, state: { from: '/cart?ref=checkout#items' } })

    expect(await screen.findByRole('heading', { name: 'Your cart' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/cart?ref=checkout#items')
  })

  it.each(['/login', '/register'])('uses a safe returnTo for an existing session on %s', async (path) => {
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(authResponse))
    renderApp(`${path}?${new URLSearchParams({ returnTo: '/cart?ref=checkout#items' })}`)

    expect(await screen.findByRole('heading', { name: 'Your cart' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/cart?ref=checkout#items')
  })

  it.each(['/login', '/register'])('rejects an external origin for an existing session on %s', async (path) => {
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(authResponse))
    renderApp({ pathname: path, state: { from: '//example.com/' } })

    expect(await screen.findByRole('heading', { name: 'The collection' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent(/^\/$/)
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
