import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { getOrderById, listOrders } from '../api/orders'
import { AUTH_STORAGE_KEY } from '../api/sessionStorage'
import { AuthProvider } from '../context/AuthContext'
import { CartProvider } from '../context/CartContext'
import { AppRouteTree } from '../routes/AppRoutes'
import { ProtectedRoute } from '../routes/ProtectedRoute'
import type { OrderResponse } from '../types/order'

vi.mock('../api/orders', () => ({
  getOrderById: vi.fn(),
  listOrders: vi.fn(),
  placeOrder: vi.fn(),
}))

const order: OrderResponse = {
  id: 99,
  createdAt: '2026-01-15T12:00:00Z',
  total: 45.48,
  items: [
    { productId: 1, productName: 'Ceramic mug', unitPrice: 18.5, quantity: 2, subtotal: 37 },
    { productId: 2, productName: 'Tea', unitPrice: 8.48, quantity: 1, subtotal: 8.48 },
  ],
}

function saveSession(role: 'CUSTOMER' | 'ADMIN' = 'CUSTOMER') {
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify({
    accessToken: 'token', tokenType: 'Bearer', expiresIn: 3600,
    user: { id: 1, name: 'Ana', email: 'ana@example.com', role },
  }))
}

function LocationProbe() {
  const { pathname, search, hash, state } = useLocation()
  return <div data-testid="location">{JSON.stringify({ pathname, search, hash, state })}</div>
}

function renderOrders(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider>
        <CartProvider><AppRouteTree /><LocationProbe /></CartProvider>
      </AuthProvider>
    </MemoryRouter>,
  )
}

beforeEach(() => {
  vi.resetAllMocks()
  localStorage.clear()
})

it('shows loading and empty states in the order history', async () => {
  saveSession()
  vi.mocked(listOrders).mockImplementation(() => new Promise(() => undefined))
  const { unmount } = renderOrders('/orders')
  expect(screen.getByRole('status')).toHaveTextContent('Loading orders')
  expect(listOrders).toHaveBeenCalledWith(0, 20)
  unmount()

  vi.mocked(listOrders).mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
  renderOrders('/orders')
  expect(await screen.findByText('You have no orders yet.')).toBeInTheDocument()
  expect(screen.getByRole('link', { name: 'Browse products' })).toHaveAttribute('href', '/')
})

