import { act, fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { AUTH_STORAGE_KEY } from '../api/sessionStorage'
import { AuthProvider } from '../context/AuthContext'
import { CartProvider } from '../context/CartContext'
import { CART_STORAGE_KEY } from '../types/cart'
import type { Product } from '../types/product'
import { ProductCard } from './ProductCard'

const product: Product = {
  id: 12, name: 'Ceramic mug', description: 'A handmade mug', price: 18.5, stock: 6, category: 'HOME',
}

function LocationProbe() {
  return <div data-testid="location">{useLocation().pathname}</div>
}

function renderCard(currentProduct = product) {
  return render(
    <MemoryRouter initialEntries={['/']}>
      <AuthProvider>
        <CartProvider>
          <Routes>
            <Route element={<ProductCard product={currentProduct} />} path="/" />
            <Route element={<h1>Product detail</h1>} path="/products/:id" />
          </Routes>
          <LocationProbe />
        </CartProvider>
      </AuthProvider>
    </MemoryRouter>,
  )
}

afterEach(() => {
  vi.useRealTimers()
})

it('uses the product title as the stretched link and supports keyboard navigation', async () => {
  const user = userEvent.setup()
  renderCard()

  const link = screen.getByRole('link', { name: 'Ceramic mug' })
  expect(link).toHaveAttribute('href', '/products/12')
  expect(link).toHaveClass('after:absolute', 'after:inset-0')
  expect(link).not.toContainElement(screen.getByRole('button', { name: 'Add to cart' }))
  await user.tab()
  expect(link).toHaveFocus()
  await user.keyboard('{Enter}')
  expect(screen.getByRole('heading', { name: 'Product detail' })).toBeInTheDocument()
})

it('adds one item without navigating and resets feedback after the latest click', () => {
  vi.useFakeTimers()
  renderCard()

  fireEvent.click(screen.getByRole('button', { name: 'Add to cart' }))
  expect(screen.getByTestId('location')).toHaveTextContent(/^\/$/)
  expect(screen.getByRole('button', { name: 'Added ✓' })).toHaveClass('relative', 'z-10')
  expect(screen.getByRole('status')).toHaveTextContent('Added to your cart.')
  expect(JSON.parse(localStorage.getItem(CART_STORAGE_KEY) ?? 'null').items).toEqual([
    { productId: 12, name: 'Ceramic mug', price: 18.5, quantity: 1 },
  ])

  act(() => vi.advanceTimersByTime(1000))
  fireEvent.click(screen.getByRole('button', { name: 'Added ✓' }))
  act(() => vi.advanceTimersByTime(1000))
  expect(screen.getByRole('button', { name: 'Added ✓' })).toBeInTheDocument()
  expect(JSON.parse(localStorage.getItem(CART_STORAGE_KEY) ?? 'null').items[0].quantity).toBe(2)
  act(() => vi.advanceTimersByTime(500))
  expect(screen.getByRole('button', { name: 'Add to cart' })).toBeEnabled()
})

it('disables adding an out-of-stock product', () => {
  renderCard({ ...product, stock: 0 })
  expect(screen.getByRole('button', { name: 'Out of stock' })).toBeDisabled()
  expect(screen.getByRole('link', { name: 'Ceramic mug' })).toHaveAttribute('href', '/products/12')
})

it('disables adding when the cart reaches the available stock', async () => {
  const user = userEvent.setup()
  renderCard({ ...product, stock: 1 })

  await user.click(screen.getByRole('button', { name: 'Add to cart' }))
  expect(screen.getByRole('button', { name: 'Added ✓' })).toBeDisabled()
  expect(JSON.parse(localStorage.getItem(CART_STORAGE_KEY) ?? 'null').items[0].quantity).toBe(1)
})

it('also respects the maximum order quantity when stock is higher', () => {
  localStorage.setItem(CART_STORAGE_KEY, JSON.stringify({
    items: [{ productId: 12, name: 'Ceramic mug', price: 18.5, quantity: 1000 }],
    stockByProductId: { 12: 2000 },
  }))
  renderCard({ ...product, stock: 2000 })

  expect(screen.getByRole('button', { name: 'Add to cart' })).toBeDisabled()
})

it('shows cart errors instead of success feedback', async () => {
  const user = userEvent.setup()
  const items = Array.from({ length: 50 }, (_, index) => ({
    productId: index + 100, name: `Product ${index + 100}`, price: 1, quantity: 1,
  }))
  localStorage.setItem(CART_STORAGE_KEY, JSON.stringify({
    items, stockByProductId: Object.fromEntries(items.map((item) => [item.productId, 1])),
  }))
  renderCard()

  await user.click(screen.getByRole('button', { name: 'Add to cart' }))
  expect(screen.getByRole('alert')).toHaveTextContent('A cart cannot contain more than 50 different products.')
  expect(screen.getByRole('button', { name: 'Add to cart' })).toBeEnabled()
  expect(screen.getByTestId('location')).toHaveTextContent(/^\/$/)
})

it.each([
  { role: 'ADMIN', visible: false },
  { role: 'CUSTOMER', visible: true },
] as const)('shows purchase controls for $role: $visible', ({ role, visible }) => {
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify({
    accessToken: 'token', tokenType: 'Bearer', expiresIn: 3600,
    user: { id: 1, name: 'Ana', email: 'ana@example.com', role },
  }))
  renderCard()

  expect(screen.getByRole('link', { name: 'Ceramic mug' })).toHaveAttribute('href', '/products/12')
  expect(screen.queryByRole('button', { name: 'Add to cart' }) !== null).toBe(visible)
})
