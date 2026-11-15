import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, type AxiosAdapter } from 'axios'
import { useState, type PropsWithChildren } from 'react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { MemoryRouter, useLocation } from 'react-router-dom'
import client from '../api/client'
import { getOrderById, placeOrder } from '../api/orders'
import { loginUser, registerUser } from '../api/auth'
import { AuthProvider } from '../context/AuthContext'
import { AuthContext } from '../context/authContextValue'
import { CartProvider } from '../context/CartContext'
import { AUTH_SESSION_CLEARED_EVENT, AUTH_STORAGE_KEY } from '../api/sessionStorage'
import { CART_STORAGE_KEY } from '../types/cart'
import type { AuthSession, LoginRequest } from '../types/auth'
import { AppRouteTree } from '../routes/AppRoutes'

vi.mock('../api/orders', () => ({ placeOrder: vi.fn(), getOrderById: vi.fn(), listOrders: vi.fn() }))
vi.mock('../api/auth', () => ({ loginUser: vi.fn(), registerUser: vi.fn() }))

const originalAdapter = client.defaults.adapter
const unauthorizedAdapter: AxiosAdapter = async (config) => {
  throw new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, undefined, {
    status: 401, statusText: 'Unauthorized', headers: {}, config, data: {},
  })
}

function saveCart() {
  localStorage.setItem(CART_STORAGE_KEY, JSON.stringify({
    items: [{ productId: 42, name: 'Mug', price: 34.99, quantity: 2 }],
    stockByProductId: { 42: 5 },
  }))
}

function saveSession(role: 'CUSTOMER' | 'ADMIN' = 'CUSTOMER') {
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify({
    accessToken: 'token',
    tokenType: 'Bearer',
    expiresIn: 3600,
    user: { id: 1, name: 'Ana', email: 'ana@example.com', role },
  }))
}

function LocationStateProbe() {
  const { pathname, state } = useLocation()
  return <div data-testid="route-state">{JSON.stringify({ pathname, state })}</div>
}

function renderCart() {
  return render(
    <MemoryRouter initialEntries={['/cart']}>
      <AuthProvider>
        <CartProvider><AppRouteTree /><LocationStateProbe /></CartProvider>
      </AuthProvider>
    </MemoryRouter>,
  )
}

beforeEach(() => {
  localStorage.clear()
  vi.clearAllMocks()
  vi.mocked(getOrderById).mockResolvedValue({
    id: 99, items: [], total: 69.98, createdAt: '2026-01-01T12:00:00Z',
  })
})

afterEach(() => {
  client.defaults.adapter = originalAdapter
  window.history.replaceState(null, '', '/')
})

it('shows an empty cart with a link to the catalog', () => {
  renderCart()
  expect(screen.getByText('Your cart is empty.')).toBeInTheDocument()
  expect(screen.getByRole('link', { name: 'Browse products' })).toHaveAttribute('href', '/')
  expect(screen.getByRole('link', { name: /Cart/ })).toHaveAttribute('href', '/cart')
})

it('calculates the total and updates quantities and the header badge', async () => {
  const user = userEvent.setup()
  saveCart()
  renderCart()

  expect(screen.getByText('$69.98')).toBeInTheDocument()
  expect(screen.getByLabelText('2 items in cart')).toBeInTheDocument()
  await user.click(screen.getByRole('button', { name: 'Increase quantity of Mug' }))
  expect(screen.getByText('$104.97')).toBeInTheDocument()
  expect(screen.getByLabelText('3 items in cart')).toBeInTheDocument()
  await user.click(screen.getByRole('button', { name: 'Remove Mug' }))
  expect(screen.getByText('Your cart is empty.')).toBeInTheDocument()
})

