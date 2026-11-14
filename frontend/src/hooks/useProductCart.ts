import { useCart } from './useCart'
import { MAX_ITEM_QUANTITY } from '../types/cart'
import type { Product } from '../types/product'

export function useProductCart(product: Product) {
  const { add, items } = useCart()
  const quantity = items.find((item) => item.productId === product.id)?.quantity ?? 0

  return {
    quantity,
    limitReached: quantity >= Math.min(product.stock, MAX_ITEM_QUANTITY),
    addProduct: () => add(product),
  }
}
