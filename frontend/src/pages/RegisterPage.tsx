import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { AuthCard } from '../components/AuthCard'
import { getApiProblem } from '../api/problem'
import { registerUser } from '../api/auth'
import { getAuthRedirect } from '../routes/authRedirect'

const registerSchema = z.object({
  name: z.string().trim().min(1, 'Please enter your name.').max(100, 'Name cannot exceed 100 characters.'),
  email: z
    .string()
    .trim()
    .min(1, 'Please enter your email address.')
    .email('Please enter a valid email address.')
    .max(254, 'Email cannot exceed 254 characters.'),
  password: z
    .string()
    .min(8, 'Password must be at least 8 characters.')
    .refine((value) => /\p{L}/u.test(value), 'Password must contain at least one letter.')
    .refine((value) => /\p{Nd}/u.test(value), 'Password must contain at least one number.')
    .refine(
      (value) => new TextEncoder().encode(value).length <= 72,
      'Password cannot exceed 72 bytes.',
    ),
})

type RegisterValues = z.infer<typeof registerSchema>

const inputClassName =
  'mt-2 block w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-ink outline-none transition placeholder:text-slate-400 focus:border-blossom-500 focus:ring-4 focus:ring-blossom-100'

export function RegisterPage() {
  const [formError, setFormError] = useState('')
  const location = useLocation()
  const navigate = useNavigate()
  const destination = getAuthRedirect(location.state, location.search)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterValues>({ resolver: zodResolver(registerSchema) })

  const onSubmit = async (values: RegisterValues) => {
    setFormError('')
    try {
      await registerUser(values)
      navigate(destination, { replace: true })
    } catch (error) {
      const problem = getApiProblem(error)
      if (problem.errors.name) setError('name', { message: problem.errors.name })
      if (problem.errors.email) setError('email', { message: problem.errors.email })
      if (problem.errors.password) setError('password', { message: problem.errors.password })
      setFormError(problem.detail)
    }
  }

  return (
    <AuthCard
      title="Create your account"
      description="Join Blossom and find your new favorites."
      footer={
        <>
          Already have an account?{' '}
          <Link className="font-semibold text-blossom-600 hover:text-blossom-700" state={location.state} to={{ pathname: '/login', search: location.search }}>
            Sign in
          </Link>
        </>
      }
    >
        <form className="space-y-4" noValidate onSubmit={handleSubmit(onSubmit)}>
          <div>
            <label className="text-sm font-medium text-slate-700" htmlFor="name">
              Name
            </label>
            <input
              autoComplete="name"
              className={inputClassName}
              id="name"
              placeholder="Your name"
              type="text"
              {...register('name')}
            />
            {errors.name && <p className="mt-2 text-sm text-red-600">{errors.name.message}</p>}
          </div>
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
              autoComplete="new-password"
              className={inputClassName}
              id="password"
              placeholder="At least 8 characters"
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
            {isSubmitting ? 'Creating account…' : 'Create account'}
          </button>
        </form>
    </AuthCard>
  )
}
