import type { QueryClient } from '@tanstack/react-query'
import { useQueryClient } from '@tanstack/react-query'
import { act, render } from '@testing-library/react'
import { setSession } from '../auth/authStore'
import { Providers } from './Providers'

function renderProviders() {
  let client!: QueryClient
  function Grab() {
    client = useQueryClient()
    return null
  }
  render(
    <Providers>
      <Grab />
    </Providers>,
  )
  return client
}

const signIn = () => setSession({ accessToken: 't', tokenType: 'Bearer', expiresAt: Date.now() + 60_000 })

describe('Providers', () => {
  it('wipes the query cache when the session ends, so the next user never sees old data', () => {
    signIn()
    const client = renderProviders()
    client.setQueryData(['transactions', 'list'], ['чужие траты'])
    expect(client.getQueryData(['transactions', 'list'])).toEqual(['чужие траты'])

    act(() => setSession(null))

    expect(client.getQueryData(['transactions', 'list'])).toBeUndefined()
    expect(client.getQueryCache().getAll()).toHaveLength(0)
  })

  it('keeps the cache while the user stays signed in', () => {
    signIn()
    const client = renderProviders()
    client.setQueryData(['me'], { name: 'Алекс' })

    act(() => signIn()) // например, повторный вход обновляет токен

    expect(client.getQueryData(['me'])).toEqual({ name: 'Алекс' })
  })
})
