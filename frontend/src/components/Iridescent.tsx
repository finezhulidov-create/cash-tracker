import type { BoxProps } from '@chakra-ui/react'
import { Box } from '@chakra-ui/react'

const GLOSS =
  'radial-gradient(90% 120% at 0% 0%, rgba(255,255,255,.45), transparent 55%), radial-gradient(60% 90% at 100% 100%, rgba(255,255,255,.18), transparent 60%)'

/**
 * Поверхность с переливом (hero, панель входа, общий бюджет): медленно «плывущий» градиент,
 * поверх него блик, а контент лежит над обоими слоями. Цвета текста задают переменные темы.
 */
export function Iridescent({ children, ...props }: BoxProps) {
  return (
    <Box
      position="relative"
      overflow="hidden"
      isolation="isolate"
      color="var(--ct-on-flow)"
      {...props}
    >
      <Box
        aria-hidden
        position="absolute"
        inset="0"
        zIndex="-2"
        background="var(--ct-flow)"
        backgroundSize="300% 300%"
        animation="ctFlow 12s ease-in-out infinite alternate, ctHue 30s linear infinite"
      />
      <Box
        aria-hidden
        position="absolute"
        inset="0"
        zIndex="-1"
        background={GLOSS}
        mixBlendMode="soft-light"
        pointerEvents="none"
      />
      {children}
    </Box>
  )
}
