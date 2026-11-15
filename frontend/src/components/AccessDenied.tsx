import { Link } from 'react-router-dom'

export function AccessDenied({ message }: { message: string }) {
  return (
    <main className="mx-auto max-w-4xl px-4 py-12">
      <h1 className="text-2xl font-semibold text-ink">Access denied</h1>
      <p className="mt-4 text-slate-600">{message}</p>
      <Link className="mt-5 inline-block font-semibold text-blossom-700 underline" to="/">
        Browse products
      </Link>
    </main>
  )
}
