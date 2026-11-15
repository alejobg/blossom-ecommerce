import axios from 'axios'
import type { CartItem } from '../types/cart'
import type { ApiProblem } from '../types/auth'
import { getApiProblem } from './problem'

interface OrderProblem extends ApiProblem {
  productId?: number
  available?: number
}

export function getOrderProblem(error: unknown, items: CartItem[]): {
  message: string
  stock?: { productId: number; available: number }
} {
  const detail = getApiProblem(error).detail
  if (!axios.isAxiosError<OrderProblem>(error) || !error.response) return { message: detail }

  const { status, data } = error.response
  if (status === 409 && Number.isSafeInteger(data?.productId) &&
      Number.isInteger(data?.available) && (data.available ?? -1) >= 0 &&
      data.productId !== undefined && data.available !== undefined) {
    const item = items.find((entry) => entry.productId === data.productId)
    const name = item?.name ?? `Product #${data.productId}`
    return {
      message: `${detail} ${name}: ${data.available} available. Adjust the quantity or remove the item.`,
      stock: { productId: data.productId, available: data.available },
    }
  }
  if (status === 404) {
    const productId = data?.productId
    if (typeof productId === 'number' && Number.isSafeInteger(productId) && productId > 0) {
      const item = items.find((entry) => entry.productId === productId)
      const name = item?.name ?? `Product #${productId}`
      return { message: `${detail} ${name} is unavailable. Please remove it from your cart.` }
    }
    return { message: `${detail} Please remove the unavailable product from your cart.` }
  }
  return { message: detail }
}
