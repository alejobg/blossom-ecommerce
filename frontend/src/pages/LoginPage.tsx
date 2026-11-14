import { useState } from 'react'
import { Link } from 'react-router-dom'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { AuthCard } from '../components/AuthCard'
import { getApiProblem } from '../api/problem'
import { useAuth } from '../hooks/useAuth'

const loginSchema = z.object({
  email: z.string().trim().email('Please enter a valid email address.'),
  password: z.string().min(1, 'Please enter your password.'),
})

type LoginValues = z.infer<typeof loginSchema>

const inputClassName =
  'mt-2 block w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-ink outline-none transition placeholder:text-slate-400 focus:border-blossom-500 focus:ring-4 focus:ring-blossom-100'

export function LoginPage() {
  const { login, logout, session } = useAuth()
  const [formError, setFormError] = useState('')
  const [success, setSuccess] = useState('')
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginValues>({ resolver: zodResolver(loginSchema) })

  const onSubmit = async (values: LoginValues) => {
    setFormError('')
    setSuccess('')
    try {
      await login(values)
      setSuccess('You have signed in successfully.')
    } catch (error) {
      const problem = getApiProblem(error)
      if (problem.errors.email) setError('email', { message: problem.errors.email })
      if (problem.errors.password) setError('password', { message: problem.errors.password })
      setFormError(problem.detail)
    }
  }

  return (
    <AuthCard
      title="Sign in"
      description="Sign in to your account to continue."
      footer={
        <>
          Don&apos;t have an account?{' '}
          <Link className="font-semibold text-blossom-600 hover:text-blossom-700" to="/register">
            Create an account
          </Link>
        </>
      }
    >
      {session ? (
        <div className="rounded-2xl border border-blossom-100 bg-blossom-50 p-5">
          <p className="font-medium text-blossom-700">Hello, {session.user.name}</p>
          <p className="mt-1 text-sm text-slate-600" role="status">
            {success || 'You are already signed in.'}
          </p>
          <button
            className="mt-4 text-sm font-semibold text-blossom-700 underline decoration-blossom-300 underline-offset-4"
            onClick={logout}
            type="button"
          >
            Sign out
          </button>
        </div>
      ) : (
        <form className="space-y-5" noValidate onSubmit={handleSubmit(onSubmit)}>
          <div>
            <label className="text-sm font-medium text-slate-700" htmlFor="email">
              Email address
            </label>
            <input
              autoComplete="email"
              className={inputClassName}
              id="email"
              placeholder="you@example.com"
              type="email"
              {...register('email')}
            />
            {errors.email && <p className="mt-2 text-sm text-red-600">{errors.email.message}</p>}
          </div>
          <div>
            <label className="text-sm font-medium text-slate-700" htmlFor="password">
              Password
            </label>
            <input
              autoComplete="current-password"
              className={inputClassName}
              id="password"
              placeholder="Your password"
              type="password"
              {...register('password')}
            />
            {errors.password && <p className="mt-2 text-sm text-red-600">{errors.password.message}</p>}
          </div>
          {formError && (
            <p className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">
              {formError}
            </p>
          )}
          {success && (
            <p className="rounded-xl bg-blossom-50 px-4 py-3 text-sm text-blossom-700" role="status">
              {success}
            </p>
          )}
          <button
            className="flex w-full items-center justify-center rounded-xl bg-blossom-600 px-5 py-3.5 text-sm font-semibold text-white transition hover:bg-blossom-700 focus:outline-none focus:ring-4 focus:ring-blossom-100 disabled:cursor-not-allowed disabled:opacity-60"
            disabled={isSubmitting}
            type="submit"
          >
            {isSubmitting ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
      )}
    </AuthCard>
  )
}