it('shows the backend history error and allows retrying', async () => {
  const user = userEvent.setup()
  saveSession()
  vi.mocked(listOrders).mockRejectedValueOnce({
    isAxiosError: true, response: { status: 500, data: { detail: 'Orders are unavailable.' } },
  }).mockResolvedValueOnce({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
  renderOrders('/orders')

  expect(await screen.findByRole('alert')).toHaveTextContent('Orders are unavailable.')
  await user.click(screen.getByRole('button', { name: 'Try again' }))
  expect(await screen.findByText('You have no orders yet.')).toBeInTheDocument()
  expect(listOrders).toHaveBeenCalledTimes(2)
})

it('shows orders in backend order with item counts and paginates', async () => {
  const user = userEvent.setup()
  saveSession()
  vi.mocked(listOrders).mockImplementation(async (page, size) => ({
    content: page === 0 ? [order, { ...order, id: 98, createdAt: '2025-12-10T12:00:00Z' }] : [{ ...order, id: 97 }],
    page, size, totalElements: 3, totalPages: 2,
  }))
  renderOrders('/orders')

  const links = await screen.findAllByRole('link', { name: /^Order #/ })
  expect(links.map((link) => link.textContent)).toEqual(['Order #99', 'Order #98'])
  const first = links[0].closest('li')
  expect(first).not.toBeNull()
  if (!first) throw new Error('Order list item not found')
  expect(within(first).getByText(/3 items/)).toBeInTheDocument()
  expect(within(first).getByText('$45.48')).toBeInTheDocument()
  expect(within(first).getByText('Jan 15, 2026')).toHaveAttribute('datetime', order.createdAt)
  expect(links[0]).toHaveAttribute('href', '/orders/99')
  expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled()

  await user.click(screen.getByRole('button', { name: 'Next' }))
  expect(await screen.findByRole('link', { name: 'Order #97' })).toBeInTheDocument()
  expect(listOrders).toHaveBeenLastCalledWith(1, 20)
  expect(screen.getByText('Page 2 of 2')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled()
  await user.click(screen.getByRole('button', { name: 'Previous' }))
  expect(await screen.findByRole('link', { name: 'Order #99' })).toBeInTheDocument()
  expect(listOrders).toHaveBeenLastCalledWith(0, 20)
})

it('shows loading and the stored order items and prices', async () => {
  saveSession()
  vi.mocked(getOrderById).mockImplementation(() => new Promise(() => undefined))
  const { unmount } = renderOrders('/orders/99')
  expect(screen.getByRole('status')).toHaveTextContent('Loading order')
  expect(getOrderById).toHaveBeenCalledWith('99')
  unmount()

  vi.mocked(getOrderById).mockResolvedValue(order)
  renderOrders('/orders/99')
  expect(await screen.findByRole('heading', { name: 'Order #99' })).toBeInTheDocument()
  expect(screen.getByText('Jan 15, 2026')).toHaveAttribute('datetime', order.createdAt)
  expect(screen.getByRole('heading', { name: 'Ceramic mug' })).toBeInTheDocument()
  expect(screen.getByText('$18.50 each · Quantity: 2')).toBeInTheDocument()
  expect(screen.getByText('Subtotal: $37.00')).toBeInTheDocument()
  expect(screen.getByText('$8.48 each · Quantity: 1')).toBeInTheDocument()
  expect(screen.getByText('Subtotal: $8.48')).toBeInTheDocument()
  expect(screen.getByText('Total: $45.48')).toBeInTheDocument()
  expect(screen.getByRole('link', { name: 'Back to my orders' })).toHaveAttribute('href', '/orders')
})

it('shows a clear 404 for a missing or inaccessible order', async () => {
  saveSession()
  vi.mocked(getOrderById).mockRejectedValue({
    isAxiosError: true, response: { status: 404, data: { detail: 'Not found' } },
  })
  renderOrders('/orders/99')
  expect(await screen.findByRole('alert')).toHaveTextContent('Order not found')
  expect(screen.queryByRole('button', { name: 'Try again' })).not.toBeInTheDocument()
})

it('shows other detail errors and retries', async () => {
  const user = userEvent.setup()
  saveSession()
  vi.mocked(getOrderById).mockRejectedValueOnce({
    isAxiosError: true, response: { status: 500, data: { detail: 'Please try later.' } },
  }).mockResolvedValueOnce(order)
  renderOrders('/orders/99')

  expect(await screen.findByRole('alert')).toHaveTextContent('Please try later.')
  await user.click(screen.getByRole('button', { name: 'Try again' }))
  expect(await screen.findByRole('heading', { name: 'Order #99' })).toBeInTheDocument()
  expect(getOrderById).toHaveBeenCalledTimes(2)
})

it('shows an empty state when an order has no items', async () => {
  saveSession()
  vi.mocked(getOrderById).mockResolvedValue({ ...order, items: [] })
  renderOrders('/orders/99')
  expect(await screen.findByText('This order has no items.')).toBeInTheDocument()
})

it.each(['/orders?ref=header#history', '/orders/99?ref=cart#detail'])(
  'redirects guests from %s to sign-in without fetching orders',
  (path) => {
    renderOrders(path)
    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
    expect(JSON.parse(screen.getByTestId('location').textContent ?? '{}').state).toEqual({ from: path })
    expect(screen.queryByRole('link', { name: 'My orders' })).not.toBeInTheDocument()
    expect(listOrders).not.toHaveBeenCalled()
    expect(getOrderById).not.toHaveBeenCalled()
  },
)

it.each(['/orders', '/orders/99'])('blocks admin access to %s without fetching orders', (path) => {
  saveSession('ADMIN')
  renderOrders(path)
  expect(screen.getByRole('heading', { name: 'Access denied' })).toBeInTheDocument()
  expect(screen.getByText('Only customer accounts can view orders.')).toBeInTheDocument()
  expect(screen.queryByRole('link', { name: 'My orders' })).not.toBeInTheDocument()
  expect(listOrders).not.toHaveBeenCalled()
  expect(getOrderById).not.toHaveBeenCalled()
})

it('displays the access-denied message supplied to the protected route', () => {
  saveSession('ADMIN')
  render(
    <MemoryRouter initialEntries={['/restricted']}>
      <AuthProvider>
        <Routes>
          <Route element={<ProtectedRoute allowedRole="CUSTOMER" deniedMessage="Custom access message." />}>
            <Route element={<p>Restricted content</p>} path="/restricted" />
          </Route>
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )

  expect(screen.getByRole('heading', { name: 'Access denied' })).toBeInTheDocument()
  expect(screen.getByText('Custom access message.')).toBeInTheDocument()
  expect(screen.queryByText('Restricted content')).not.toBeInTheDocument()
})

it('shows the orders link for customers and opens the history from the detail', async () => {
  const user = userEvent.setup()
  saveSession()
  vi.mocked(getOrderById).mockResolvedValue(order)
  vi.mocked(listOrders).mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
  renderOrders('/orders/99')

  const link = screen.getByRole('link', { name: 'My orders' })
  expect(link).toHaveAttribute('href', '/orders')
  await user.click(link)
  expect(await screen.findByText('You have no orders yet.')).toBeInTheDocument()
  await waitFor(() => expect(listOrders).toHaveBeenCalledWith(0, 20))
})
