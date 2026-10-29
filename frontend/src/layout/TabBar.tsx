import { Box, Flex, Grid } from '@chakra-ui/react'
import { Plus } from 'lucide-react'
import { Link, useLocation } from 'react-router-dom'
import { ICON_GRADIENT_ID } from '../components/GradientDefs'
import type { NavItem } from './nav'
import { isActivePath, TAB_LEFT, TAB_RIGHT } from './nav'

function Tab({ item, active }: { item: NavItem; active: boolean }) {
  const { to, label, Icon } = item
  return (
    <Link to={to} aria-current={active ? 'page' : undefined}>
      <Flex
        direction="column"
        align="center"
        justify="center"
        gap="1"
        h="56px"
        fontSize="2xs"
        fontWeight="500"
        color={active ? 'fg' : 'fg.muted'}
      >
        <Icon size={22} aria-hidden stroke={active ? `url(#${ICON_GRADIENT_ID})` : 'currentColor'} />
        {label}
      </Flex>
    </Link>
  )
}

/** Нижний таб-бар телефона: два раздела, кнопка «+», ещё два раздела. */
export function TabBar({ onAdd }: { onAdd: () => void }) {
  const { pathname } = useLocation()

  return (
    <Grid
      as="nav"
      aria-label="Основное меню"
      position="fixed"
      left="0"
      right="0"
      bottom="0"
      zIndex="30"
      templateColumns="repeat(5, minmax(0, 1fr))"
      alignItems="center"
      h="calc(68px + env(safe-area-inset-bottom))"
      px="1.5"
      pb="env(safe-area-inset-bottom)"
      bg="bg.panel/80"
      backdropFilter="blur(24px)"
      borderTopWidth="1px"
      borderColor="border.muted"
    >
      {TAB_LEFT.map((item) => (
        <Tab key={item.to} item={item} active={isActivePath(pathname, item.to)} />
      ))}
      <Flex justify="center">
        <Box
          as="button"
          aria-label="Добавить трату"
          onClick={onAdd}
          display="flex"
          alignItems="center"
          justifyContent="center"
          w="56px"
          h="56px"
          mt="-26px"
          borderRadius="2xl"
          color="#111111"
          cursor="pointer"
          background="linear-gradient(135deg, #f472b6, #c084fc 30%, #67e8f9 65%, #fef08a)"
          boxShadow="0 10px 24px rgba(168, 85, 247, 0.4), 0 0 0 4px var(--chakra-colors-bg)"
          transition="transform 150ms"
          _active={{ transform: 'scale(0.96)' }}
        >
          <Plus size={26} aria-hidden />
        </Box>
      </Flex>
      {TAB_RIGHT.map((item) => (
        <Tab key={item.to} item={item} active={isActivePath(pathname, item.to)} />
      ))}
    </Grid>
  )
}
