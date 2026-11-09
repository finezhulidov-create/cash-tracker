import { afterEach, vi } from 'vitest'
import {
  authorizationHeader,
  getSession,
  peekSession,
  reloadSessionFromStorage,
  setSession,
  subscribe,
} from './authStore'

const session = (expiresInMs: number) => ({
  accessToken: 'tok',
  tokenType: 'Bearer',
  expiresAt: Date.now() + expiresInMs,
})

afterEach(() => {
  vi.useRealTimers()
})

describe('authStore', () => {
  it('stores the session in localStorage and notifies subscribers', () => {
    const listener = vi.fn()
    const unsubscribe = subscribe(listener)

    setSession(session(60_000))

    expect(listener).toHaveBeenCalledTimes(1)
    expect(JSON.parse(localStorage.getItem('ct.session')!)).toMatchObject({ accessToken: 'tok', tokenType: 'Bearer' })
    expect(authorizationHeader()).toBe('Bearer tok')

    unsubscribe()
    setSession(null)
    expect(listener).toHaveBeenCalledTimes(1)
    expect(localStorage.getItem('ct.session')).toBeNull()
    expect(authorizationHeader()).toBeNull()
  })

  it('restores a valid session after a page reload', () => {
    setSession(session(60_000))
    reloadSessionFromStorage()
    expect(peekSession()?.accessToken).toBe('tok')
  })

  it('ignores an expired session on startup', () => {
    localStorage.setItem('ct.session', JSON.stringify(session(-1000)))
    reloadSessionFromStorage()
    expect(peekSession()).toBeNull()
    expect(localStorage.getItem('ct.session')).toBeNull()
  })

  it('ignores a corrupted value on startup', () => {
    localStorage.setItem('ct.session', '{not json')
    reloadSessionFromStorage()
    expect(peekSession()).toBeNull()

    localStorage.setItem('ct.session', JSON.stringify({ accessToken: 1 }))
    reloadSessionFromStorage()
    expect(peekSession()).toBeNull()
  })

  it('getSession drops a session that has just expired', () => {
    setSession(session(-1))
    expect(getSession()).toBeNull()
    expect(peekSession()).toBeNull()
  })

  it('logs out by itself when the token expires', () => {
    vi.useFakeTimers()
    const listener = vi.fn()
    subscribe(listener)
    setSession(session(5000))

    vi.advanceTimersByTime(4999)
    expect(peekSession()).not.toBeNull()

    vi.advanceTimersByTime(2)
    expect(peekSession()).toBeNull()
    expect(listener).toHaveBeenCalledTimes(2)
  })
})
