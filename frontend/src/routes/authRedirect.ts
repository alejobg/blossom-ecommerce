import type { Location } from 'react-router-dom'

export function authOrigin(location: Location) {
  return {
    from: {
      pathname: location.pathname,
      search: location.search,
      hash: location.hash,
    },
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

export function getAuthRedirect(state: unknown, search: string): string {
  if (typeof state !== 'object' || state === null || !('from' in state)) {
    const returnTo = new URLSearchParams(search).get('returnTo')
    if (!returnTo || !returnTo.startsWith('/') || returnTo.startsWith('//') || returnTo.includes('\\')) {
      return '/'
    }
    const url = new URL(returnTo, window.location.origin)
    return url.origin === window.location.origin && isAllowedPath(url.pathname)
      ? `${url.pathname}${url.search}${url.hash}`
      : '/'
  }
  const from = state.from
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
  return `${pathname}${fromSearch}${hash}`
}
