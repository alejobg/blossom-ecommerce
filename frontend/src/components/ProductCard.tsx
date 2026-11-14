import { Link } from 'react-router-dom'
import type { Product } from '../types/product'
import { PRODUCT_CATEGORY_LABELS } from '../types/product'
import { formatPrice } from '../utils/formatPrice'

interface ProductCardProps {
  product: Product
}

export function ProductCard({ product }: ProductCardProps) {
  return (
    <article className="group flex h-full flex-col rounded-2xl border border-slate-200 bg-white p-5 transition hover:-translate-y-0.5 hover:border-blossom-200 hover:shadow-lg">
      <div className="mb-5 flex min-h-32 items-end rounded-xl bg-gradient-to-br from-blossom-50 via-[#f2f4ec] to-[#e9eee4] p-4">
        <span className="rounded-full bg-white/80 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-blossom-700">
          {PRODUCT_CATEGORY_LABELS[product.category]}
        </span>
      </div>
      <h2 className="text-lg font-semibold text-ink">{product.name}</h2>
      <p className="mt-2 flex-1 text-sm leading-6 text-slate-500">{product.description}</p>
      <div className="mt-5 flex items-center justify-between gap-3">
        <p className="font-semibold text-blossom-700" aria-label={`Price ${formatPrice(product.price)}`}>
          {formatPrice(product.price)}
        </p>
        <Link
          className="text-sm font-semibold text-blossom-700 underline decoration-blossom-200 underline-offset-4 group-hover:decoration-blossom-600"
          to={`/products/${product.id}`}
        >
          View details
        </Link>
      </div>
    </article>
  )
}
