import { Box, Field, Flex, Heading, Input, SegmentGroup, Span, Text } from '@chakra-ui/react'
import type { FormEvent } from 'react'
import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { errorMessage, isApiError } from '../../api/errors'
import { useLogin, useRegister } from '../../auth/useAuth'
import { Form } from '../../components/Form'
import { Aura } from '../../components/Aura'
import { Iridescent } from '../../components/Iridescent'
import { IridescentButton } from '../../components/IridescentButton'
import { Logo } from '../../components/Logo'
import { ThemeIconSegment } from '../../components/ThemeControls'
import { useIsPhone } from '../../layout/useLayoutMode'
import type { AuthErrors, AuthMode, AuthValues } from './validation'
import { validateAuth } from './validation'

const COPY = {
  login: {
    title: 'С возвращением',
    subtitle: 'Войдите, чтобы продолжить учёт трат',
    cta: 'Войти',
  },
  register: {
    title: 'Создать аккаунт',
    subtitle: 'Пара полей — и можно вести учёт',
    cta: 'Создать аккаунт',
  },
} as const

const FEATURES = [
  ['Сплиты.', 'Один чек — несколько категорий'],
  ['Бюджеты.', 'Лимит, потрачено и остаток на месяц'],
  ['Аналитика.', 'Траты по категориям и по месяцам'],
] as const

export function LoginPage() {
  const phone = useIsPhone()
  const navigate = useNavigate()
  const location = useLocation()
  const login = useLogin()
  const register = useRegister()

  const [mode, setMode] = useState<AuthMode>('login')
  const [values, setValues] = useState<AuthValues>({ name: '', email: '', password: '' })
  const [errors, setErrors] = useState<AuthErrors>({})

  const copy = COPY[mode]
  const pending = login.isPending || register.isPending
  const redirectTo = (location.state as { from?: string } | null)?.from ?? '/'

  function update(field: keyof AuthValues, value: string) {
    setValues((prev) => ({ ...prev, [field]: value }))
    setErrors((prev) => ({ ...prev, [field]: undefined }))
  }

  function switchMode(next: AuthMode) {
    setMode(next)
    setErrors({})
    login.reset()
    register.reset()
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    const found = validateAuth(mode, values)
    setErrors(found)
    if (Object.keys(found).length > 0) return

    const email = values.email.trim()
    try {
      if (mode === 'login') {
        await login.mutateAsync({ email, password: values.password })
      } else {
        await register.mutateAsync({ email, username: values.name.trim(), password: values.password })
      }
      navigate(redirectTo, { replace: true })
    } catch (error) {
      // Занятый email — ошибка поля email, остальное (пароль, лимит запросов, сеть) показываем у пароля
      if (isApiError(error, 'EMAIL_ALREADY_EXISTS')) setErrors({ email: errorMessage(error) })
      else setErrors({ password: errorMessage(error) })
    }
  }

  return (
    <Box position="relative" minH="100dvh">
      <Aura />
      <Flex position="relative" zIndex="1" minH="100dvh" direction={phone ? 'column' : 'row'}>
        <Iridescent
          flex={phone ? '0 0 auto' : '1.1 1 0'}
          minH={phone ? '300px' : '100dvh'}
          display="flex"
          flexDirection="column"
          justifyContent="space-between"
          gap="8"
          p={phone ? '24px 20px 32px' : '40px 48px'}
          borderRadius="0"
        >
          <Logo onFlow />
          <Flex direction="column" gap="4" maxW="560px">
            <Heading
              as="h1"
              fontWeight="600"
              fontSize={phone ? '36px' : '60px'}
              lineHeight="1.1"
              letterSpacing="-0.025em"
              textWrap="balance"
            >
              Каждый рубль —{' '}
              <Span
                bg="var(--ct-chip-bg)"
                borderRightWidth="2px"
                borderRightColor="currentColor"
                borderRadius="l2 0 0 l2"
                px="1.5"
              >
                на своём месте
              </Span>
            </Heading>
            <Text fontSize="lg" lineHeight="28px" color="var(--ct-on-flow-muted)" textWrap="pretty">
              Делите чеки по категориям, ставьте лимиты на месяц и смотрите, куда уходят деньги.
            </Text>
          </Flex>
          {!phone && (
            <Flex direction="column" gap="2.5" fontSize="sm">
              {FEATURES.map(([lead, rest]) => (
                <div key={lead}>
                  <b>{lead}</b> <Span color="var(--ct-on-flow-muted)">{rest}</Span>
                </div>
              ))}
            </Flex>
          )}
        </Iridescent>

        <Flex flex="1" align="center" justify="center" px="5" py="8">
          <Flex direction="column" gap="5" w="full" maxW="400px">
            <Flex justify="flex-end">
              <ThemeIconSegment />
            </Flex>

            <Box>
              <Heading as="h2" fontSize="3xl" lineHeight="36px" fontWeight="600" letterSpacing="-0.01em">
                {copy.title}
              </Heading>
              <Text mt="1.5" fontSize="sm" color="fg.muted">
                {copy.subtitle}
              </Text>
            </Box>

            <SegmentGroup.Root
              width="full"
              value={mode}
              onValueChange={(event) => {
                if (event.value === 'login' || event.value === 'register') switchMode(event.value)
              }}
            >
              <SegmentGroup.Indicator />
              <SegmentGroup.Items
                flex="1"
                items={[
                  { value: 'login', label: 'Вход' },
                  { value: 'register', label: 'Регистрация' },
                ]}
              />
            </SegmentGroup.Root>

            <Form display="flex" flexDirection="column" gap="4" noValidate onSubmit={onSubmit}>
              {mode === 'register' && (
                <Field.Root invalid={Boolean(errors.name)}>
                  <Field.Label>Имя</Field.Label>
                  <Input
                    size="lg"
                    placeholder="Как к вам обращаться"
                    autoComplete="nickname"
                    value={values.name}
                    onChange={(event) => update('name', event.currentTarget.value)}
                  />
                  <Field.ErrorText>{errors.name}</Field.ErrorText>
                </Field.Root>
              )}
              <Field.Root invalid={Boolean(errors.email)}>
                <Field.Label>Email</Field.Label>
                <Input
                  size="lg"
                  type="email"
                  placeholder="you@example.com"
                  autoComplete="email"
                  value={values.email}
                  onChange={(event) => update('email', event.currentTarget.value)}
                />
                <Field.ErrorText>{errors.email}</Field.ErrorText>
              </Field.Root>
              <Field.Root invalid={Boolean(errors.password)}>
                <Field.Label>Пароль</Field.Label>
                <Input
                  size="lg"
                  type="password"
                  placeholder="••••••••"
                  autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                  value={values.password}
                  onChange={(event) => update('password', event.currentTarget.value)}
                />
                <Field.ErrorText>{errors.password}</Field.ErrorText>
              </Field.Root>
              <IridescentButton type="submit" size="lg" w="full" loading={pending}>
                {copy.cta}
              </IridescentButton>
            </Form>
          </Flex>
        </Flex>
      </Flex>
    </Box>
  )
}
