import axios from 'axios'
import type { ApiProblem } from '../types/auth'

export function getApiProblem(error: unknown): Required<Pick<ApiProblem, 'detail'>> & {
  errors: Record<string, string>
} {
  if (axios.isAxiosError<ApiProblem>(error)) {
    const problem = error.response?.data
    const errors = problem?.errors ?? {}

    return {
      detail:
        problem?.detail ??
        (error.response
          ? "We couldn't complete your request."
          : 'Unable to connect to the server. Please try again.'),
      errors,
    }
  }

  return {
    detail: 'An unexpected error occurred. Please try again.',
    errors: {},
  }
}
