import { Link, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { authOrigin } from '../routes/authRedirect'

export function AppLayout() {
  const { session, logout } = useAuth()
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
      <Outlet />
    </div>
  )
}
