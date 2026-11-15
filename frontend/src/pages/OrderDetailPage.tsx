import { useEffect, useState } from 'react'
import axios from 'axios'
import { Link, useParams } from 'react-router-dom'
import { getOrderById } from '../api/orders'
import { getApiProblem } from '../api/problem'
import type { OrderResponse } from '../types/order'
import { formatOrderDate } from '../utils/formatOrderDate'
import { formatPrice } from '../utils/formatPrice'

type DetailRequest =
  | { key: string; status: 'loading' }
  | { key: string; status: 'success'; order: OrderResponse }
  | { key: string; status: 'notFound' }
  | { key: string; status: 'error'; message: string }

export function OrderDetailPage() {
  const { id = '' } = useParams()
  const [retryKey, setRetryKey] = useState(0)
  const requestKey = `${id}:${retryKey}`
  const [request, setRequest] = useState<DetailRequest>({ key: '', status: 'loading' })
  const current = request.key === requestKey
  const order = current && request.status === 'success' ? request.order : null
  const notFound = current && request.status === 'notFound'
  const error = current && request.status === 'error' ? request.message : ''
  const loading = !current || request.status === 'loading'

  useEffect(() => {
    let active = true
    getOrderById(id)
      .then((response) => {
        if (active) setRequest({ key: requestKey, status: 'success', order: response })
      })
      .catch((requestError: unknown) => {
        if (!active) return
        if (axios.isAxiosError(requestError) && requestError.response?.status === 404) {
          setRequest({ key: requestKey, status: 'notFound' })
        } else {
          setRequest({ key: requestKey, status: 'error', message: getApiProblem(requestError).detail })
        }
      })
    return () => { active = false }
  }, [id, requestKey])

  return (
    <main className="mx-auto max-w-4xl px-4 py-8 sm:px-6 sm:py-12">
      <Link className="text-sm font-semibold text-blossom-700 underline underline-offset-4" to="/orders">
        Back to my orders
      </Link>
      {loading && (
        <div className="mt-8 rounded-2xl bg-white px-6 py-16 text-center text-sm text-slate-500" role="status">
          Loading order…
        </div>
      )}
      {!loading && (notFound || error) && (
        <div className="mt-8 rounded-2xl border border-red-100 bg-white px-6 py-12 text-center" role="alert">
          <h1 className="text-2xl font-semibold text-ink">{notFound ? 'Order not found' : 'We couldn’t load this order'}</h1>
          <p className="mt-3 text-sm text-slate-600">{notFound ? 'This order could not be found.' : error}</p>
          {!notFound && (
            <button className="mt-5 rounded-full bg-blossom-600 px-5 py-2.5 text-sm font-semibold text-white"
              onClick={() => setRetryKey((currentKey) => currentKey + 1)} type="button">
              Try again
            </button>
          )}
        </div>
      )}
      {order && (
        <article className="mt-8 rounded-2xl bg-white p-6 shadow-sm sm:p-8">
          <h1 className="text-3xl font-semibold text-ink">Order #{order.id}</h1>
          <p className="mt-2 text-sm text-slate-600">
            Placed on <time dateTime={order.createdAt}>{formatOrderDate(order.createdAt)}</time>
          </p>
          {order.items.length === 0 ? (
            <p className="mt-8 text-slate-600">This order has no items.</p>
          ) : (
            <ul className="mt-8 divide-y divide-slate-200">
              {order.items.map((item) => (
                <li className="flex flex-wrap justify-between gap-4 py-4" key={item.productId}>
                  <div>
                    <h2 className="font-semibold text-ink">{item.productName}</h2>
                    <p className="mt-1 text-sm text-slate-600">
                      {formatPrice(item.unitPrice)} each · Quantity: {item.quantity}
                    </p>
                  </div>
                  <p className="font-semibold text-ink">Subtotal: {formatPrice(item.subtotal)}</p>
                </li>
              ))}
            </ul>
          )}
          <p className="mt-6 border-t border-slate-200 pt-5 text-right text-lg font-semibold text-ink">
            Total: {formatPrice(order.total)}
          </p>
        </article>
      )}
    </main>
  )
}
