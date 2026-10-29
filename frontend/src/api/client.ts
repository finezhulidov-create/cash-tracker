import { authorizationHeader, setSession } from '../auth/authStore'
import type { ErrorResponse } from './types'

const BASE_URL: string = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '')

/** Ошибка API в формате ErrorResponse бэкенда; status 0 — сервер недоступен. */
export class ApiError extends Error {
  readonly status: number
  readonly errorCode: string
  /** Ошибки валидации по полям: имя поля → сообщение */
  readonly fieldErrors: Record<string, string>
  /** Через сколько секунд повторить (заголовок Retry-After при 429) */
  readonly retryAfter?: number

  constructor(
    status: number,
    message: string,
    errorCode: string,
    fieldErrors: Record<string, string> = {},
    retryAfter?: number,
  ) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.errorCode = errorCode
    this.fieldErrors = fieldErrors
    this.retryAfter = retryAfter
  }
}

export type QueryValue = string | number | boolean | null | undefined
export type QueryParams = Record<string, QueryValue>

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  query?: QueryParams
  body?: unknown
  signal?: AbortSignal
}

export function buildUrl(path: string, query?: QueryParams): string {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value === undefined || value === null || value === '') continue
    params.append(key, String(value))
  }
  const queryString = params.toString()
  return `${BASE_URL}${path}${queryString ? `?${queryString}` : ''}`
}

async function toApiError(response: Response): Promise<ApiError> {
  const retryAfterHeader = Number(response.headers.get('Retry-After'))
  const retryAfter = Number.isFinite(retryAfterHeader) && retryAfterHeader > 0 ? retryAfterHeader : undefined
  try {
    const body = (await response.json()) as Partial<ErrorResponse>
    return new ApiError(
      response.status,
      body.message ?? response.statusText,
      body.errorCode ?? `HTTP_${response.status}`,
      body.errors ?? {},
      retryAfter,
    )
  } catch {
    return new ApiError(response.status, response.statusText, `HTTP_${response.status}`, {}, retryAfter)
  }
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', query, body, signal } = options
  const headers: Record<string, string> = { Accept: 'application/json' }
  const authorization = authorizationHeader()
  if (authorization) headers.Authorization = authorization
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  let response: Response
  try {
    response = await fetch(buildUrl(path, query), {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    throw new ApiError(0, 'Сервер недоступен', 'NETWORK_ERROR')
  }

  if (!response.ok) {
    const error = await toApiError(response)
    // Токен отозван или истёк. Ответы /auth/** с 401 — это просто неверный пароль, сессию они не затрагивают.
    if (response.status === 401 && !path.startsWith('/auth/')) setSession(null)
    throw error
  }

  const text = await response.text()
  return (text ? JSON.parse(text) : undefined) as T
}
