export interface CartItem {
  productId: number
  name: string
  price: number
  quantity: number
}

export const MAX_CART_ITEMS = 50
export const MAX_ITEM_QUANTITY = 1000
export const CART_STORAGE_KEY = 'blossom.cart'
