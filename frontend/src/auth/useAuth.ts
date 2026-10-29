import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useSyncExternalStore } from 'react'
import { authApi } from '../api/endpoints'
import type { TokenResponse } from '../api/types'
import type { Session } from './authStore'
import { peekSession, setSession, subscribe } from './authStore'

export function useSession(): Session | null {
  return useSyncExternalStore(subscribe, peekSession)
}

function toSession(token: TokenResponse): Session {
  return {
    accessToken: token.accessToken,
    tokenType: token.tokenType || 'Bearer',
    expiresAt: Date.now() + token.expiresIn * 1000,
  }
}

export function useLogin() {
  return useMutation({
    mutationFn: authApi.login,
    onSuccess: (token) => setSession(toSession(token)),
  })
}

export function useRegister() {
  return useMutation({
    mutationFn: authApi.register,
    onSuccess: (token) => setSession(toSession(token)),
  })
}

/** Выход: кэш запросов очищается, чтобы следующий пользователь не увидел чужие данные. */
export function useLogout() {
  const queryClient = useQueryClient()
  return () => {
    setSession(null)
    queryClient.clear()
  }
}
