import { Box, Flex } from '@chakra-ui/react'
import type { ReactNode } from 'react'
import type { DonutSegment } from '../lib/donut'
import { donutBackground } from '../lib/donut'

interface DonutProps {
  segments: readonly DonutSegment[]
  /** Диаметр, px */
  size: number
  /** Толщина кольца, px */
  thickness: number
  /** Содержимое в центре */
  children?: ReactNode
  label?: string
}

export function Donut({ segments, size, thickness, children, label }: DonutProps) {
  const background = donutBackground(segments)
  const mask = `radial-gradient(farthest-side, transparent calc(100% - ${thickness}px), #000 calc(100% - ${thickness - 1}px))`

  return (
    <Box
      position="relative"
      flexShrink={0}
      w={`${size}px`}
      h={`${size}px`}
      role={label ? 'img' : undefined}
      aria-label={label}
    >
      <Box
        position="absolute"
        inset="0"
        borderRadius="full"
        bg={background ? undefined : 'bg.muted'}
        style={{ background: background ?? undefined, WebkitMask: mask, mask }}
      />
      <Flex
        position="absolute"
        inset={`${thickness}px`}
        direction="column"
        align="center"
        justify="center"
        textAlign="center"
      >
        {children}
      </Flex>
    </Box>
  )
}
