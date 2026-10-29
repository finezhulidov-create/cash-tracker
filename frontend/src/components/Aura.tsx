import { Box } from '@chakra-ui/react'

const blob = (colorVar: string) => `radial-gradient(circle, var(${colorVar}), transparent 65%)`

/**
 * Фон-«аура»: три размытых цветных пятна под всем интерфейсом (размеры и позиции — как в прототипе).
 * Цвета и общая прозрачность берутся из CSS-переменных темы; медленный hue-rotate отключается при reduced motion.
 */
export function Aura() {
  return (
    <Box
      aria-hidden
      position="fixed"
      inset="0"
      zIndex="0"
      pointerEvents="none"
      overflow="hidden"
      opacity="var(--ct-aura-opacity)"
      animation="ctHue 40s linear infinite"
    >
      <Box
        position="absolute"
        top="-22%"
        right="-12%"
        w="720px"
        h="720px"
        borderRadius="full"
        background={blob('--ct-aura-1')}
      />
      <Box
        position="absolute"
        top="18%"
        left="-18%"
        w="620px"
        h="620px"
        borderRadius="full"
        background={blob('--ct-aura-2')}
      />
      <Box
        position="absolute"
        bottom="-26%"
        right="18%"
        w="680px"
        h="680px"
        borderRadius="full"
        background={blob('--ct-aura-3')}
      />
    </Box>
  )
}
