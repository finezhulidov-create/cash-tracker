import { Box, Flex, Text } from '@chakra-ui/react'
import { Wallet } from 'lucide-react'

interface LogoMarkProps {
  size?: number
  /** На iridescent-панели: полупрозрачная плашка вместо градиента, цвета берутся из темы панели */
  onFlow?: boolean
}

/** Квадрат с iridescent-градиентом и иконкой кошелька. */
export function LogoMark({ size = 32, onFlow = false }: LogoMarkProps) {
  return (
    <Flex
      align="center"
      justify="center"
      flexShrink={0}
      w={`${size}px`}
      h={`${size}px`}
      borderRadius="l3"
      background={
        onFlow
          ? 'var(--ct-chip-bg)'
          : 'linear-gradient(135deg, #f9a8d4, #c084fc 35%, #67e8f9 70%, #99f6e4)'
      }
      color={onFlow ? 'var(--ct-on-flow)' : '#111111'}
    >
      <Wallet size={Math.round(size * 0.56)} strokeWidth={2} aria-hidden />
    </Flex>
  )
}

export function Logo({ showText = true, onFlow = false }: { showText?: boolean; onFlow?: boolean }) {
  return (
    <Flex align="center" gap="2.5">
      <LogoMark onFlow={onFlow} />
      {showText && (
        <Box>
          <Text fontFamily="heading" fontWeight="600" fontSize="lg" lineHeight="1" whiteSpace="nowrap">
            Cash Tracker
          </Text>
        </Box>
      )}
    </Flex>
  )
}
