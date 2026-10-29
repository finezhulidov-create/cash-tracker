import { Box } from '@chakra-ui/react'
import { useEffect, useState } from 'react'

interface ProgressBarProps {
  /** Доля заполнения в процентах; значения вне 0–100 обрезаются */
  percent: number
  /** Высота, px */
  height?: number
  /** CSS background заливки: градиент категории или цвет */
  fill: string
  /** CSS background трека; по умолчанию приглушённый фон темы */
  track?: string
  label?: string
}

/** Горизонтальный бар. При появлении заливка «вырастает» с нуля за 400 мс. */
export function ProgressBar({ percent, height = 8, fill, track, label }: ProgressBarProps) {
  const clamped = Math.min(100, Math.max(0, Number.isFinite(percent) ? percent : 0))
  const [ready, setReady] = useState(false)

  useEffect(() => {
    const frame = requestAnimationFrame(() => setReady(true))
    return () => cancelAnimationFrame(frame)
  }, [])

  return (
    <Box
      role="progressbar"
      aria-label={label}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={Math.round(clamped)}
      h={`${height}px`}
      borderRadius="full"
      overflow="hidden"
      bg={track ? undefined : 'bg.muted'}
      boxShadow={track ? undefined : 'inset'}
      style={track ? { background: track } : undefined}
    >
      <Box
        h="100%"
        borderRadius="full"
        transition="width 400ms ease"
        style={{ width: ready ? `${clamped}%` : '0%', background: fill }}
      />
    </Box>
  )
}
