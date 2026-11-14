import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useCart } from '../hooks/useCart'
import { MAX_ITEM_QUANTITY } from '../types/cart'
import type { Product } from '../types/product'

export function AddToCart({ product }: { product: Product }) {
  const { add, items } = useCart()
  const [message, setMessage] = useState('')
  const quantity = items.find((item) => item.productId === product.id)?.quantity ?? 0
  const limit = Math.min(product.stock, MAX_ITEM_QUANTITY)

  const handleAdd = () => {
    const error = add(product)
    setMessage(error ?? 'Added to your cart.')
  }

  return (
    <>
      <button
        className="mt-8 w-full rounded-xl bg-blossom-600 px-5 py-3.5 text-sm font-semibold text-white hover:bg-blossom-700 disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto"
        disabled={quantity >= limit}
        onClick={handleAdd}
        type="button"
      >
        Add to cart
      </button>
      {quantity >= limit && (
        <p className="mt-3 text-sm text-slate-500">
          {product.stock === 0 ? 'Out of stock.' : 'The maximum available quantity is in your cart.'}
        </p>
      )}
      {message && <p className="mt-3 text-sm text-blossom-700" role="status">{message}</p>}
      {quantity > 0 && (
        <Link className="mt-3 text-sm font-semibold text-blossom-700 underline" to="/cart">
          View cart
        </Link>
      )}
    </>
  )
}
