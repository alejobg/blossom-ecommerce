import type { ReactNode } from 'react'

interface AuthCardProps {
  title: string
  description: string
  children: ReactNode
  footer: ReactNode
}

export function AuthCard({ title, description, children, footer }: AuthCardProps) {
  return (
    <main className="min-h-screen bg-[#f7f8f4] px-4 py-8 sm:px-8 sm:py-12">
      <div className="mx-auto grid min-h-[min(760px,calc(100vh-4rem))] max-w-6xl overflow-hidden rounded-[2rem] bg-white shadow-[0_24px_80px_-32px_rgba(42,57,38,0.28)] lg:grid-cols-[1fr_0.92fr]">
        <aside className="relative hidden overflow-hidden bg-blossom-700 p-12 text-white lg:flex lg:flex-col lg:justify-between">
          <div className="absolute -right-24 -top-20 h-80 w-80 rounded-full border border-white/10" />
          <div className="absolute -bottom-36 -left-24 h-96 w-96 rounded-full border border-white/10" />
          <a className="relative text-2xl font-semibold tracking-tight" href="/login">
            blossom<span className="text-lime-200">.</span>
          </a>
          <div className="relative max-w-md pb-8">
            <p className="mb-5 text-xs font-semibold uppercase tracking-[0.24em] text-lime-200">
              Little details, big moments
            </p>
            <h2 className="text-4xl font-medium leading-tight tracking-tight">
              Find something that makes you smile.
            </h2>
            <p className="mt-5 max-w-sm leading-7 text-white/75">
              Thoughtfully selected to bring beauty to everyday moments.
            </p>
          </div>
          <p className="relative text-sm text-white/60">Made with care, just for you.</p>
        </aside>

        <section className="flex items-center justify-center px-6 py-10 sm:px-12 lg:px-14">
          <div className="w-full max-w-md">
            <a className="mb-10 inline-block text-xl font-semibold tracking-tight text-blossom-700 lg:hidden" href="/login">
              blossom<span className="text-lime-600">.</span>
            </a>
            <p className="text-sm font-medium text-blossom-600">Welcome to Blossom</p>
            <h1 className="mt-2 text-3xl font-semibold tracking-tight text-ink sm:text-4xl">{title}</h1>
            <p className="mt-3 text-sm leading-6 text-slate-500">{description}</p>
            <div className="mt-8">{children}</div>
            <div className="mt-7 border-t border-slate-100 pt-6 text-center text-sm text-slate-500">
              {footer}
            </div>
          </div>
        </section>
      </div>
    </main>
  )
}
