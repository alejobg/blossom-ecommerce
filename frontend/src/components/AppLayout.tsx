import { Link, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { useCart } from '../hooks/useCart'
import { authOrigin } from '../routes/authRedirect'
import { canPurchase } from '../utils/canPurchase'

export function AppLayout() {
  const { session, logout } = useAuth()
  const { itemCount, storageError } = useCart()
  const location = useLocation()

  return (
    <div className="min-h-screen bg-[#f7f8f4]">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-7xl flex-wrap items-center gap-x-6 gap-y-3 px-4 py-4 sm:px-6 lg:px-8">
          <Link className="text-2xl font-semibold tracking-tight text-blossom-700" to="/">
            blossom<span className="text-lime-600">.</span>
          </Link>
          <nav aria-label="Main navigation" className="flex items-center gap-4 text-sm font-medium">
            <Link className="text-slate-600 transition hover:text-blossom-700" to="/">
              Products
            </Link>
          </nav>
          <nav aria-label="Account" className="ml-auto flex items-center gap-4 text-sm font-medium">
            {session?.user.role === 'CUSTOMER' && (
              <Link className="text-blossom-700 hover:underline" to="/orders">
                My orders
              </Link>
            )}
            {canPurchase(session) && (
              <Link className="text-blossom-700 hover:underline" to="/cart">
                Cart <span aria-label={`${itemCount} items in cart`} className="rounded-full bg-blossom-100 px-2 py-1 text-xs">{itemCount}</span>
              </Link>
            )}
            {session ? (
              <>
                <span className="text-slate-700">{session.user.name}</span>
                <button className="text-blossom-700 hover:underline" onClick={logout} type="button">
                  Sign out
                </button>
              </>
            ) : (
              <Link
                className="text-blossom-700 hover:underline"
                state={authOrigin(location)}
                to="/login"
              >
                Sign in
              </Link>
            )}
          </nav>
        </div>
      </header>
      {storageError && <p className="mx-auto max-w-7xl px-4 py-3 text-sm text-red-700" role="alert">{storageError}</p>}
      <Outlet />
    </div>
  )
}
