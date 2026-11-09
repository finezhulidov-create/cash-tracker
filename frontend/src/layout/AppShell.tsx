import { Box, Flex } from '@chakra-ui/react'
import { useEffect } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { useMe } from '../api/hooks'
import { Aura } from '../components/Aura'
import { GradientDefs } from '../components/GradientDefs'
import { useTransactionDrawer } from '../features/transactions/drawerContext'
import { TransactionDrawerProvider } from '../features/transactions/TransactionDrawerProvider'
import { DesktopHeader, MobileHeader } from './Headers'
import { usePageMeta } from './pageMeta'
import { Sidebar } from './Sidebar'
import { TabBar } from './TabBar'
import { useLayoutMode } from './useLayoutMode'

const PAGE_PADDING = {
  phone: '16px 16px 104px',
  tablet: '24px 24px 48px',
  desktop: '28px 40px 56px',
} as const

function Frame() {
  const mode = useLayoutMode()
  const { pathname } = useLocation()
  const me = useMe()
  const { openNew } = useTransactionDrawer()
  const { title, subtitle } = usePageMeta(pathname, me.data)

  // Новый экран открывается сверху, а не на прокрутке предыдущего
  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])

  // Название экрана во вкладке браузера и в истории
  useEffect(() => {
    document.title = `${title} · Cash Tracker`
    return () => {
      document.title = 'Cash Tracker'
    }
  }, [title])

  return (
    <Flex position="relative" zIndex="1" minH="100dvh">
      {mode !== 'phone' && <Sidebar compact={mode === 'tablet'} user={me.data} />}
      <Flex as="main" id="main" direction="column" flex="1" minW="0">
        {mode === 'phone' ? (
          <MobileHeader title={title} user={me.data} />
        ) : (
          <DesktopHeader title={title} subtitle={subtitle} compact={mode === 'tablet'} onAdd={openNew} />
        )}
        <Box
          w="100%"
          maxW="1240px"
          mx="auto"
          display="flex"
          flexDirection="column"
          gap="4"
          style={{ padding: PAGE_PADDING[mode] }}
        >
          <Outlet />
        </Box>
      </Flex>
      {mode === 'phone' && <TabBar onAdd={openNew} />}
    </Flex>
  )
}

/** Каркас авторизованной части: аура, меню, шапка и выезжающая панель траты. */
export function AppShell() {
  return (
    <TransactionDrawerProvider>
      <GradientDefs />
      <Aura />
      <Frame />
    </TransactionDrawerProvider>
  )
}
