import { Box, Button, chakra, Field, Flex, Grid, Heading, Input, Skeleton, Stack, Text } from '@chakra-ui/react'
import { ChevronRight, LogOut, Tags } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { errorMessage } from '../../api/errors'
import { useCategories, useMe, useUpdateBalance } from '../../api/hooks'
import { useLogout } from '../../auth/useAuth'
import { Form } from '../../components/Form'
import { GlassCard } from '../../components/GlassCard'
import { QueryError } from '../../components/QueryError'
import { ThemeLabelSegment } from '../../components/ThemeControls'
import { UserAvatar } from '../../components/UserAvatar'
import { centsToInput, formatMoney, parseAmount, sanitizeAmountInput, toCents } from '../../lib/money'
import { notify } from '../../lib/notify'
import { useThemeMode } from '../../theme/themeContext'

export function ProfilePage() {
  const me = useMe()
  const categories = useCategories()
  const logout = useLogout()
  const { mode, resolved } = useThemeMode()

  const themeHint =
    mode === 'system'
      ? `Повторяет настройки устройства — сейчас ${resolved === 'dark' ? 'тёмная' : 'светлая'}`
      : 'Выбрано вручную'

  if (me.isError) {
    return <QueryError error={me.error} onRetry={() => void me.refetch()} />
  }

  return (
    <Grid templateColumns="repeat(auto-fit, minmax(min(100%, 340px), 1fr))" gap="4" alignItems="start">
      <GlassCard>
        <Flex align="center" gap="4">
          <UserAvatar name={me.data?.name} size="xl" ring={3} />
          <Box minW="0">
            {me.data ? (
              <>
                <Heading as="h2" fontSize="xl" fontWeight="600" lineClamp="1">
                  {me.data.name}
                </Heading>
                <Text fontSize="sm" color="fg.muted" lineClamp="1">
                  {me.data.email}
                </Text>
              </>
            ) : (
              <Stack gap="2">
                <Skeleton h="5" w="140px" />
                <Skeleton h="4" w="180px" />
              </Stack>
            )}
          </Box>
        </Flex>
      </GlassCard>

      <BalanceCard balance={me.data?.balance} />

      <GlassCard>
        <Stack gap="3">
          <Text fontWeight="600">Тема оформления</Text>
          <ThemeLabelSegment />
          <Text fontSize="xs" color="fg.muted">
            {themeHint}
          </Text>
        </Stack>
      </GlassCard>

      <GlassCard>
        <Stack gap="1" m="-2">
          <Link to="/categories">
            <Flex
              align="center"
              gap="3"
              px="2"
              py="2.5"
              borderRadius="l2"
              fontSize="sm"
              fontWeight="500"
              transition="background 150ms"
              _hover={{ bg: 'bg.muted' }}
            >
              <Tags size={20} aria-hidden color="var(--chakra-colors-fg-muted)" />
              <Box flex="1">Категории</Box>
              <Text as="span" color="fg.muted" fontWeight="400">
                {categories.data?.length ?? ''}
              </Text>
              <ChevronRight size={16} aria-hidden color="var(--chakra-colors-fg-muted)" />
            </Flex>
          </Link>
          <chakra.button
            type="button"
            display="flex"
            alignItems="center"
            gap="3"
            px="2"
            py="2.5"
            borderRadius="l2"
            fontSize="sm"
            fontWeight="500"
            color="fg.error"
            textAlign="left"
            cursor="pointer"
            transition="background 150ms"
            _hover={{ bg: 'bg.error' }}
            onClick={logout}
          >
            <LogOut size={20} aria-hidden />
            <Box flex="1">Выйти</Box>
          </chakra.button>
        </Stack>
      </GlassCard>
    </Grid>
  )
}

function BalanceCard({ balance }: { balance: number | undefined }) {
  const update = useUpdateBalance()
  const [draft, setDraft] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault()
    const value = parseAmount(draft ?? '')
    if (value === null) {
      setError('Введите сумму, например 15 000 или 1 250,50')
      return
    }
    try {
      await update.mutateAsync(value)
      notify.success('Баланс обновлён', formatMoney(value))
      setDraft(null)
      setError(null)
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <GlassCard>
      <Stack gap="3">
        <Flex justify="space-between" align="center">
          <Text fontWeight="600">Баланс</Text>
          {draft === null && (
            <Button
              variant="outline"
              size="xs"
              disabled={balance === undefined}
              onClick={() => setDraft(balance === undefined ? '' : centsToInput(toCents(balance)))}
            >
              Изменить
            </Button>
          )}
        </Flex>

        {draft === null ? (
          balance === undefined ? (
            <Skeleton h="10" w="200px" />
          ) : (
            <Text
              className="ct-num"
              fontFamily="heading"
              fontSize="4xl"
              lineHeight="40px"
              fontWeight="600"
              letterSpacing="-0.025em"
              color="transparent"
              style={{
                background:
                  'linear-gradient(110deg, #ec4899, #a855f7, #3b82f6, #06b6d4, #14b8a6)',
                WebkitBackgroundClip: 'text',
                backgroundClip: 'text',
              }}
            >
              {formatMoney(balance)}
            </Text>
          )
        ) : (
          <Form display="flex" flexDirection="column" gap="2" onSubmit={onSubmit} noValidate>
            <Field.Root invalid={error !== null}>
              <Flex gap="2" w="full">
                <Input
                  size="lg"
                  inputMode="decimal"
                  autoFocus
                  autoComplete="off"
                  aria-label="Новый баланс"
                  value={draft}
                  onChange={(event) => {
                    setDraft(sanitizeAmountInput(event.currentTarget.value))
                    setError(null)
                  }}
                  onKeyDown={(event) => {
                    if (event.key === 'Escape') setDraft(null)
                  }}
                />
                <Button type="submit" size="lg" loading={update.isPending}>
                  Сохранить
                </Button>
              </Flex>
              <Field.ErrorText>{error}</Field.ErrorText>
            </Field.Root>
          </Form>
        )}

        <Text fontSize="xs" color="fg.muted">
          Задаётся вручную и не меняется от трат
        </Text>
      </Stack>
    </GlassCard>
  )
}
