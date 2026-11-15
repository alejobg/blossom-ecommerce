import { ProductCard } from './ProductCard'
import type { PagedResponse } from '../types/pagination'
import type { Product } from '../types/product'

interface ProductResultsProps {
  result: PagedResponse<Product> | null
  loading: boolean
  error: string
  page: number
  onRetry: () => void
  onClearFilters: () => void
  onPageChange: (page: number) => void
}

export function ProductResults({
  result,
  loading,
  error,
  page,
  onRetry,
  onClearFilters,
  onPageChange,
}: ProductResultsProps) {
  if (loading) {
    return (
      <div className="rounded-2xl bg-white px-6 py-16 text-center text-sm text-slate-500" role="status">
        Loading products…
      </div>
    )
  }

  if (error) {
    return (
      <div className="rounded-2xl border border-red-100 bg-white px-6 py-12 text-center" role="alert">
        <h2 className="text-lg font-semibold text-ink">We couldn&apos;t load the products</h2>
        <p className="mt-2 text-sm text-slate-600">{error}</p>
        <button
          className="mt-5 rounded-full bg-blossom-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-blossom-700"
          onClick={onRetry}
          type="button"
        >
          Try again
        </button>
      </div>
    )
  }

  if (!result || result.content.length === 0) {
    return (
      <div className="rounded-2xl bg-white px-6 py-14 text-center">
        <h2 className="text-lg font-semibold text-ink">No products found</h2>
        <p className="mt-2 text-sm text-slate-500">Try changing or clearing your filters.</p>
        <button
          className="mt-5 text-sm font-semibold text-blossom-700 underline underline-offset-4"
          onClick={onClearFilters}
          type="button"
        >
          Clear filters
        </button>
      </div>
    )
  }

  return (
    <>
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3 2xl:grid-cols-4">
        {result.content.map((product) => (
          <ProductCard key={product.id} product={product} />
        ))}
      </div>
      {result.totalPages > 1 && (
        <nav aria-label="Product pages" className="mt-8 flex items-center justify-center gap-4">
          <button
            className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 disabled:cursor-not-allowed disabled:opacity-40"
            disabled={page === 0}
            onClick={() => onPageChange(page - 1)}
            type="button"
          >
            Previous
          </button>
          <span className="text-sm text-slate-600">
            Page {page + 1} of {result.totalPages}
          </span>
          <button
            className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 disabled:cursor-not-allowed disabled:opacity-40"
            disabled={page + 1 >= result.totalPages}
            onClick={() => onPageChange(page + 1)}
            type="button"
          >
            Next
          </button>
        </nav>
      )}
    </>
  )
}
