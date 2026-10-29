import { ChakraProvider } from '@chakra-ui/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { setSession } from '../auth/authStore'
import { Toasts } from '../components/Toasts'
import { system } from '../theme/system'
import { ThemeProvider } from '../theme/ThemeProvider'


interface Options {
  route?: string
  /** Войти до рендера: в запросы уйдёт заголовок Authorization */
  authenticated?: boolean
}

/** Рендер с теми же провайдерами, что в приложении, но с MemoryRouter и свежим кэшем запросов. */
export function renderWithProviders(ui: ReactElement, { route = '/', authenticated = true }: Options = {}) {
  if (authenticated) {
    setSession({ accessToken: 'test-token', tokenType: 'Bearer', expiresAt: Date.now() + 3_600_000 })
  }
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, staleTime: 0 }, mutations: { retry: false } },
  })
  return {
    queryClient,
    ...render(
      <ChakraProvider value={system}>
        <ThemeProvider>
          <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
            <Toasts />
          </QueryClientProvider>
        </ThemeProvider>
      </ChakraProvider>,
    ),
  }
}
