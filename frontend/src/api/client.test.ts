import { AxiosError, type AxiosAdapter } from 'axios'
import { afterEach, expect, it, vi } from 'vitest'
import client from './client'
import { AUTH_SESSION_CLEARED_EVENT, AUTH_STORAGE_KEY } from './sessionStorage'

const originalAdapter = client.defaults.adapter

afterEach(() => {
  client.defaults.adapter = originalAdapter
  vi.unstubAllGlobals()
})

it('clears the session and preserves the current URL when a request returns 401', async () => {
  const assign = vi.fn()
  const dispatchEvent = vi.fn()
  vi.stubGlobal('window', {
    location: { pathname: '/products/12', search: '?ref=home', hash: '#details', assign },
    dispatchEvent,
  })
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify({
    accessToken: 'token',
    tokenType: 'Bearer',
    expiresIn: 3600,
    user: { id: 1, name: 'Ana', email: 'ana@example.com', role: 'CUSTOMER' },
  }))

  const adapter: AxiosAdapter = async (config) => {
    throw new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, undefined, {
      status: 401, statusText: 'Unauthorized', headers: {}, config, data: {},
    })
  }
  client.defaults.adapter = adapter

  await expect(client.get('/products/12')).rejects.toBeInstanceOf(AxiosError)
  expect(localStorage.getItem(AUTH_STORAGE_KEY)).toBeNull()
  expect(dispatchEvent).toHaveBeenCalledWith(expect.objectContaining({ type: AUTH_SESSION_CLEARED_EVENT }))
  expect(assign).toHaveBeenCalledWith(
    `/login?${new URLSearchParams({ returnTo: '/products/12?ref=home#details' })}`,
  )
})
