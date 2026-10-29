import { Flex } from '@chakra-ui/react'
import { Split } from 'lucide-react'
import { createElement, useId } from 'react'
import {
  categoryColors,
  categoryGradientId,
  categoryIcon,
  categoryTint,
} from '../lib/categoryStyle'

interface CategoryIconProps {
  categoryId: number
  name: string
  /** Размер плашки, px */
  size?: number
  /** Размер иконки внутри, px; по умолчанию около половины плашки */
  iconSize?: number
}

/** Иконка категории на плашке: градиент категории поверх её же полупрозрачной подложки. */
export function CategoryIcon({ categoryId, name, size = 40, iconSize }: CategoryIconProps) {
  // Иконки — стабильные константы lucide, а не компоненты, создаваемые на лету
  const icon = createElement(categoryIcon(name), {
    size: iconSize ?? Math.round(size * 0.5),
    stroke: `url(#${categoryGradientId(categoryId)})`,
  })
  return (
    <Flex
      aria-hidden
      align="center"
      justify="center"
      flexShrink={0}
      w={`${size}px`}
      h={`${size}px`}
      borderRadius="l3"
      style={{ background: categoryTint(categoryId) }}
    >
      {icon}
    </Flex>
  )
}

interface SplitIconProps {
  /** Id первых двух категорий сплита — из них собирается градиент */
  categoryIds: readonly [number, number]
  size?: number
}

/** Иконка траты с несколькими частями: «split» с градиентом из двух категорий. */
export function SplitIcon({ categoryIds, size = 40 }: SplitIconProps) {
  const [first, second] = categoryIds
  const gradientId = `ct-split-${useId().replace(/[^a-zA-Z0-9_-]/g, '')}`
  return (
    <Flex
      aria-hidden
      align="center"
      justify="center"
      flexShrink={0}
      w={`${size}px`}
      h={`${size}px`}
      borderRadius="l3"
      style={{ background: categoryTint(second) }}
    >
      <svg width="0" height="0" focusable="false" style={{ position: 'absolute' }}>
        <defs>
          <linearGradient id={gradientId} gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="24" y2="24">
            <stop offset="0" stopColor={categoryColors(first).c1} />
            <stop offset="1" stopColor={categoryColors(second).c2} />
          </linearGradient>
        </defs>
      </svg>
      <Split size={Math.round(size * 0.5)} stroke={`url(#${gradientId})`} />
    </Flex>
  )
}
