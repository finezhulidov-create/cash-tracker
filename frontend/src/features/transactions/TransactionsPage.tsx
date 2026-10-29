import {
  Badge,
  Box,
  Button,
  EmptyState,
  Field,
  Flex,
  Grid,
  Input,
  InputGroup,
  NativeSelect,
  Skeleton,
  Stack,
  Text,
  VStack,
} from '@chakra-ui/react'
import { Receipt, Search, SlidersHorizontal } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useCategories, useTransactionsInfinite } from '../../api/hooks'
import { GlassCard } from '../../components/GlassCard'
import { QueryError } from '../../components/QueryError'
import { TransactionRow } from '../../components/TransactionRow'
import { useLayoutMode } from '../../layout/useLayoutMode'
import { formatDayHeading } from '../../lib/dates'
import { formatMoney, sanitizeAmountInput } from '../../lib/money'
import { FORMS, plural } from '../../lib/plural'
import { groupByDay, sumAmounts } from '../../lib/transactionView'
import { useTransactionDrawer } from './drawerContext'
import type { FilterFields } from './filters'
import {
  countActiveFilters,
  EMPTY_FILTERS,
  filtersToParams,
  hasAnyFilter,
  parseFilters,
  toApiFilters,
  validateFilters,
} from './filters'

/** Пауза после последнего нажатия в текстовых полях, прежде чем отправлять запрос. */
const TYPING_DELAY = 300

