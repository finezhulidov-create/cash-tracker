import { Box, Flex, Text } from '@chakra-ui/react'
import { Link, useLocation } from 'react-router-dom'
import type { UserDto } from '../api/types'
import { ICON_GRADIENT_ID } from '../components/GradientDefs'
import { Logo } from '../components/Logo'
import { UserAvatar } from '../components/UserAvatar'
import { isActivePath, NAV_ITEMS } from './nav'

interface SidebarProps {
  /** Планшет: узкая колонка из иконок без подписей */
  compact: boolean
  user?: UserDto
}

export function Sidebar({ compact, user }: SidebarProps) {
  const { pathname } = useLocation()

  return (
    <Flex
      as="aside"
      direction="column"
      gap="7"
      position="sticky"
      top="0"
      h="100vh"
      flexShrink={0}
      w={compact ? '76px' : '248px'}
      px={compact ? '3' : '4'}
      py="5"
      borderRightWidth="1px"
      borderColor="border.muted"
      bg="bg.panel/66"
      backdropFilter="blur(24px)"
    >
      <Flex align="center" h="40px" px={compact ? '2' : '2.5'}>
        <Logo showText={!compact} />
      </Flex>

      <Flex as="nav" aria-label="Основное меню" direction="column" gap="0.5">
        {NAV_ITEMS.map(({ to, label, Icon }) => {
          const active = isActivePath(pathname, to)
          return (
            <Link key={to} to={to} title={label} aria-current={active ? 'page' : undefined}>
              <Flex
                align="center"
                justify={compact ? 'center' : 'flex-start'}
                gap="3"
                h="40px"
                px="2.5"
                borderRadius="l2"
                fontSize="sm"
                fontWeight="500"
                bg={active ? 'bg.emphasized/60' : 'transparent'}
                color={active ? 'fg' : 'fg.muted'}
                transition="background 150ms, color 150ms"
                _hover={{ bg: active ? 'bg.emphasized/60' : 'bg.muted', color: 'fg' }}
              >
                <Icon
                  size={20}
                  aria-hidden
                  stroke={active ? `url(#${ICON_GRADIENT_ID})` : 'currentColor'}
                  style={{ flexShrink: 0 }}
                />
                {!compact && <span>{label}</span>}
              </Flex>
            </Link>
          )
        })}
      </Flex>

      <Box flex="1" />

      <Link to="/profile" title="Профиль" aria-label="Профиль">
        <Flex
          align="center"
          justify={compact ? 'center' : 'flex-start'}
          gap="2.5"
          p="2"
          borderWidth="1px"
          borderColor="border.muted"
          borderRadius="l3"
          bg="bg.panel"
          transition="background 150ms"
          _hover={{ bg: 'bg.muted' }}
        >
          <UserAvatar name={user?.name} />
          {!compact && (
            <Box flex="1" minW="0">
              <Text fontSize="sm" fontWeight="500" lineClamp="1">
                {user?.name ?? '…'}
              </Text>
              <Text fontSize="xs" color="fg.muted" lineClamp="1">
                {user?.email ?? ''}
              </Text>
            </Box>
          )}
        </Flex>
      </Link>
    </Flex>
  )
}
