import { Link, useParams } from 'react-router-dom'

export function OrderPlaceholderPage() {
  const { id } = useParams()
  return (
    <main className="mx-auto max-w-4xl px-4 py-12">
      <h1 className="text-3xl font-semibold text-ink">Order #{id}</h1>
      <p className="mt-4 text-slate-600">Order details are coming soon.</p>
      <Link className="mt-5 inline-block font-semibold text-blossom-700 underline" to="/">
        Browse products
      </Link>
    </main>
  )
}
