import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useProductCart } from '../hooks/useProductCart'
import type { Product } from '../types/product'
import { PRODUCT_CATEGORY_LABELS } from '../types/product'
import { formatPrice } from '../utils/formatPrice'

interface ProductCardProps {
  product: Product
}

export function ProductCard({ product }: ProductCardProps) {
  const { addProduct, limitReached, canPurchase } = useProductCart(product)
  const [added, setAdded] = useState(false)
  const [error, setError] = useState('')
  const feedbackTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => () => {
    if (feedbackTimer.current !== null) clearTimeout(feedbackTimer.current)
  }, [])

  const handleAdd = () => {
    const addError = addProduct()
    setError(addError ?? '')
    if (feedbackTimer.current !== null) clearTimeout(feedbackTimer.current)
    setAdded(addError === null)
    if (addError === null) {
      feedbackTimer.current = setTimeout(() => setAdded(false), 1500)
    }
  }

  return (
    <article className="group relative flex h-full flex-col rounded-2xl border border-slate-200 bg-white p-5 transition hover:-translate-y-0.5 hover:border-blossom-200 hover:shadow-lg">
      <div className="mb-5 flex min-h-32 items-end rounded-xl bg-gradient-to-br from-blossom-50 via-[#f2f4ec] to-[#e9eee4] p-4">
        <span className="rounded-full bg-white/80 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-blossom-700">
          {PRODUCT_CATEGORY_LABELS[product.category]}
        </span>
      </div>
      <h2 className="text-lg font-semibold text-ink">
        <Link
          className="focus-visible:outline-none after:absolute after:inset-0 after:rounded-2xl after:content-[''] focus-visible:after:ring-2 focus-visible:after:ring-blossom-500 focus-visible:after:ring-offset-2"
          to={`/products/${product.id}`}
        >
          {product.name}
        </Link>
      </h2>
      <p className="mt-2 flex-1 text-sm leading-6 text-slate-500">{product.description}</p>
      <div className="mt-5 flex items-center justify-between gap-3">
        <p className="font-semibold text-blossom-700" aria-label={`Price ${formatPrice(product.price)}`}>
          {formatPrice(product.price)}
        </p>
        {canPurchase && (
          <button
            className="relative z-10 rounded-xl bg-blossom-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blossom-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blossom-600 disabled:cursor-not-allowed disabled:opacity-60"
            disabled={limitReached}
            onClick={handleAdd}
            type="button"
          >
            {product.stock === 0 ? 'Out of stock' : added ? 'Added ✓' : 'Add to cart'}
          </button>
        )}
      </div>
      {canPurchase && (
        <>
          <span className="sr-only" role="status">{added ? 'Added to your cart.' : ''}</span>
          {error && <p className="mt-3 text-sm text-red-700" role="alert">{error}</p>}
        </>
      )}
    </article>
  )
}
