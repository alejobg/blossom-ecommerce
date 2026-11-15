import { useCart } from './useCart'
import { useAuth } from './useAuth'
import { MAX_ITEM_QUANTITY } from '../types/cart'
import type { Product } from '../types/product'
import { canPurchase } from '../utils/canPurchase'

export function useProductCart(product: Product) {
  const { add, items } = useCart()
  const { session } = useAuth()
  const quantity = items.find((item) => item.productId === product.id)?.quantity ?? 0

  return {
    quantity,
    canPurchase: canPurchase(session),
    limitReached: quantity >= Math.min(product.stock, MAX_ITEM_QUANTITY),
    addProduct: () => add(product),
  }
}
