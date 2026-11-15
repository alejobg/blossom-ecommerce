import client from './client'
import type { OrderResponse, PlaceOrderRequest } from '../types/order'
import type { PagedResponse } from '../types/pagination'

export async function placeOrder(request: PlaceOrderRequest): Promise<OrderResponse> {
  const response = await client.post<OrderResponse>('/orders', request)
  return response.data
}

export async function listOrders(page: number, size: number): Promise<PagedResponse<OrderResponse>> {
  const response = await client.get<PagedResponse<OrderResponse>>('/orders', { params: { page, size } })
  return response.data
}

export async function getOrderById(id: string): Promise<OrderResponse> {
  const response = await client.get<OrderResponse>(`/orders/${encodeURIComponent(id)}`)
  return response.data
}
