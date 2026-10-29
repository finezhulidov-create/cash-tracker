import { ChakraProvider } from '@chakra-ui/react'
import { QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { useEffect, useState } from 'react'
import { BrowserRouter } from 'react-router-dom'
import { peekSession, subscribe } from '../auth/authStore'
import { Toasts } from '../components/Toasts'
import { system } from '../theme/system'
import { ThemeProvider } from '../theme/ThemeProvider'
import { createQueryClient } from './queryClient'

/** Тема (Chakra + светлая/тёмная/системная), кэш запросов, роутер и уведомления. */
export function Providers({ children }: { children: ReactNode }) {
  const [queryClient] = useState(createQueryClient)

  // Любой выход из сессии — кнопка «Выйти», истёкший токен, 401 от сервера — стирает кэш:
  // следующий вошедший в этой вкладке не должен увидеть чужие данные.
  useEffect(
    () =>
      subscribe(() => {
        if (!peekSession()) queryClient.clear()
      }),
    [queryClient],
  )

  return (
    <ChakraProvider value={system}>
      <ThemeProvider>
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>{children}</BrowserRouter>
          <Toasts />
        </QueryClientProvider>
      </ThemeProvider>
    </ChakraProvider>
  )
}
