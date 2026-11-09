import type { LucideIcon } from 'lucide-react'
import { Monitor, Moon, Sun } from 'lucide-react'
import type { ThemeMode } from '../theme/themeContext'

export const MODES: readonly { value: ThemeMode; label: string; Icon: LucideIcon }[] = [
  { value: 'light', label: 'Светлая', Icon: Sun },
  { value: 'dark', label: 'Тёмная', Icon: Moon },
  { value: 'system', label: 'Системная', Icon: Monitor },
]
