export interface PlaceOrderRequest {
  items: { productId: number; quantity: number }[]
}

export interface OrderResponse {
  id: number
  items: {
    productId: number
    productName: string
    unitPrice: number
    quantity: number
    subtotal: number
  }[]
  total: number
  createdAt: string
}
