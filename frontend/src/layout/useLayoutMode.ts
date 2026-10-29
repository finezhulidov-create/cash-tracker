import { useSyncExternalStore } from 'react'

export type LayoutMode = 'phone' | 'tablet' | 'desktop'

/** Брейкпоинты из дизайна: < 720 телефон, 720–1099 планшет, ≥ 1100 десктоп. */
const PHONE_QUERY = '(max-width: 719px)'
const DESKTOP_QUERY = '(min-width: 1100px)'

function subscribe(onChange: () => void): () => void {
  const queries = [window.matchMedia(PHONE_QUERY), window.matchMedia(DESKTOP_QUERY)]
  queries.forEach((query) => query.addEventListener('change', onChange))
  return () => queries.forEach((query) => query.removeEventListener('change', onChange))
}

function getSnapshot(): LayoutMode {
  if (window.matchMedia(PHONE_QUERY).matches) return 'phone'
  if (window.matchMedia(DESKTOP_QUERY).matches) return 'desktop'
  return 'tablet'
}

export function useLayoutMode(): LayoutMode {
  return useSyncExternalStore(subscribe, getSnapshot, () => 'desktop')
}

export function useIsPhone(): boolean {
  return useLayoutMode() === 'phone'
}