it('clears the cart and its stored contents on sign out', async () => {
  const user = userEvent.setup()
  saveCart()
  saveSession()
  renderCart()

  await user.click(screen.getByRole('button', { name: 'Sign out' }))

  expect(screen.getByText('Your cart is empty.')).toBeInTheDocument()
  expect(screen.getByLabelText('0 items in cart')).toBeInTheDocument()
  expect(localStorage.getItem(AUTH_STORAGE_KEY)).toBeNull()
  expect(JSON.parse(localStorage.getItem(CART_STORAGE_KEY) ?? 'null')).toEqual({
    items: [], stockByProductId: {},
  })
})

it('clears the authenticated cart synchronously when a request returns 401', async () => {
  saveCart()
  saveSession()
  renderCart()
  window.history.replaceState(null, '', '/login')
  client.defaults.adapter = unauthorizedAdapter
  let cartAtSessionClear: string | null = null
  const observeClear = () => { cartAtSessionClear = localStorage.getItem(CART_STORAGE_KEY) }
  window.addEventListener(AUTH_SESSION_CLEARED_EVENT, observeClear)

  try {
    await act(async () => {
      await expect(client.get('/orders')).rejects.toBeInstanceOf(AxiosError)
    })
  } finally {
    window.removeEventListener(AUTH_SESSION_CLEARED_EVENT, observeClear)
  }

  expect(JSON.parse(cartAtSessionClear ?? 'null')).toEqual({ items: [], stockByProductId: {} })
  expect(screen.getByText('Your cart is empty.')).toBeInTheDocument()
  expect(localStorage.getItem(AUTH_STORAGE_KEY)).toBeNull()
})

it('preserves the guest cart when sign-in returns 401', async () => {
  saveCart()
  renderCart()
  client.defaults.adapter = unauthorizedAdapter

  await act(async () => {
    await expect(client.post('/auth/login', {})).rejects.toBeInstanceOf(AxiosError)
  })

  expect(screen.getByText('$69.98')).toBeInTheDocument()
  expect(JSON.parse(localStorage.getItem(CART_STORAGE_KEY) ?? 'null').items).toEqual([
    { productId: 42, name: 'Mug', price: 34.99, quantity: 2 },
  ])
})

