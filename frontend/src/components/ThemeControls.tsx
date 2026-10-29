import { IconButton, SegmentGroup, Span } from '@chakra-ui/react'
import { isThemeMode, useThemeMode } from '../theme/themeContext'
import { MODES } from './themeModes'

/** Переключатель из трёх иконок: солнце / луна / монитор. */
export function ThemeIconSegment() {
  const { mode, setMode } = useThemeMode()
  return (
    <SegmentGroup.Root
      size="sm"
      value={mode}
      onValueChange={(event) => isThemeMode(event.value) && setMode(event.value)}
    >
      <SegmentGroup.Indicator />
      {MODES.map(({ value, label, Icon }) => (
        <SegmentGroup.Item key={value} value={value} title={label}>
          <SegmentGroup.ItemText>
            <Icon size={14} aria-hidden />
            <Span srOnly>{label}</Span>
          </SegmentGroup.ItemText>
          <SegmentGroup.ItemHiddenInput />
        </SegmentGroup.Item>
      ))}
    </SegmentGroup.Root>
  )
}

/** Подписанный вариант для профиля: «Светлая | Тёмная | Системная». */
export function ThemeLabelSegment() {
  const { mode, setMode } = useThemeMode()
  return (
    <SegmentGroup.Root
      width="full"
      value={mode}
      onValueChange={(event) => isThemeMode(event.value) && setMode(event.value)}
    >
      <SegmentGroup.Indicator />
      <SegmentGroup.Items items={MODES.map(({ value, label }) => ({ value, label }))} flex="1" />
    </SegmentGroup.Root>
  )
}

/** Одна кнопка для телефона: по кругу light → dark → system, иконка показывает текущий режим. */
export function ThemeCycleButton() {
  const { mode, setMode } = useThemeMode()
  const index = MODES.findIndex((item) => item.value === mode)
  const current = MODES[index] ?? MODES[2]!
  const next = MODES[(index + 1) % MODES.length]!
  return (
    <IconButton
      aria-label={`Тема: ${current.label.toLowerCase()}. Переключить на «${next.label.toLowerCase()}»`}
      variant="ghost"
      size="md"
      onClick={() => setMode(next.value)}
    >
      <current.Icon />
    </IconButton>
  )
}
