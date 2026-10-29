import { http, HttpResponse } from 'msw'
import { getSession, setSession } from '../auth/authStore'
import { API, server } from '../test/server'
import { ApiError, buildUrl, request } from './client'

const signIn = () => setSession({ accessToken: 'abc', tokenType: 'Bearer', expiresAt: Date.now() + 60_000 })

describe('buildUrl', () => {
  it('skips empty query values and encodes the rest', () => {
    const url = buildUrl('/v1/transactions', { q: 'кофе и чай', page: 0, categoryId: undefined, from: '', to: null })
    expect(url).toBe(`${API}/v1/transactions?q=%D0%BA%D0%BE%D1%84%D0%B5+%D0%B8+%D1%87%D0%B0%D0%B9&page=0`)
  })

  it('has no question mark without parameters', () => {
    expect(buildUrl('/v1/users/me')).toBe(`${API}/v1/users/me`)
  })
})

describe('request', () => {
  it('sends the token as "{tokenType} {accessToken}" and parses JSON', async () => {
    signIn()
    let authorization: string | null = null
    server.use(
      http.get(`${API}/v1/users/me`, ({ request: req }) => {
        authorization = req.headers.get('Authorization')
        return HttpResponse.json({ name: 'Алекс', email: 'a@b.c', balance: 10 })
      }),
    )

    await expect(request('/v1/users/me')).resolves.toEqual({ name: 'Алекс', email: 'a@b.c', balance: 10 })
    expect(authorization).toBe('Bearer abc')
  })

  it('does not send Authorization without a session', async () => {
    let header: string | null = 'unset'
    server.use(
      http.post(`${API}/auth/login`, ({ request: req }) => {
        header = req.headers.get('Authorization')
        return HttpResponse.json({ accessToken: 't', tokenType: 'Bearer', expiresIn: 60 })
      }),
    )
    await request('/auth/login', { method: 'POST', body: { email: 'a', password: 'b' } })
    expect(header).toBeNull()
  })

  it('sends a JSON body with the right content type', async () => {
    let received: { type: string | null; body: unknown } | null = null
    server.use(
      http.put(`${API}/v1/users/me/balance`, async ({ request: req }) => {
        received = { type: req.headers.get('Content-Type'), body: await req.json() }
        return HttpResponse.json({ balance: 5 })
      }),
    )
    await request('/v1/users/me/balance', { method: 'PUT', body: { balance: 5 } })
    expect(received).toEqual({ type: 'application/json', body: { balance: 5 } })
  })

  it('returns undefined for an empty response (DELETE)', async () => {
    server.use(http.delete(`${API}/v1/categories/3`, () => new HttpResponse(null, { status: 200 })))
    await expect(request('/v1/categories/3', { method: 'DELETE' })).resolves.toBeUndefined()
  })

  it('turns ErrorResponse into ApiError with code and field errors', async () => {
    server.use(
      http.post(`${API}/v1/transactions`, () =>
        HttpResponse.json(
          {
            status: 400,
            message: 'Validation failed',
            errorCode: 'VALIDATION_ERROR',
            timestamp: '2026-10-29T10:00:00',
            errors: { description: 'must not be blank' },
          },
          { status: 400 },
        ),
      ),
    )

    const error = await request('/v1/transactions', { method: 'POST', body: {} }).catch((e: unknown) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({
      status: 400,
      errorCode: 'VALIDATION_ERROR',
      message: 'Validation failed',
      fieldErrors: { description: 'must not be blank' },
    })
  })

  it('reads Retry-After from a rate limit response', async () => {
    server.use(
      http.post(`${API}/auth/login`, () =>
        HttpResponse.json(
          { status: 429, message: 'Too many', errorCode: 'RATE_LIMIT_EXCEEDED', timestamp: 'x' },
          { status: 429, headers: { 'Retry-After': '42' } },
        ),
      ),
    )
    const error = (await request('/auth/login', { method: 'POST', body: {} }).catch((e: unknown) => e)) as ApiError
    expect(error).toMatchObject({ status: 429, errorCode: 'RATE_LIMIT_EXCEEDED', retryAfter: 42 })
  })

  it('falls back to a generic error for a non-JSON failure', async () => {
    server.use(http.get(`${API}/v1/users/me`, () => new HttpResponse('<html>Bad gateway</html>', { status: 502 })))
    const error = (await request('/v1/users/me').catch((e: unknown) => e)) as ApiError
    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 502, errorCode: 'HTTP_502', fieldErrors: {} })
  })

  it('reports an unreachable server as NETWORK_ERROR with status 0', async () => {
    server.use(http.get(`${API}/v1/users/me`, () => HttpResponse.error()))
    const error = (await request('/v1/users/me').catch((e: unknown) => e)) as ApiError
    expect(error).toMatchObject({ status: 0, errorCode: 'NETWORK_ERROR' })
  })

  it('drops the session when a protected endpoint answers 401', async () => {
    signIn()
    server.use(
      http.get(`${API}/v1/users/me`, () =>
        HttpResponse.json(
          { status: 401, message: 'Authentication required', errorCode: 'UNAUTHORIZED', timestamp: 'x' },
          { status: 401 },
        ),
      ),
    )
    await expect(request('/v1/users/me')).rejects.toMatchObject({ status: 401 })
    expect(getSession()).toBeNull()
  })

  it('keeps the session when /auth/login answers 401 (wrong password is not an expired token)', async () => {
    signIn()
    server.use(
      http.post(`${API}/auth/login`, () =>
        HttpResponse.json(
          { status: 401, message: 'Invalid', errorCode: 'INVALID_CREDENTIALS', timestamp: 'x' },
          { status: 401 },
        ),
      ),
    )
    await expect(request('/auth/login', { method: 'POST', body: {} })).rejects.toMatchObject({
      errorCode: 'INVALID_CREDENTIALS',
    })
    expect(getSession()).not.toBeNull()
  })
})