it('shows a 409 stock conflict and keeps the cart so the user can correct it', async () => {
  const user = userEvent.setup()
  saveCart()
  saveSession()
  vi.mocked(placeOrder).mockRejectedValue({
    isAxiosError: true,
    response: {
      status: 409,
      data: { detail: 'Insufficient stock.', productId: 42, requested: 2, available: 1 },
    },
  })
  renderCart()

  await user.click(screen.getByRole('button', { name: 'Place order' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('Mug: 1 available.')
  expect(screen.getByText('$69.98')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Place order' })).toBeDisabled()
  await user.click(screen.getByRole('button', { name: 'Decrease quantity of Mug' }))
  expect(screen.getByText('$34.99')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Place order' })).toBeEnabled()
})

it('identifies an unavailable product on 404 without clearing the cart', async () => {
  const user = userEvent.setup()
  saveCart()
  saveSession()
  vi.mocked(placeOrder).mockRejectedValue({
    isAxiosError: true,
    response: { status: 404, data: { detail: 'Product with id 42 was not found' } },
  })
  renderCart()

  await user.click(screen.getByRole('button', { name: 'Place order' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('Mug is unavailable. Please remove it')
  expect(screen.getByText('$69.98')).toBeInTheDocument()
})

it('identifies an unavailable product using the 404 response productId', async () => {
  const user = userEvent.setup()
  saveCart()
  saveSession()
  vi.mocked(placeOrder).mockRejectedValue({
    isAxiosError: true,
    response: { status: 404, data: { detail: 'A product is unavailable.', productId: 42 } },
  })
  renderCart()

  await user.click(screen.getByRole('button', { name: 'Place order' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('Mug is unavailable. Please remove it')
  expect(screen.getByText('$69.98')).toBeInTheDocument()
})

it('posts only product IDs and quantities, then clears the cart and opens the order', async () => {
  const user = userEvent.setup()
  saveCart()
  saveSession()
  vi.mocked(placeOrder).mockResolvedValue({ id: 99, items: [], total: 69.98, createdAt: '2026-01-01T00:00:00Z' })
  renderCart()

  await user.click(screen.getByRole('button', { name: 'Place order' }))
  expect(placeOrder).toHaveBeenCalledWith({ items: [{ productId: 42, quantity: 2 }] })
  expect(await screen.findByRole('heading', { name: 'Order #99' })).toBeInTheDocument()
  expect(screen.getByLabelText('0 items in cart')).toBeInTheDocument()
  expect(JSON.parse(localStorage.getItem(CART_STORAGE_KEY) ?? 'null').items).toEqual([])
})

it('redirects guests to login and returns them to their cart', async () => {
  const user = userEvent.setup()
  saveCart()
  vi.mocked(loginUser).mockResolvedValue({
    accessToken: 'token',
    tokenType: 'Bearer',
    expiresIn: 3600,
    user: { id: 1, name: 'Ana', email: 'ana@example.com', role: 'CUSTOMER' },
  })
  renderCart()

  await user.click(screen.getByRole('button', { name: 'Place order' }))
  expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
  expect(JSON.parse(screen.getByTestId('route-state').textContent ?? '{}')).toEqual({
    pathname: '/login', state: { from: '/cart' },
  })
  expect(placeOrder).not.toHaveBeenCalled()
  await user.type(screen.getByLabelText('Email address'), 'ana@example.com')
  await user.type(screen.getByLabelText('Password'), 'secret123')
  await user.click(screen.getByRole('button', { name: 'Sign in' }))
  expect(await screen.findByRole('heading', { name: 'Your cart' })).toBeInTheDocument()
  expect(screen.getByText('$69.98')).toBeInTheDocument()
  expect(JSON.parse(localStorage.getItem(CART_STORAGE_KEY) ?? 'null').items).toEqual([
    { productId: 42, name: 'Mug', price: 34.99, quantity: 2 },
  ])
  expect(placeOrder).not.toHaveBeenCalled()
  vi.mocked(placeOrder).mockResolvedValue({ id: 99, items: [], total: 69.98, createdAt: '2026-01-01T00:00:00Z' })
  await user.click(screen.getByRole('button', { name: 'Place order' }))
  expect(placeOrder).toHaveBeenCalledWith({ items: [{ productId: 42, quantity: 2 }] })
  expect(await screen.findByRole('heading', { name: 'Order #99' })).toBeInTheDocument()
})

it('returns to the cart when the session renders before the login submit navigates', async () => {
  const user = userEvent.setup()
  saveCart()
  vi.mocked(loginUser).mockResolvedValue({
    accessToken: 'token', tokenType: 'Bearer', expiresIn: 3600,
    user: { id: 1, name: 'Ana', email: 'ana@example.com', role: 'CUSTOMER' },
  })
  let finishLogin = () => {}
  const loginCompletion = new Promise<void>((resolve) => { finishLogin = resolve })
  function DelayedAuthProvider({ children }: PropsWithChildren) {
    const [session, setSession] = useState<AuthSession | null>(null)
    const login = async (credentials: LoginRequest) => {
      setSession(await loginUser(credentials))
      await loginCompletion
    }
    return (
      <AuthContext.Provider value={{
        session, isAuthenticated: session !== null, login, logout: () => setSession(null),
      }}>
        {children}
      </AuthContext.Provider>
    )
  }
  render(
    <MemoryRouter initialEntries={['/cart']}>
      <DelayedAuthProvider>
        <CartProvider><AppRouteTree /><LocationStateProbe /></CartProvider>
      </DelayedAuthProvider>
    </MemoryRouter>,
  )

  await user.click(screen.getByRole('button', { name: 'Place order' }))
  expect(placeOrder).not.toHaveBeenCalled()
  expect(JSON.parse(screen.getByTestId('route-state').textContent ?? '{}')).toEqual({
    pathname: '/login', state: { from: '/cart' },
  })
  await user.type(screen.getByLabelText('Email address'), 'ana@example.com')
  await user.type(screen.getByLabelText('Password'), 'secret123')
  await user.click(screen.getByRole('button', { name: 'Sign in' }))

  expect(await screen.findByRole('heading', { name: 'Your cart' })).toBeInTheDocument()
  await act(async () => finishLogin())
  expect(JSON.parse(screen.getByTestId('route-state').textContent ?? '{}').pathname).toBe('/cart')
  expect(screen.getByText('$69.98')).toBeInTheDocument()
})

it('returns a newly registered customer to the unchanged cart', async () => {
  const user = userEvent.setup()
  saveCart()
  vi.mocked(registerUser).mockResolvedValue({ id: 1, name: 'Ana', email: 'ana@example.com', role: 'CUSTOMER' })
  vi.mocked(loginUser).mockResolvedValue({
    accessToken: 'token', tokenType: 'Bearer', expiresIn: 3600,
    user: { id: 1, name: 'Ana', email: 'ana@example.com', role: 'CUSTOMER' },
  })
  renderCart()

  await user.click(screen.getByRole('button', { name: 'Place order' }))
  await user.click(screen.getByRole('link', { name: 'Create an account' }))
  await user.type(screen.getByLabelText('Name'), 'Ana')
  await user.type(screen.getByLabelText('Email address'), 'ana@example.com')
  await user.type(screen.getByLabelText('Password'), 'secret123')
  await user.click(screen.getByRole('button', { name: 'Create account' }))

  expect(await screen.findByRole('heading', { name: 'Your cart' })).toBeInTheDocument()
  expect(screen.getByText('$69.98')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Place order' })).toBeEnabled()
  expect(JSON.parse(localStorage.getItem(CART_STORAGE_KEY) ?? 'null').items).toEqual([
    { productId: 42, name: 'Mug', price: 34.99, quantity: 2 },
  ])
  expect(placeOrder).not.toHaveBeenCalled()
})

it('denies admin access to the cart and hides its header link', () => {
  saveCart()
  saveSession('ADMIN')
  renderCart()

  expect(screen.getByRole('heading', { name: 'Access denied' })).toBeInTheDocument()
  expect(screen.getByText('Only customer accounts can view the cart.')).toBeInTheDocument()
  expect(screen.queryByRole('link', { name: /Cart/ })).not.toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Place order' })).not.toBeInTheDocument()
  expect(placeOrder).not.toHaveBeenCalled()
})

it('clears a guest cart when signing in as an admin', async () => {
  const user = userEvent.setup()
  saveCart()
  vi.mocked(loginUser).mockResolvedValue({
    accessToken: 'token', tokenType: 'Bearer', expiresIn: 3600,
    user: { id: 1, name: 'Admin', email: 'admin@example.com', role: 'ADMIN' },
  })
  renderCart()

  await user.click(screen.getByRole('button', { name: 'Place order' }))
  await user.type(screen.getByLabelText('Email address'), 'admin@example.com')
  await user.type(screen.getByLabelText('Password'), 'secret123')
  await user.click(screen.getByRole('button', { name: 'Sign in' }))

  expect(await screen.findByRole('heading', { name: 'Access denied' })).toBeInTheDocument()
  expect(screen.queryByRole('link', { name: /Cart/ })).not.toBeInTheDocument()
  expect(JSON.parse(localStorage.getItem(CART_STORAGE_KEY) ?? 'null')).toEqual({
    items: [], stockByProductId: {},
  })
  expect(placeOrder).not.toHaveBeenCalled()
  await user.click(screen.getByRole('button', { name: 'Sign out' }))
  expect(screen.getByText('Your cart is empty.')).toBeInTheDocument()
})
