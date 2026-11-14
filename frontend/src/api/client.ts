import axios from 'axios'
import { clearAuthSession, getStoredAuthSession } from './sessionStorage'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
})

client.interceptors.request.use((config) => {
  const session = getStoredAuthSession()
  if (session) {
    config.headers.Authorization = `${session.tokenType} ${session.accessToken}`
  }
  return config
})

client.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      clearAuthSession()
      if (window.location.pathname !== '/login') {
        const returnTo = `${window.location.pathname}${window.location.search}${window.location.hash}`
        window.location.assign(`/login?${new URLSearchParams({ returnTo })}`)
      }
    }
    return Promise.reject(error)
  },
)

export default client