export function TransactionsPage() {
  const mode = useLayoutMode()
  const [params, setParams] = useSearchParams()
  const { openEdit, openNew } = useTransactionDrawer()
  const categories = useCategories()

  // fields — то, что видно в полях; applied — то, что ушло в запрос и в URL (текст — с паузой)
  const [fields, setFields] = useState<FilterFields>(() => parseFilters(params))
  const [applied, setApplied] = useState<FilterFields>(fields)

  useEffect(() => {
    if (fields === applied) return
    const textUnchanged =
      fields.q === applied.q && fields.min === applied.min && fields.max === applied.max
    const timer = setTimeout(() => setApplied(fields), textUnchanged ? 0 : TYPING_DELAY)
    return () => clearTimeout(timer)
  }, [fields, applied])

  useEffect(() => {
    const next = filtersToParams(applied)
    if (next.toString() !== params.toString()) setParams(next, { replace: true })
  }, [applied, params, setParams])

  const activeCount = countActiveFilters(fields)
  const filterError = validateFilters(fields)
  const apiFilters = useMemo(() => toApiFilters(applied), [applied])

  // Панель фильтров по умолчанию открыта на десктопе или если фильтры пришли из ссылки
  const [filtersOpen, setFiltersOpen] = useState(mode === 'desktop' || activeCount > 0)

  const setField = (key: keyof FilterFields, value: string) =>
    setFields((prev) => ({ ...prev, [key]: value }))

  const resetAll = () => setFields(EMPTY_FILTERS)
  const resetFilters = () => setFields((prev) => ({ ...EMPTY_FILTERS, q: prev.q }))

  const query = useTransactionsInfinite(apiFilters, !validateFilters(applied))
  const transactions = useMemo(
    () => query.data?.pages.flatMap((page) => page.content) ?? [],
    [query.data],
  )
  const totalCount = query.data?.pages[0]?.page.totalElements ?? 0
  const allLoaded = !query.hasNextPage
  const groups = useMemo(() => groupByDay(transactions), [transactions])
  const today = useMemo(() => new Date(), [])

  return (
    <Stack gap="4">
      <Flex gap="3" wrap="wrap" align="center">
        <Box flex="1 1 260px" minW="0">
          <InputGroup startElement={<Search size={16} aria-hidden />}>
            <Input
              value={fields.q}
              onChange={(event) => setField('q', event.currentTarget.value)}
              placeholder="Поиск по описанию"
              aria-label="Поиск по описанию"
              autoComplete="off"
              maxLength={255}
            />
          </InputGroup>
        </Box>
        <Button
          variant={filtersOpen ? 'subtle' : 'outline'}
          aria-expanded={filtersOpen}
          onClick={() => setFiltersOpen((open) => !open)}
        >
          <SlidersHorizontal aria-hidden />
          Фильтры
          {activeCount > 0 && (
            <Badge colorPalette="purple" variant="solid" borderRadius="full" ml="1">
              {activeCount}
            </Badge>
          )}
        </Button>
      </Flex>

      {filtersOpen && (
        <GlassCard>
          <Stack gap="3.5">
            <Grid templateColumns="repeat(auto-fill, minmax(min(100%, 170px), 1fr))" gap="3">
              <Field.Root>
                <Field.Label>Категория</Field.Label>
                <NativeSelect.Root>
                  <NativeSelect.Field
                    value={fields.cat}
                    onChange={(event) => setField('cat', event.currentTarget.value)}
                  >
                    <option value="">Все категории</option>
                    {categories.data?.map((category) => (
                      <option key={category.id} value={category.id}>
                        {category.categoryName}
                      </option>
                    ))}
                  </NativeSelect.Field>
                  <NativeSelect.Indicator />
                </NativeSelect.Root>
              </Field.Root>
              <Field.Root invalid={filterError?.startsWith('Сумма') ?? false}>
                <Field.Label>Сумма от, ₽</Field.Label>
                <Input
                  value={fields.min}
                  onChange={(event) => setField('min', sanitizeAmountInput(event.currentTarget.value))}
                  placeholder="0"
                  inputMode="decimal"
                  autoComplete="off"
                />
              </Field.Root>
              <Field.Root invalid={filterError?.startsWith('Сумма') ?? false}>
                <Field.Label>Сумма до, ₽</Field.Label>
                <Input
                  value={fields.max}
                  onChange={(event) => setField('max', sanitizeAmountInput(event.currentTarget.value))}
                  placeholder="∞"
                  inputMode="decimal"
                  autoComplete="off"
                />
              </Field.Root>
              <Field.Root invalid={filterError?.startsWith('Дата') ?? false}>
                <Field.Label>Дата с</Field.Label>
                <Input
                  type="date"
                  value={fields.from}
                  onChange={(event) => setField('from', event.currentTarget.value)}
                />
              </Field.Root>
              <Field.Root invalid={filterError?.startsWith('Дата') ?? false}>
                <Field.Label>Дата по</Field.Label>
                <Input
                  type="date"
                  value={fields.to}
                  onChange={(event) => setField('to', event.currentTarget.value)}
                />
              </Field.Root>
            </Grid>
            {filterError && (
              <Text role="alert" fontSize="sm" color="fg.error">
                {filterError}
              </Text>
            )}
            <Flex justify="flex-end">
              <Button variant="ghost" size="sm" disabled={activeCount === 0} onClick={resetFilters}>
                Сбросить фильтры
              </Button>
            </Flex>
          </Stack>
        </GlassCard>
      )}

      {query.isError ? (
        <QueryError error={query.error} onRetry={() => void query.refetch()} />
      ) : query.isPending && !filterError ? (
        <ListSkeleton />
      ) : filterError || transactions.length === 0 ? (
        <GlassCard>
          <EmptyState.Root>
            <EmptyState.Content>
              <EmptyState.Indicator>
                <Receipt />
              </EmptyState.Indicator>
              {hasAnyFilter(fields) ? (
                <>
                  <VStack textAlign="center">
                    <EmptyState.Title>Ничего не найдено</EmptyState.Title>
                    <EmptyState.Description>
                      Попробуйте изменить фильтры или сбросить их
                    </EmptyState.Description>
                  </VStack>
                  <Button variant="outline" size="sm" onClick={resetAll}>
                    Сбросить всё
                  </Button>
                </>
              ) : (
                <>
                  <VStack textAlign="center">
                    <EmptyState.Title>Трат пока нет</EmptyState.Title>
                    <EmptyState.Description>
                      Добавьте первую трату — она появится здесь
                    </EmptyState.Description>
                  </VStack>
                  <Button variant="outline" size="sm" onClick={openNew}>
                    Добавить трату
                  </Button>
                </>
              )}
            </EmptyState.Content>
          </EmptyState.Root>
        </GlassCard>
      ) : (
        <>
          <Flex justify="space-between" align="baseline" wrap="wrap" gap="2" px="1" fontSize="sm">
            <Text color="fg.muted">
              {totalCount} {plural(totalCount, FORMS.operation)}
            </Text>
            <Text>
              {allLoaded ? 'Итого' : 'Загружено на'}{' '}
              <b className="ct-num">{formatMoney(sumAmounts(transactions))}</b>
            </Text>
          </Flex>

          {groups.map((group) => (
            <Stack key={group.date} gap="2">
              <Flex justify="space-between" px="1" fontSize="xs" color="fg.muted" fontWeight="500">
                <span>{formatDayHeading(group.date, today)}</span>
                <span className="ct-num">{formatMoney(group.total)}</span>
              </Flex>
              <Box
                borderWidth="1px"
                borderColor="border"
                borderRadius="l3"
                bg="bg.panel/82"
                backdropFilter="blur(20px)"
                p="1"
              >
                {group.items.map((transaction) => (
                  <TransactionRow
                    key={transaction.id}
                    transaction={transaction}
                    variant="list"
                    onOpen={openEdit}
                  />
                ))}
              </Box>
            </Stack>
          ))}

          {query.hasNextPage && (
            <Flex justify="center" pt="1">
              <Button
                variant="outline"
                loading={query.isFetchingNextPage}
                onClick={() => void query.fetchNextPage()}
              >
                Показать ещё
              </Button>
            </Flex>
          )}
        </>
      )}
    </Stack>
  )
}

function ListSkeleton() {
  return (
    <Stack gap="4" aria-busy="true" aria-label="Загрузка операций">
      {[0, 1].map((group) => (
        <Stack key={group} gap="2">
          <Skeleton h="4" w="40%" />
          <Stack gap="1" p="1" borderWidth="1px" borderColor="border" borderRadius="l3" bg="bg.panel/82">
            {[0, 1, 2].map((row) => (
              <Flex key={row} align="center" gap="3.5" px="3" py="2.5">
                <Skeleton w="40px" h="40px" borderRadius="l3" />
                <Stack flex="1" gap="2">
                  <Skeleton h="4" w="55%" />
                  <Skeleton h="3" w="30%" />
                </Stack>
                <Skeleton h="4" w="64px" />
              </Flex>
            ))}
          </Stack>
        </Stack>
      ))}
    </Stack>
  )
}
