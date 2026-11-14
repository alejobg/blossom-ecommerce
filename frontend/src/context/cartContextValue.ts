import { createContext } from 'react'
import type { CartItem } from '../types/cart'
import type { Product } from '../types/product'

export interface CartContextValue {
  items: CartItem[]
  stockByProductId: Record<string, number>
  total: number
  itemCount: number
  storageError: string
  add: (product: Product) => string | null
  remove: (productId: number) => void
  updateQuantity: (productId: number, quantity: number) => string | null
  updateStock: (productId: number, stock: number) => void
  clear: () => void
}

export const CartContext = createContext<CartContextValue | undefined>(undefined)
