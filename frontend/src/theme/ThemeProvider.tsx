import type { ReactNode } from 'react'
import { useCallback, useEffect, useMemo, useState, useSyncExternalStore } from 'react'
import type { ResolvedTheme, ThemeMode } from './themeContext'
import { isThemeMode, THEME_STORAGE_KEY, ThemeContext } from './themeContext'

const DARK_QUERY = '(prefers-color-scheme: dark)'

function subscribeSystem(onChange: () => void): () => void {
  const query = window.matchMedia(DARK_QUERY)
  query.addEventListener('change', onChange)
  return () => query.removeEventListener('change', onChange)
}

const systemPrefersDark = () => window.matchMedia(DARK_QUERY).matches

function readStoredMode(): ThemeMode {
  try {
    const stored = localStorage.getItem(THEME_STORAGE_KEY)
    return isThemeMode(stored) ? stored : 'system'
  } catch {
    return 'system'
  }
}

/**
 * Светлая / тёмная / системная тема. Класс `.light` или `.dark` на <html> — это стандартный
 * механизм Chakra UI v3. Выбор хранится в localStorage, «системная» следит за настройкой устройства
 * в реальном времени, а изменения в соседней вкладке подхватываются через событие storage.
 */
export function ThemeProvider({ children }: { children: ReactNode }) {
  const [mode, setModeState] = useState<ThemeMode>(readStoredMode)
  const systemDark = useSyncExternalStore(subscribeSystem, systemPrefersDark, () => false)
  const resolved: ResolvedTheme = mode === 'system' ? (systemDark ? 'dark' : 'light') : mode

  useEffect(() => {
    const root = document.documentElement
    root.classList.remove('light', 'dark')
    root.classList.add(resolved)
    root.style.colorScheme = resolved
  }, [resolved])

  useEffect(() => {
    const onStorage = (event: StorageEvent) => {
      if (event.key === THEME_STORAGE_KEY) setModeState(isThemeMode(event.newValue) ? event.newValue : 'system')
    }
    window.addEventListener('storage', onStorage)
    return () => window.removeEventListener('storage', onStorage)
  }, [])

  const setMode = useCallback((next: ThemeMode) => {
    setModeState(next)
    try {
      localStorage.setItem(THEME_STORAGE_KEY, next)
    } catch {
      // приватный режим: тема действует до перезагрузки
    }
  }, [])

  const value = useMemo(() => ({ mode, resolved, setMode }), [mode, resolved, setMode])
  return <ThemeContext value={value}>{children}</ThemeContext>
}
