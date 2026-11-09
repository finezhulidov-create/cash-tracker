import type { CardRootProps } from '@chakra-ui/react'
import { Card, Flex, Heading, Text } from '@chakra-ui/react'
import type { ReactNode } from 'react'
import { useIsPhone } from '../layout/useLayoutMode'

interface GlassCardProps extends Omit<CardRootProps, 'title'> {
  title?: ReactNode
  subtitle?: ReactNode
  /** Кнопка или ссылка справа от заголовка */
  action?: ReactNode
  /** Убрать внутренние отступы (списки на всю ширину карточки) */
  flush?: boolean
}

/** «Стеклянная» карточка: полупрозрачная панель с размытием фона. */
export function GlassCard({ title, subtitle, action, flush, children, ...props }: GlassCardProps) {
  const phone = useIsPhone()
  const hasHeader = title !== undefined || action !== undefined

  return (
    <Card.Root
      variant="outline"
      size={phone ? 'sm' : 'lg'}
      bg="bg.panel/82"
      backdropFilter="blur(20px)"
      borderRadius="l3"
      {...props}
    >
      <Card.Body gap="0" p={flush ? '0' : undefined}>
        {hasHeader && (
          <Flex
            align="center"
            justify="space-between"
            gap="3"
            mb="4"
            px={flush ? { base: '4', md: '6' } : undefined}
            pt={flush ? { base: '4', md: '6' } : undefined}
          >
            <div>
              {title && (
                <Heading as="h2" size="md" fontWeight="600" letterSpacing="-0.005em">
                  {title}
                </Heading>
              )}
              {subtitle && (
                <Text fontSize="sm" color="fg.muted" mt="0.5">
                  {subtitle}
                </Text>
              )}
            </div>
            {action}
          </Flex>
        )}
        {children}
      </Card.Body>
    </Card.Root>
  )
}
