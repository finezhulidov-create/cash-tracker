import { Box, Flex, Heading, Text } from '@chakra-ui/react'
import { Plus } from 'lucide-react'
import { Link } from 'react-router-dom'
import type { UserDto } from '../api/types'
import { IridescentButton } from '../components/IridescentButton'
import { LogoMark } from '../components/Logo'
import { ThemeCycleButton, ThemeIconSegment } from '../components/ThemeControls'
import { UserAvatar } from '../components/UserAvatar'

const STICKY_GLASS = {
  position: 'sticky',
  top: '0',
  zIndex: 20,
  borderBottomWidth: '1px',
  borderColor: 'border.muted',
  bg: 'bg/70',
  backdropFilter: 'blur(20px)',
} as const

interface DesktopHeaderProps {
  title: string
  subtitle: string
  /** Планшет: уменьшенные боковые отступы */
  compact: boolean
  onAdd: () => void
}

/** Шапка на планшете и десктопе: заголовок экрана, переключатель темы, «Добавить трату». */
export function DesktopHeader({ title, subtitle, compact, onAdd }: DesktopHeaderProps) {
  return (
    <Flex as="header" align="center" gap="4" py="4" px={compact ? '6' : '10'} {...STICKY_GLASS}>
      <Box flex="1" minW="0">
        <Heading as="h1" fontSize="2xl" lineHeight="32px" fontWeight="600" letterSpacing="-0.01em">
          {title}
        </Heading>
        <Text fontSize="sm" color="fg.muted" lineClamp="1">
          {subtitle}
        </Text>
      </Box>
      <ThemeIconSegment />
      <IridescentButton onClick={onAdd}>
        <Plus aria-hidden />
        Добавить трату
      </IridescentButton>
    </Flex>
  )
}

interface MobileHeaderProps {
  title: string
  user?: UserDto
}

/** Шапка на телефоне: логотип, заголовок, смена темы и аватар (ведёт в профиль). */
export function MobileHeader({ title, user }: MobileHeaderProps) {
  return (
    <Flex as="header" align="center" gap="2.5" py="2.5" pl="4" pr="3" {...STICKY_GLASS}>
      <LogoMark size={28} />
      <Heading as="h1" flex="1" minW="0" fontSize="xl" lineHeight="28px" fontWeight="600" lineClamp="1">
        {title}
      </Heading>
      <ThemeCycleButton />
      <Link to="/profile" aria-label="Профиль">
        <UserAvatar name={user?.name} />
      </Link>
    </Flex>
  )
}
