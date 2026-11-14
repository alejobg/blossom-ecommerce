import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { AuthCard } from '../components/AuthCard'
import { getApiProblem } from '../api/problem'
import { useAuth } from '../hooks/useAuth'
import { getAuthRedirect } from '../routes/authRedirect'

const loginSchema = z.object({
  email: z.string().trim().email('Please enter a valid email address.'),
  password: z.string().min(1, 'Please enter your password.'),
})

type LoginValues = z.infer<typeof loginSchema>

const inputClassName =
  'mt-2 block w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-ink outline-none transition placeholder:text-slate-400 focus:border-blossom-500 focus:ring-4 focus:ring-blossom-100'

export function LoginPage() {
  const { login } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const destination = getAuthRedirect(location.state, location.search)
  const [formError, setFormError] = useState('')
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginValues>({ resolver: zodResolver(loginSchema) })

  const onSubmit = async (values: LoginValues) => {
    setFormError('')
    try {
      await login(values)
      navigate(destination, { replace: true })
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
          <Link className="font-semibold text-blossom-600 hover:text-blossom-700" state={location.state} to={{ pathname: '/register', search: location.search }}>
            Create an account
          </Link>
        </>
      }
    >
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
          <button
            className="flex w-full items-center justify-center rounded-xl bg-blossom-600 px-5 py-3.5 text-sm font-semibold text-white transition hover:bg-blossom-700 focus:outline-none focus:ring-4 focus:ring-blossom-100 disabled:cursor-not-allowed disabled:opacity-60"
            disabled={isSubmitting}
            type="submit"
          >
            {isSubmitting ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
    </AuthCard>
  )
}
