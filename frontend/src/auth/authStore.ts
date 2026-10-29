/**
 * Сессия: токен и момент его истечения. Лежит вне React, чтобы fetch-клиент мог читать токен
 * и сбрасывать сессию на 401 без контекстов. Хранится в localStorage (выбор по умолчанию для SPA без refresh-токена).
 */
export interface Session {
  accessToken: string
  tokenType: string
  /** Момент истечения, мс с начала эпохи */
  expiresAt: number
}

const STORAGE_KEY = 'ct.session'
/** setTimeout не умеет больше ~24.8 суток; токен живёт сутки, но подстрахуемся. */
const MAX_TIMEOUT = 2_147_483_647

let session: Session | null = null
let expiryTimer: ReturnType<typeof setTimeout> | undefined
const listeners = new Set<() => void>()

function readStored(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as Partial<Session>
    if (
      typeof parsed.accessToken === 'string' &&
      typeof parsed.tokenType === 'string' &&
      typeof parsed.expiresAt === 'number' &&
      parsed.expiresAt > Date.now()
    ) {
      return parsed as Session
    }
  } catch {
    // повреждённое значение или недоступное хранилище — считаем, что сессии нет
  }
  return null
}

function persist(next: Session | null): void {
  try {
    if (next) localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    else localStorage.removeItem(STORAGE_KEY)
  } catch {
    // приватный режим: сессия живёт только в памяти до перезагрузки
  }
}

function scheduleExpiry(next: Session | null): void {
  clearTimeout(expiryTimer)
  if (!next) return
  const delay = Math.min(Math.max(next.expiresAt - Date.now(), 0), MAX_TIMEOUT)
  expiryTimer = setTimeout(() => setSession(null), delay)
}

function init(): void {
  session = readStored()
  scheduleExpiry(session)
  if (!session) persist(null)
}

init()

/** Текущая сессия без побочных эффектов — для useSyncExternalStore. Истечение обрабатывает таймер. */
export function peekSession(): Session | null {
  return session
}

/** Сессия для запросов: просроченная считается отсутствующей и сразу сбрасывается. */
export function getSession(): Session | null {
  if (session && session.expiresAt <= Date.now()) {
    setSession(null)
  }
  return session
}

export function setSession(next: Session | null): void {
  session = next
  persist(next)
  scheduleExpiry(next)
  listeners.forEach((listener) => listener())
}

export function subscribe(listener: () => void): () => void {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

/** Заголовок Authorization в формате «{tokenType} {accessToken}» или null без сессии. */
export function authorizationHeader(): string | null {
  const current = getSession()
  return current ? `${current.tokenType} ${current.accessToken}` : null
}

/** Для тестов: перечитать состояние из localStorage. */
export function reloadSessionFromStorage(): void {
  clearTimeout(expiryTimer)
  init()
  listeners.forEach((listener) => listener())
}
