import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { z } from 'zod'
import { AUTH_SESSION_CLEARED_EVENT, AUTH_SESSION_SAVED_EVENT, getStoredAuthSession } from '../api/sessionStorage'
import { CART_STORAGE_KEY, MAX_CART_ITEMS, MAX_ITEM_QUANTITY, type CartItem } from '../types/cart'
import type { Product } from '../types/product'
import { canPurchase } from '../utils/canPurchase'
import { CartContext } from './cartContextValue'

const cartSchema = z.object({
  items: z.array(z.object({
    productId: z.number().int().positive(),
    name: z.string().min(1),
    price: z.number().finite().nonnegative(),
    quantity: z.number().int().min(1).max(MAX_ITEM_QUANTITY),
  })).max(MAX_CART_ITEMS),
  stockByProductId: z.record(z.string(), z.number().int().nonnegative()),
}).refine(
  (cart) => new Set(cart.items.map((item) => item.productId)).size === cart.items.length &&
    cart.items.every((item) => cart.stockByProductId[item.productId] !== undefined),
)

type CartState = z.infer<typeof cartSchema>

function readCart(): { cart: CartState; error: string } {
  const empty = { items: [], stockByProductId: {} }
  try {
    const stored = localStorage.getItem(CART_STORAGE_KEY)
    if (!stored) return { cart: empty, error: '' }
    const result = cartSchema.safeParse(JSON.parse(stored))
    if (result.success) return { cart: result.data, error: '' }
    console.error('Stored cart has invalid data', result.error)
  } catch (error) {
    console.error('Unable to load stored cart', error)
  }
  return { cart: empty, error: 'Unable to load the saved cart.' }
}

export function CartProvider({ children }: { children: ReactNode }) {
  const [initial] = useState(readCart)
  const [cart, setCart] = useState<CartState>(initial.cart)
  const [storageError, setStorageError] = useState(initial.error)
  const cartRef = useRef(cart)

  const save = useCallback((next: CartState) => {
    cartRef.current = next
    setCart(next)
    try {
      localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(next))
      setStorageError('')
    } catch (error) {
      console.error('Unable to save cart', error)
      setStorageError('Unable to save your cart. Changes may not persist after a reload.')
    }
  }, [])

  const add = useCallback((product: Product): string | null => {
    const current = cartRef.current
    const existing = current.items.find((item) => item.productId === product.id)
    const quantity = (existing?.quantity ?? 0) + 1
    if (existing && current.stockByProductId[product.id] !== product.stock) {
      save({ ...current, stockByProductId: { ...current.stockByProductId, [product.id]: product.stock } })
    }
    if (quantity > product.stock) return 'Cannot add more than the available stock.'
    if (quantity > MAX_ITEM_QUANTITY) return 'A product cannot exceed 1,000 units per order.'
    if (!existing && current.items.length >= MAX_CART_ITEMS) {
      return 'A cart cannot contain more than 50 different products.'
    }
    const item: CartItem = {
      productId: product.id,
      name: product.name,
      price: product.price,
      quantity,
    }
    save({
      items: existing
        ? current.items.map((entry) => entry.productId === product.id ? item : entry)
        : [...current.items, item],
      stockByProductId: { ...current.stockByProductId, [product.id]: product.stock },
    })
    return null
  }, [save])

  const remove = useCallback((productId: number) => {
    const current = cartRef.current
    const stockByProductId = { ...current.stockByProductId }
    delete stockByProductId[productId]
    save({ items: current.items.filter((item) => item.productId !== productId), stockByProductId })
  }, [save])

  const updateQuantity = useCallback((productId: number, quantity: number): string | null => {
    const current = cartRef.current
    const item = current.items.find((entry) => entry.productId === productId)
    if (!item) return 'This product is no longer in the cart.'
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > MAX_ITEM_QUANTITY) {
      return 'Quantity must be between 1 and 1,000.'
    }
    if (quantity > current.stockByProductId[productId]) {
      return 'Quantity cannot exceed the available stock.'
    }
    save({
      ...current,
      items: current.items.map((entry) => entry.productId === productId ? { ...entry, quantity } : entry),
    })
    return null
  }, [save])

  const updateStock = useCallback((productId: number, stock: number) => {
    const current = cartRef.current
    if (!current.items.some((item) => item.productId === productId) ||
      current.stockByProductId[productId] === stock) return
    save({ ...current, stockByProductId: { ...current.stockByProductId, [productId]: stock } })
  }, [save])

  const clear = useCallback(() => save({ items: [], stockByProductId: {} }), [save])

  useEffect(() => {
    const handleSessionSaved = () => {
      if (!canPurchase(getStoredAuthSession())) clear()
    }
    const handleSessionCleared = (event: Event) => {
      if (!(event instanceof CustomEvent)) return
      const detail: unknown = event.detail
      if (typeof detail === 'object' && detail !== null &&
          'hadSession' in detail && detail.hadSession === true) clear()
    }
    window.addEventListener(AUTH_SESSION_SAVED_EVENT, handleSessionSaved)
    window.addEventListener(AUTH_SESSION_CLEARED_EVENT, handleSessionCleared)
    return () => {
      window.removeEventListener(AUTH_SESSION_SAVED_EVENT, handleSessionSaved)
      window.removeEventListener(AUTH_SESSION_CLEARED_EVENT, handleSessionCleared)
    }
  }, [clear])

  const value = useMemo(() => ({
    ...cart,
    total: cart.items.reduce((sum, item) => sum + Math.round(item.price * 100) * item.quantity, 0) / 100,
    itemCount: cart.items.reduce((sum, item) => sum + item.quantity, 0),
    storageError,
    add,
    remove,
    updateQuantity,
    updateStock,
    clear,
  }), [cart, storageError, add, remove, updateQuantity, updateStock, clear])

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}
