import { createContext, useContext } from 'react'

export type ThemeMode = 'light' | 'dark' | 'system'
export type ResolvedTheme = 'light' | 'dark'

/** Ключ localStorage; тот же читает предзагрузочный скрипт в index.html. */
export const THEME_STORAGE_KEY = 'ct.theme'

export interface ThemeContextValue {
  /** Выбор пользователя */
  mode: ThemeMode
  /** Что реально включено: для «системной» — по настройке устройства */
  resolved: ResolvedTheme
  setMode: (mode: ThemeMode) => void
}

export const ThemeContext = createContext<ThemeContextValue | null>(null)

export function isThemeMode(value: string | null | undefined): value is ThemeMode {
  return value === 'light' || value === 'dark' || value === 'system'
}

export function useThemeMode(): ThemeContextValue {
  const value = useContext(ThemeContext)
  if (!value) throw new Error('useThemeMode нужно вызывать внутри ThemeProvider')
  return value
}
