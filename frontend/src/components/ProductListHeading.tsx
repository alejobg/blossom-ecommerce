import type { PagedResponse } from '../types/pagination'
import type { Product } from '../types/product'

interface ProductListHeadingProps {
  result: PagedResponse<Product> | null
  loading: boolean
}

export function ProductListHeading({ result, loading }: ProductListHeadingProps) {
  return (
    <div className="mb-7 flex flex-wrap items-end justify-between gap-4">
      <div>
        <p className="text-sm font-medium text-blossom-600">Thoughtfully chosen for you</p>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight text-ink sm:text-4xl">
          The collection
        </h1>
      </div>
      {result && !loading && (
        <p className="text-sm text-slate-500">
          {result.totalElements} {result.totalElements === 1 ? 'product' : 'products'}
        </p>
      )}
    </div>
  )
}
