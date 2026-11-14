import { useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { getOrderProblem } from '../api/orderProblem'
import { placeOrder } from '../api/orders'
import { CartItemRow } from '../components/CartItemRow'
import { useAuth } from '../hooks/useAuth'
import { useCart } from '../hooks/useCart'
import { authOrigin } from '../routes/authRedirect'
import { formatPrice } from '../utils/formatPrice'

export function CartPage() {
  const { items, stockByProductId, total, remove, updateQuantity, updateStock, clear } = useCart()
  const { session } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const inFlight = useRef(false)
  const hasInsufficientStock = items.some((item) => item.quantity > stockByProductId[item.productId])

  const handleQuantity = (productId: number, quantity: number) => {
    setError(updateQuantity(productId, quantity) ?? '')
  }

  const handleCheckout = async () => {
    if (inFlight.current || items.length === 0 || hasInsufficientStock) return
    if (!session) {
      navigate('/login', { state: authOrigin(location) })
      return
    }
    if (session.user.role === 'ADMIN') return

    inFlight.current = true
    setSubmitting(true)
    setError('')
    try {
      const order = await placeOrder({
        items: items.map(({ productId, quantity }) => ({ productId, quantity })),
      })
      clear()
      navigate(`/orders/${order.id}`, { replace: true })
    } catch (requestError) {
      const problem = getOrderProblem(requestError, items)
      if (problem.stock) updateStock(problem.stock.productId, problem.stock.available)
      setError(problem.message)
    } finally {
      inFlight.current = false
      setSubmitting(false)
    }
  }

  return (
    <main className="mx-auto max-w-4xl px-4 py-8 sm:px-6 sm:py-12">
      <h1 className="text-3xl font-semibold tracking-tight text-ink">Your cart</h1>
      {items.length === 0 ? (
        <div className="mt-7 rounded-2xl bg-white p-8 text-center">
          <p className="text-slate-600">Your cart is empty.</p>
          <Link className="mt-4 inline-block font-semibold text-blossom-700 underline" to="/">
            Browse products
          </Link>
        </div>
      ) : (
        <div className="mt-7 rounded-2xl border border-slate-200 bg-white p-6">
          <ul>
            {items.map((item) => (
              <CartItemRow
                disabled={submitting}
                item={item}
                key={item.productId}
                onChangeQuantity={handleQuantity}
                onRemove={(id) => { remove(id); setError('') }}
                stock={stockByProductId[item.productId]}
              />
            ))}
          </ul>
          <div className="mt-6 flex justify-between border-t border-slate-200 pt-5 text-lg font-semibold">
            <span>Total</span>
            <span>{formatPrice(total)}</span>
          </div>
          {error && <p className="mt-5 rounded-xl bg-red-50 p-4 text-sm text-red-700" role="alert">{error}</p>}
          {session?.user.role === 'ADMIN' && (
            <p className="mt-5 text-sm text-slate-600">Admin accounts cannot place orders.</p>
          )}
          <button
            className="mt-6 w-full rounded-xl bg-blossom-600 px-5 py-3.5 font-semibold text-white hover:bg-blossom-700 disabled:cursor-not-allowed disabled:opacity-60"
            disabled={submitting || session?.user.role === 'ADMIN' || hasInsufficientStock}
            onClick={handleCheckout}
            type="button"
          >
            {submitting ? 'Placing order…' : 'Place order'}
          </button>
        </div>
      )}
    </main>
  )
}
