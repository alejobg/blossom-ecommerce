import { useEffect, useState } from 'react'
import axios from 'axios'
import { Link, useParams } from 'react-router-dom'
import { getApiProblem } from '../api/problem'
import { getProductById } from '../api/products'
import { AddToCart } from '../components/AddToCart'
import { useCart } from '../hooks/useCart'
import { PRODUCT_CATEGORY_LABELS, type Product } from '../types/product'
import { formatPrice } from '../utils/formatPrice'

const NOT_FOUND_MESSAGE = 'This product could not be found. It may have been removed or the link is incorrect.'

type ProductDetailRequest =
  | { key: string; status: 'loading' }
  | { key: string; status: 'success'; product: Product }
  | { key: string; status: 'notFound' }
  | { key: string; status: 'error'; message: string }

export function ProductDetailPage() {
  const { id = '' } = useParams()
  const { updateStock } = useCart()
  const [retryKey, setRetryKey] = useState(0)
  const requestKey = `${id}:${retryKey}`
  const [request, setRequest] = useState<ProductDetailRequest>({
    key: '',
    status: 'loading',
  })
  const isCurrentRequest = request.key === requestKey
  const loading = !isCurrentRequest || request.status === 'loading'
  const product =
    isCurrentRequest && request.status === 'success' ? request.product : null
  const error =
    !isCurrentRequest || request.status === 'loading'
      ? ''
      : request.status === 'notFound'
        ? NOT_FOUND_MESSAGE
        : request.status === 'error'
          ? request.message
          : ''
  const notFound = isCurrentRequest && request.status === 'notFound'

  useEffect(() => {
    let active = true
    getProductById(id)
      .then((response) => {
        if (active) {
          updateStock(response.id, response.stock)
          setRequest({ key: requestKey, status: 'success', product: response })
        }
      })
      .catch((requestError: unknown) => {
        if (!active) return
        if (axios.isAxiosError(requestError) && requestError.response?.status === 404) {
          setRequest({ key: requestKey, status: 'notFound' })
        } else {
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
  }, [id, requestKey, updateStock])

  return (
      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 sm:py-12 lg:px-8">
        <Link
          className="text-sm font-semibold text-blossom-700 underline decoration-blossom-200 underline-offset-4"
          to="/"
        >
          Back to products
        </Link>
        {loading && (
          <div className="mt-8 rounded-2xl bg-white px-6 py-16 text-center text-sm text-slate-500" role="status">
            Loading product…
          </div>
        )}
        {!loading && error && (
          <div className="mt-8 rounded-2xl border border-red-100 bg-white px-6 py-14 text-center" role="alert">
            <h1 className="text-2xl font-semibold text-ink">
              {notFound ? 'Product not found' : 'We couldn’t load this product'}
            </h1>
            <p className="mx-auto mt-3 max-w-lg text-sm leading-6 text-slate-600">{error}</p>
            {!notFound && (
              <button
                className="mt-5 rounded-full bg-blossom-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-blossom-700"
                onClick={() => setRetryKey((current) => current + 1)}
                type="button"
              >
                Try again
              </button>
            )}
          </div>
        )}
        {!loading && !error && product && (
          <article className="mt-8 grid gap-8 rounded-3xl bg-white p-6 shadow-sm sm:p-10 lg:grid-cols-2 lg:gap-14">
            <div className="flex min-h-64 items-end rounded-2xl bg-gradient-to-br from-blossom-50 via-[#f2f4ec] to-[#e9eee4] p-6 lg:min-h-[28rem]">
              <span className="rounded-full bg-white/85 px-4 py-2 text-sm font-semibold text-blossom-700">
                {PRODUCT_CATEGORY_LABELS[product.category]}
              </span>
            </div>
            <div className="flex flex-col justify-center">
              <p className="text-sm font-semibold uppercase tracking-[0.16em] text-blossom-600">
                {PRODUCT_CATEGORY_LABELS[product.category]}
              </p>
              <h1 className="mt-3 text-3xl font-semibold tracking-tight text-ink sm:text-4xl">
                {product.name}
              </h1>
              <p className="mt-5 text-2xl font-semibold text-blossom-700">{formatPrice(product.price)}</p>
              <p className="mt-6 leading-7 text-slate-600">{product.description}</p>
              <p className="mt-5 text-sm text-slate-500">In stock: {product.stock}</p>
              <AddToCart key={product.id} product={product} />
            </div>
          </article>
        )}
      </main>
  )
}
