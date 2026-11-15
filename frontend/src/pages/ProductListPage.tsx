import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { getApiProblem } from '../api/problem'
import { searchProducts } from '../api/products'
import { ProductFilterBar } from '../components/ProductFilterBar'
import { ProductListHeading } from '../components/ProductListHeading'
import { ProductResults } from '../components/ProductResults'
import type { PagedResponse } from '../types/pagination'
import {
  PRODUCT_CATEGORIES,
  type Product,
  type ProductSearchParams,
} from '../types/product'

const PAGE_SIZE = 20
const NAME_DEBOUNCE_MS = 350

type ProductListRequest =
  | { key: string; status: 'loading' }
  | { key: string; status: 'success'; result: PagedResponse<Product> }
  | { key: string; status: 'error'; message: string }

function parsePage(value: string | null): number {
  const page = Number(value)
  return Number.isInteger(page) && page >= 0 ? page : 0
}

function parsePrice(value: string | null): number | undefined {
  if (value === null || value.trim() === '') return undefined
  const price = Number(value)
  return Number.isFinite(price) ? price : undefined
}

export function ProductListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const nameParam = searchParams.get('name') ?? ''
  const categoryParam = searchParams.get('category') ?? ''
  const minPriceParam = searchParams.get('minPrice') ?? ''
  const maxPriceParam = searchParams.get('maxPrice') ?? ''
  const page = parsePage(searchParams.get('page'))
  const category = PRODUCT_CATEGORIES.find((value) => value === categoryParam) ?? ''
  const [nameDraft, setNameDraft] = useState({ urlValue: nameParam, value: nameParam })
  const [retryKey, setRetryKey] = useState(0)
  const nameInput = nameDraft.urlValue === nameParam ? nameDraft.value : nameParam

  const query = useMemo<ProductSearchParams>(
    () => ({
      ...(nameParam.trim() ? { name: nameParam.trim() } : {}),
      ...(category ? { category } : {}),
      ...(parsePrice(minPriceParam) !== undefined
        ? { minPrice: parsePrice(minPriceParam) }
        : {}),
      ...(parsePrice(maxPriceParam) !== undefined
        ? { maxPrice: parsePrice(maxPriceParam) }
        : {}),
      page,
      size: PAGE_SIZE,
    }),
    [nameParam, category, minPriceParam, maxPriceParam, page],
  )
  const requestKey = `${JSON.stringify(query)}:${retryKey}`
  const [request, setRequest] = useState<ProductListRequest>({ key: '', status: 'loading' })
  const isCurrentRequest = request.key === requestKey
  const loading = !isCurrentRequest || request.status === 'loading'
  const result =
    isCurrentRequest && request.status === 'success' ? request.result : null
  const error = isCurrentRequest && request.status === 'error' ? request.message : ''

  useEffect(() => {
    const name = nameInput.trim()
    if (name === nameParam.trim()) return

    const timeout = window.setTimeout(() => {
      const next = new URLSearchParams(searchParams)
      if (name) next.set('name', name)
      else next.delete('name')
      next.delete('page')
      setSearchParams(next)
    }, NAME_DEBOUNCE_MS)

    return () => window.clearTimeout(timeout)
  }, [nameInput, nameParam, searchParams, setSearchParams])

  useEffect(() => {
    let active = true
    searchProducts(query)
      .then((response) => {
        if (active) setRequest({ key: requestKey, status: 'success', result: response })
      })
      .catch((requestError: unknown) => {
        if (active) {
          setRequest({
            key: requestKey,
            status: 'error',
            message: getApiProblem(requestError).detail,
          })
        }
      })

    return () => {
      active = false
    }
  }, [query, requestKey])

  const updateFilter = (key: 'category' | 'minPrice' | 'maxPrice', value: string) => {
    const next = new URLSearchParams(searchParams)
    if (value) next.set(key, value)
    else next.delete(key)
    next.delete('page')
    setSearchParams(next)
  }

  const updatePage = (nextPage: number) => {
    const next = new URLSearchParams(searchParams)
    if (nextPage === 0) next.delete('page')
    else next.set('page', String(nextPage))
    setSearchParams(next)
  }

  const resetFilters = () => {
    setSearchParams(new URLSearchParams())
  }

  return (
      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 sm:py-12 lg:px-8">
        <ProductListHeading loading={loading} result={result} />

        <ProductFilterBar
          category={category}
          maxPrice={maxPriceParam}
          minPrice={minPriceParam}
          name={nameInput}
          onCategoryChange={(value) => updateFilter('category', value)}
          onMaxPriceChange={(value) => updateFilter('maxPrice', value)}
          onMinPriceChange={(value) => updateFilter('minPrice', value)}
          onNameChange={(value) => setNameDraft({ urlValue: nameParam, value })}
          onReset={resetFilters}
        />

        <section aria-label="Product results" className="mt-7">
          <ProductResults
            error={error}
            loading={loading}
            onClearFilters={resetFilters}
            onPageChange={updatePage}
            onRetry={() => setRetryKey((current) => current + 1)}
            page={page}
            result={result}
          />
        </section>
      </main>
  )
}
