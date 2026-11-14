import {
  PRODUCT_CATEGORIES,
  PRODUCT_CATEGORY_LABELS,
  type ProductCategory,
} from '../types/product'

interface ProductFilterBarProps {
  name: string
  category: ProductCategory | ''
  minPrice: string
  maxPrice: string
  onNameChange: (value: string) => void
  onCategoryChange: (value: string) => void
  onMinPriceChange: (value: string) => void
  onMaxPriceChange: (value: string) => void
  onReset: () => void
}

const inputClassName =
  'mt-2 block w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-ink outline-none transition focus:border-blossom-500 focus:ring-4 focus:ring-blossom-100'

export function ProductFilterBar({
  name,
  category,
  minPrice,
  maxPrice,
  onNameChange,
  onCategoryChange,
  onMinPriceChange,
  onMaxPriceChange,
  onReset,
}: ProductFilterBarProps) {
  return (
    <section aria-label="Product filters" className="rounded-2xl border border-slate-200 bg-white p-5">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div>
          <label className="text-sm font-medium text-slate-700" htmlFor="product-name">
            Name
          </label>
          <input
            className={inputClassName}
            id="product-name"
            onChange={(event) => onNameChange(event.target.value)}
            placeholder="Search products"
            type="search"
            value={name}
          />
        </div>
        <div>
          <label className="text-sm font-medium text-slate-700" htmlFor="product-category">
            Category
          </label>
          <select
            className={inputClassName}
            id="product-category"
            onChange={(event) => onCategoryChange(event.target.value)}
            value={category}
          >
            <option value="">All categories</option>
            {PRODUCT_CATEGORIES.map((value) => (
              <option key={value} value={value}>
                {PRODUCT_CATEGORY_LABELS[value]}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="text-sm font-medium text-slate-700" htmlFor="min-price">
            Minimum price
          </label>
          <input
            className={inputClassName}
            id="min-price"
            min="0"
            onChange={(event) => onMinPriceChange(event.target.value)}
            placeholder="0.00"
            step="0.01"
            type="number"
            value={minPrice}
          />
        </div>
        <div>
          <label className="text-sm font-medium text-slate-700" htmlFor="max-price">
            Maximum price
          </label>
          <input
            className={inputClassName}
            id="max-price"
            min="0"
            onChange={(event) => onMaxPriceChange(event.target.value)}
            placeholder="Any"
            step="0.01"
            type="number"
            value={maxPrice}
          />
        </div>
      </div>
      <button
        className="mt-4 text-sm font-semibold text-blossom-700 underline decoration-blossom-200 underline-offset-4 hover:decoration-blossom-700"
        onClick={onReset}
        type="button"
      >
        Clear filters
      </button>
    </section>
  )
}
