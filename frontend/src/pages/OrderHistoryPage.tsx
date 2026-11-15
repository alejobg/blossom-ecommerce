import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getApiProblem } from '../api/problem'
import { listOrders } from '../api/orders'
import type { OrderResponse } from '../types/order'
import type { PagedResponse } from '../types/pagination'
import { formatOrderDate } from '../utils/formatOrderDate'
import { formatPrice } from '../utils/formatPrice'

const PAGE_SIZE = 20

type HistoryRequest =
  | { key: string; status: 'loading' }
  | { key: string; status: 'success'; result: PagedResponse<OrderResponse> }
  | { key: string; status: 'error'; message: string }

export function OrderHistoryPage() {
  const [page, setPage] = useState(0)
  const [retryKey, setRetryKey] = useState(0)
  const requestKey = `${page}:${retryKey}`
  const [request, setRequest] = useState<HistoryRequest>({ key: '', status: 'loading' })
  const current = request.key === requestKey
  const result = current && request.status === 'success' ? request.result : null
  const error = current && request.status === 'error' ? request.message : ''
  const loading = !current || request.status === 'loading'

  useEffect(() => {
    let active = true
    listOrders(page, PAGE_SIZE)
      .then((response) => {
        if (active) setRequest({ key: requestKey, status: 'success', result: response })
      })
      .catch((requestError: unknown) => {
        if (active) setRequest({ key: requestKey, status: 'error', message: getApiProblem(requestError).detail })
      })
    return () => { active = false }
  }, [page, requestKey])

  return (
    <main className="mx-auto max-w-4xl px-4 py-8 sm:px-6 sm:py-12">
      <h1 className="text-3xl font-semibold tracking-tight text-ink">My orders</h1>
      {loading && (
        <div className="mt-8 rounded-2xl bg-white px-6 py-16 text-center text-sm text-slate-500" role="status">
          Loading orders…
        </div>
      )}
      {!loading && error && (
        <div className="mt-8 rounded-2xl border border-red-100 bg-white px-6 py-12 text-center" role="alert">
          <h2 className="text-lg font-semibold text-ink">We couldn&apos;t load your orders</h2>
          <p className="mt-2 text-sm text-slate-600">{error}</p>
          <button className="mt-5 rounded-full bg-blossom-600 px-5 py-2.5 text-sm font-semibold text-white"
            onClick={() => setRetryKey((currentKey) => currentKey + 1)} type="button">
            Try again
          </button>
        </div>
      )}
      {result && (
        <>
          {result.content.length === 0 ? (
            <div className="mt-8 rounded-2xl bg-white px-6 py-14 text-center">
              <p className="text-slate-600">{page === 0 ? 'You have no orders yet.' : 'No orders on this page.'}</p>
              <Link className="mt-4 inline-block font-semibold text-blossom-700 underline" to="/">
                Browse products
              </Link>
            </div>
          ) : (
            <ul className="mt-8 space-y-4">
              {result.content.map((order) => {
                const count = order.items.reduce((sum, item) => sum + item.quantity, 0)
                return (
                  <li className="rounded-2xl border border-slate-200 bg-white p-5 sm:p-6" key={order.id}>
                    <div className="flex flex-wrap items-center justify-between gap-4">
                      <div>
                        <Link className="font-semibold text-blossom-700 underline underline-offset-4" to={`/orders/${order.id}`}>
                          Order #{order.id}
                        </Link>
                        <p className="mt-2 text-sm text-slate-600">
                          <time dateTime={order.createdAt}>{formatOrderDate(order.createdAt)}</time>
                          {' · '}{count} {count === 1 ? 'item' : 'items'}
                        </p>
                      </div>
                      <p className="font-semibold text-ink">{formatPrice(order.total)}</p>
                    </div>
                  </li>
                )
              })}
            </ul>
          )}
          {(page > 0 || result.totalPages > 1) && (
            <nav aria-label="Order pages" className="mt-8 flex items-center justify-center gap-4">
              <button className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm disabled:opacity-40"
                disabled={page === 0} onClick={() => setPage((currentPage) => currentPage - 1)} type="button">
                Previous
              </button>
              <span className="text-sm text-slate-600">Page {page + 1} of {result.totalPages}</span>
              <button className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm disabled:opacity-40"
                disabled={page + 1 >= result.totalPages} onClick={() => setPage((currentPage) => currentPage + 1)} type="button">
                Next
              </button>
            </nav>
          )}
        </>
      )}
    </main>
  )
}
