import type { Location } from 'react-router-dom'

export function authOrigin(location: Location) {
  return {
    from: `${location.pathname}${location.search}${location.hash}`,
  }
}

function isAllowedPath(pathname: string): boolean {
  return pathname.startsWith('/') &&
    !pathname.startsWith('//') &&
    !pathname.includes('\\') &&
    !pathname.includes('?') &&
    !pathname.includes('#') &&
    pathname !== '/login' &&
    pathname !== '/register'
}

function safeRedirect(value: unknown): string {
  if (typeof value !== 'string' || !value.startsWith('/') ||
      value.startsWith('//') || value.includes('\\')) {
    return '/'
  }
  const url = new URL(value, window.location.origin)
  return url.origin === window.location.origin && isAllowedPath(url.pathname)
    ? `${url.pathname}${url.search}${url.hash}`
    : '/'
}

export function getAuthRedirect(state: unknown, search: string): string {
  if (typeof state !== 'object' || state === null || !('from' in state)) {
    return safeRedirect(new URLSearchParams(search).get('returnTo'))
  }

  const from = state.from
  if (typeof from === 'string') return safeRedirect(from)
  if (typeof from !== 'object' || from === null || !('pathname' in from)) return '/'

  const { pathname } = from
  if (
    typeof pathname !== 'string' ||
    !isAllowedPath(pathname) ||
    new URL(pathname, window.location.origin).origin !== window.location.origin
  ) return '/'

  const fromSearch = 'search' in from && typeof from.search === 'string' && from.search.startsWith('?')
    ? from.search
    : ''
  const hash = 'hash' in from && typeof from.hash === 'string' && from.hash.startsWith('#')
    ? from.hash
    : ''
  return safeRedirect(`${pathname}${fromSearch}${hash}`)
}
