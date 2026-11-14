import client from './client'
import type { OrderResponse, PlaceOrderRequest } from '../types/order'

export async function placeOrder(request: PlaceOrderRequest): Promise<OrderResponse> {
  const response = await client.post<OrderResponse>('/orders', request)
  return response.data
}
