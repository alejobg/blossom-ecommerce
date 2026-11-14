import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { expect, it } from 'vitest'
import { CartProvider } from './CartContext'
import { useCart } from '../hooks/useCart'
import { CART_STORAGE_KEY } from '../types/cart'
import type { Product } from '../types/product'

const product: Product = {
  id: 1, name: 'Mug', description: '', category: 'HOME', price: 34.99, stock: 2,
}

function CartProbe({ currentProduct = product }: { currentProduct?: Product }) {
  const { items, add, remove, updateQuantity, total } = useCart()
  const [message, setMessage] = useState('')
  return (
    <>
      <p>Quantity: {items[0]?.quantity ?? 0}</p>
      <p>Total: {total}</p>
      <p role="status">{message}</p>
      <button onClick={() => setMessage(add(currentProduct) ?? '')}>Add</button>
      <button onClick={() => setMessage(updateQuantity(1, 3) ?? '')}>Set quantity to 3</button>
      <button onClick={() => setMessage(updateQuantity(1, 1001) ?? '')}>Set quantity to 1001</button>
      <button onClick={() => remove(1)}>Remove</button>
    </>
  )
}

it('adds items, enforces stock, removes items, and persists the cart across remounts', async () => {
  const user = userEvent.setup()
  const { unmount } = render(<CartProvider><CartProbe /></CartProvider>)

  await user.click(screen.getByRole('button', { name: 'Add' }))
  await user.click(screen.getByRole('button', { name: 'Add' }))
  expect(screen.getByText('Quantity: 2')).toBeInTheDocument()
  expect(screen.getByText('Total: 69.98')).toBeInTheDocument()

  await user.click(screen.getByRole('button', { name: 'Add' }))
  expect(screen.getByRole('status')).toHaveTextContent('Cannot add more than the available stock.')
  await user.click(screen.getByRole('button', { name: 'Set quantity to 3' }))
  expect(screen.getByRole('status')).toHaveTextContent('Quantity cannot exceed the available stock.')
  expect(screen.getByText('Quantity: 2')).toBeInTheDocument()

  unmount()
  render(<CartProvider><CartProbe /></CartProvider>)
  expect(screen.getByText('Quantity: 2')).toBeInTheDocument()
  expect(JSON.parse(localStorage.getItem(CART_STORAGE_KEY) ?? 'null')).toEqual({
    items: [{ productId: 1, name: 'Mug', price: 34.99, quantity: 2 }],
    stockByProductId: { 1: 2 },
  })

  await user.click(screen.getByRole('button', { name: 'Remove' }))
  expect(screen.getByText('Quantity: 0')).toBeInTheDocument()
  expect(JSON.parse(localStorage.getItem(CART_STORAGE_KEY) ?? 'null').items).toEqual([])
})

it('enforces the order limit of 1,000 units even when more stock exists', async () => {
  const user = userEvent.setup()
  localStorage.setItem(CART_STORAGE_KEY, JSON.stringify({
    items: [{ productId: 1, name: 'Mug', price: 34.99, quantity: 1000 }],
    stockByProductId: { 1: 2000 },
  }))
  render(<CartProvider><CartProbe currentProduct={{ ...product, stock: 2000 }} /></CartProvider>)

  await user.click(screen.getByRole('button', { name: 'Add' }))
  expect(screen.getByRole('status')).toHaveTextContent('A product cannot exceed 1,000 units per order.')
  await user.click(screen.getByRole('button', { name: 'Set quantity to 1001' }))
  expect(screen.getByRole('status')).toHaveTextContent('Quantity must be between 1 and 1,000.')
  expect(screen.getByText('Quantity: 1000')).toBeInTheDocument()
})

it('rejects a 51st distinct product without changing the saved cart', async () => {
  const user = userEvent.setup()
  const items = Array.from({ length: 50 }, (_, index) => ({
    productId: index + 2, name: `Product ${index + 2}`, price: 1, quantity: 1,
  }))
  const stockByProductId = Object.fromEntries(items.map((item) => [item.productId, 5]))
  localStorage.setItem(CART_STORAGE_KEY, JSON.stringify({ items, stockByProductId }))
  render(<CartProvider><CartProbe /></CartProvider>)

  await user.click(screen.getByRole('button', { name: 'Add' }))
  expect(screen.getByRole('status')).toHaveTextContent('A cart cannot contain more than 50 different products.')
  expect(JSON.parse(localStorage.getItem(CART_STORAGE_KEY) ?? 'null').items).toHaveLength(50)
})
