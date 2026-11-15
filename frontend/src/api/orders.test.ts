import { afterEach, expect, it } from 'vitest'
import client from './client'
import { getOrderById, listOrders } from './orders'
import { AUTH_STORAGE_KEY } from './sessionStorage'
import type { OrderResponse } from '../types/order'

const originalAdapter = client.defaults.adapter
const order: OrderResponse = {
  id: 99,
  items: [{ productId: 1, productName: 'Mug', unitPrice: 18.5, quantity: 2, subtotal: 37 }],
  total: 37,
  createdAt: '2026-01-15T12:00:00Z',
}

afterEach(() => {
  client.defaults.adapter = originalAdapter
})

it('requests a page of orders with the session token', async () => {
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify({
    accessToken: 'token', tokenType: 'Bearer', expiresIn: 3600,
    user: { id: 1, name: 'Ana', email: 'ana@example.com', role: 'CUSTOMER' },
  }))
  const result = { content: [order], page: 1, size: 20, totalElements: 21, totalPages: 2 }
  client.defaults.adapter = async (config) => {
    expect(config.url).toBe('/orders')
    expect(config.method).toBe('get')
    expect(config.params).toEqual({ page: 1, size: 20 })
    expect(config.headers.Authorization).toBe('Bearer token')
    return { data: result, status: 200, statusText: 'OK', headers: {}, config }
  }

  expect(await listOrders(1, 20)).toEqual(result)
})

it('requests the detail by its encoded order ID', async () => {
  client.defaults.adapter = async (config) => {
    expect(config.url).toBe('/orders/99')
    expect(config.method).toBe('get')
    return { data: order, status: 200, statusText: 'OK', headers: {}, config }
  }

  expect(await getOrderById('99')).toEqual(order)
})
