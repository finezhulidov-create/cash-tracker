import {
  Badge,
  Box,
  Button,
  EmptyState,
  Field,
  Flex,
  Grid,
  IconButton,
  Input,
  NativeSelect,
  Skeleton,
  Span,
  Stack,
  Text,
  VStack,
} from '@chakra-ui/react'
import { Check, PiggyBank, Pencil, Plus, Trash2, X } from 'lucide-react'
import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { errorMessage } from '../../api/errors'
import {
  useCategories,
  useCreateBudget,
  useDeleteBudget,
  useMonthSummary,
  useUpdateBudget,
} from '../../api/hooks'
import type { BudgetStatusDto, CategoryDto } from '../../api/types'
import { CategoryIcon } from '../../components/CategoryIcon'
import { Form } from '../../components/Form'
import { GlassCard } from '../../components/GlassCard'
import { Iridescent } from '../../components/Iridescent'
import { ProgressBar } from '../../components/ProgressBar'
import { PeriodStepper } from '../../components/PeriodStepper'
import { QueryError } from '../../components/QueryError'
import { useIsPhone } from '../../layout/useLayoutMode'
import { budgetProgress } from '../../lib/budget'
import { categoryBarGradient, categoryGradient, OVERSPENT_GRADIENT } from '../../lib/categoryStyle'
import { addMonths, monthName, monthTitle, yearMonthOf } from '../../lib/dates'
import { centsToInput, formatMoney, parseAmount, sanitizeAmountInput, toCents } from '../../lib/money'
import { notify } from '../../lib/notify'
import { unbudgetedCategories } from './budgetForm'

/** Значение «На что» для общего бюджета месяца. */
const OVERALL = 'overall'

interface FormState {
  target: string
  limit: string
}

interface EditState {
  budgetId: number
  value: string
}

export function BudgetsPage() {
  const [yearMonth, setYearMonth] = useState(() => yearMonthOf(new Date()))
  const summary = useMonthSummary(yearMonth)
  const categories = useCategories()
  const [form, setForm] = useState<FormState | null>(null)

  const rows = useMemo(
    () =>
      [...(summary.data?.categories ?? [])].sort((a, b) =>
        (a.category?.categoryName ?? '').localeCompare(b.category?.categoryName ?? '', 'ru'),
      ),
    [summary.data],
  )
  const overall = summary.data?.overall ?? null
  const free = useMemo(
    () => unbudgetedCategories(categories.data ?? [], summary.data?.categories ?? []),
    [categories.data, summary.data],
  )
  const formOptions = overall ? free.length : free.length + 1
  const monthEmpty = summary.data !== undefined && !overall && rows.length === 0

  function changeMonth(delta: number) {
    setYearMonth((current) => addMonths(current, delta))
    setForm(null)
  }

  function openForm(target?: string) {
    setForm({ target: target ?? (overall ? String(free[0]?.id ?? '') : OVERALL), limit: '' })
  }

  return (
    <Stack gap="4">
      <Flex align="center" wrap="wrap" gap="3">
        <PeriodStepper
          label={monthTitle(yearMonth)}
          prevLabel="Предыдущий месяц"
          nextLabel="Следующий месяц"
          onPrev={() => changeMonth(-1)}
          onNext={() => changeMonth(1)}
        />
        <Box flex="1" />
        <Button
          variant="outline"
          size="sm"
          disabled={summary.isPending || formOptions === 0}
          onClick={() => openForm()}
        >
          <Plus aria-hidden />
          Новый бюджет
        </Button>
      </Flex>

      {form && (
        <BudgetForm
          key={form.target}
          initial={form}
          yearMonth={yearMonth}
          overallTaken={overall !== null}
          free={free}
          onClose={() => setForm(null)}
        />
      )}

      {summary.isError ? (
        <QueryError error={summary.error} onRetry={() => void summary.refetch()} />
      ) : summary.isPending ? (
        <BudgetsSkeleton />
      ) : (
        <>
          {overall ? (
            <OverallCard status={overall} yearMonth={yearMonth} />
          ) : (
            <Flex
              align="center"
              justify="space-between"
              wrap="wrap"
              gap="3"
              p="5"
              borderWidth="1px"
              borderStyle="dashed"
              borderColor="border.emphasized"
              borderRadius="l3"
            >
              <Box>
                <Text fontWeight="600">Общий бюджет не задан</Text>
                <Text fontSize="sm" color="fg.muted">
                  Лимит на все траты месяца, независимо от категорий
                </Text>
              </Box>
              <Button variant="outline" size="sm" onClick={() => openForm(OVERALL)}>
                Задать лимит
              </Button>
            </Flex>
          )}

          {rows.length > 0 && (
            <Grid templateColumns="repeat(auto-fill, minmax(min(100%, 280px), 1fr))" gap="4">
              {rows.map((status) => (
                <CategoryBudgetCard key={status.budgetId} status={status} yearMonth={yearMonth} />
              ))}
            </Grid>
          )}

          {free.length > 0 && (overall || rows.length > 0) && (
            <Flex align="center" wrap="wrap" gap="2">
              <Text fontSize="sm" color="fg.muted" mr="1">
                Без лимита:
              </Text>
              {free.map((category) => (
                <Button
                  key={category.id}
                  variant="outline"
                  size="xs"
                  onClick={() => openForm(String(category.id))}
                >
                  <Span
                    aria-hidden
                    w="8px"
                    h="8px"
                    borderRadius="full"
                    style={{ background: categoryGradient(category.id) }}
                  />
                  {category.categoryName}
                  <Plus aria-hidden />
                </Button>
              ))}
            </Flex>
          )}

          {monthEmpty && (
            <GlassCard>
              <EmptyState.Root>
                <EmptyState.Content>
                  <EmptyState.Indicator>
                    <PiggyBank />
                  </EmptyState.Indicator>
                  <VStack textAlign="center">
                    <EmptyState.Title>На {monthName(Number(yearMonth.slice(5)))} бюджетов нет</EmptyState.Title>
                    <EmptyState.Description>
                      Задайте общий лимит или лимиты по категориям — остаток посчитается по реальным тратам
                    </EmptyState.Description>
                  </VStack>
                  {categories.data?.length === 0 && (
                    <Button asChild variant="outline" size="sm">
                      <Link to="/categories">Создать категорию</Link>
                    </Button>
                  )}
                </EmptyState.Content>
              </EmptyState.Root>
            </GlassCard>
          )}
        </>
      )}
    </Stack>
  )
}

// ---------- форма нового бюджета ----------

interface BudgetFormProps {
  initial: FormState
  yearMonth: string
  overallTaken: boolean
  free: readonly CategoryDto[]
  onClose: () => void
}

function BudgetForm({ initial, yearMonth, overallTaken, free, onClose }: BudgetFormProps) {
  const create = useCreateBudget()
  const [target, setTarget] = useState(initial.target)
  const [limit, setLimit] = useState(initial.limit)
  const [error, setError] = useState<string | null>(null)

  const amount = parseAmount(limit)
  const valid = target !== '' && amount !== null && amount > 0

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    if (!valid || amount === null) return
    const categoryId = target === OVERALL ? null : Number(target)
    try {
      await create.mutateAsync({ amount, period: `${yearMonth}-01`, categoryId })
      const name =
        categoryId === null
          ? 'Общий бюджет'
          : (free.find((category) => category.id === categoryId)?.categoryName ?? 'Бюджет')
      notify.success('Бюджет создан', `${name}: ${formatMoney(amount)} на ${monthName(Number(yearMonth.slice(5)))}`)
      onClose()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <GlassCard>
      <Form display="flex" flexWrap="wrap" gap="3" alignItems="flex-end" onSubmit={submit} noValidate>
        <Box flex="1 1 220px" minW="0">
          <Field.Root>
            <Field.Label>На что</Field.Label>
            <NativeSelect.Root>
              <NativeSelect.Field value={target} onChange={(event) => setTarget(event.currentTarget.value)}>
                {!overallTaken && <option value={OVERALL}>Общий бюджет на месяц</option>}
                {free.map((category) => (
                  <option key={category.id} value={category.id}>
                    {category.categoryName}
                  </option>
                ))}
              </NativeSelect.Field>
              <NativeSelect.Indicator />
            </NativeSelect.Root>
          </Field.Root>
        </Box>
        <Box flex="1 1 160px" minW="0">
          <Field.Root invalid={error !== null}>
            <Field.Label>Лимит на месяц, ₽</Field.Label>
            <Input
              placeholder="10 000"
              inputMode="decimal"
              autoComplete="off"
              autoFocus
              value={limit}
              onChange={(event) => {
                setLimit(sanitizeAmountInput(event.currentTarget.value))
                setError(null)
              }}
            />
            <Field.ErrorText>{error}</Field.ErrorText>
          </Field.Root>
        </Box>
        <Flex gap="2">
          <Button type="button" variant="ghost" onClick={onClose}>
            Отмена
          </Button>
          <Button type="submit" disabled={!valid} loading={create.isPending}>
            Создать
          </Button>
        </Flex>
      </Form>
    </GlassCard>
  )
}

// ---------- общий бюджет ----------

function OverallCard({ status, yearMonth }: { status: BudgetStatusDto; yearMonth: string }) {
  const phone = useIsPhone()
  const update = useUpdateBudget()
  const progress = budgetProgress(status)
  const [edit, setEdit] = useState<EditState | null>(null)

  async function save() {
    const value = parseAmount(edit?.value ?? '')
    if (value === null || value <= 0) return
    try {
      await update.mutateAsync({
        id: status.budgetId,
        body: { limitAmount: value, period: `${yearMonth}-01` },
      })
      notify.success('Лимит обновлён', `Общий бюджет: ${formatMoney(value)}`)
      setEdit(null)
    } catch (e) {
      notify.error('Не удалось обновить лимит', errorMessage(e))
    }
  }

  const over = progress.tone === 'over'
  const stats: readonly [string, number][] = [
    ['Лимит', status.assigned],
    ['Потрачено', status.spent],
    [over ? 'Перерасход' : 'Осталось', Math.abs(progress.available)],
  ]

  return (
    <Iridescent borderRadius="l3" boxShadow="lg">
      <Stack gap="5" p={phone ? '20px' : '28px'}>
        <Flex justify="space-between" align="center" gap="3">
          <Text fontWeight="600">Общий бюджет на месяц</Text>
          {edit === null && (
            <Box
              as="button"
              aria-label="Изменить лимит"
              display="flex"
              alignItems="center"
              gap="1.5"
              h="32px"
              px="2.5"
              border="0"
              borderRadius="l2"
              fontSize="xs"
              fontWeight="500"
              color="inherit"
              cursor="pointer"
              style={{ background: 'var(--ct-chip-bg)' }}
              onClick={() => setEdit({ budgetId: status.budgetId, value: centsToInput(toCents(status.assigned)) })}
            >
              <Pencil size={14} aria-hidden />
              Изменить
            </Box>
          )}
        </Flex>

        {edit !== null && (
          <Form
            display="flex"
            gap="2"
            alignItems="center"
            maxW="360px"
            onSubmit={(event) => {
              event.preventDefault()
              void save()
            }}
          >
            <Input
              inputMode="decimal"
              autoFocus
              autoComplete="off"
              aria-label="Новый лимит"
              value={edit.value}
              borderColor="var(--ct-bar-track)"
              onChange={(event) => setEdit({ ...edit, value: sanitizeAmountInput(event.currentTarget.value) })}
              onKeyDown={(event) => {
                if (event.key === 'Escape') setEdit(null)
              }}
            />
            <Button type="submit" loading={update.isPending}>
              Сохранить
            </Button>
            <IconButton aria-label="Отмена" variant="ghost" color="inherit" onClick={() => setEdit(null)}>
              <X />
            </IconButton>
          </Form>
        )}

        <Grid templateColumns="repeat(3, minmax(0, 1fr))" gap="4">
          {stats.map(([label, value]) => (
            <Flex key={label} direction="column" gap="0.5">
              <Text fontSize="xs" color="var(--ct-on-flow-muted)">
                {label}
              </Text>
              <Text
                className="ct-num"
                fontFamily="heading"
                fontSize={phone ? 'lg' : '3xl'}
                fontWeight="600"
                letterSpacing="-0.02em"
              >
                {formatMoney(value)}
              </Text>
            </Flex>
          ))}
        </Grid>

        <ProgressBar
          percent={progress.fill}
          height={10}
          label="Расход общего бюджета"
          fill="var(--ct-bar-fill)"
          track="var(--ct-bar-track)"
        />
      </Stack>
    </Iridescent>
  )
}

// ---------- бюджет категории ----------

function CategoryBudgetCard({ status, yearMonth }: { status: BudgetStatusDto; yearMonth: string }) {
  const category = status.category
  const update = useUpdateBudget()
  const remove = useDeleteBudget()
  const [edit, setEdit] = useState<EditState | null>(null)
  const progress = budgetProgress(status)
  if (!category) return null

  async function save() {
    const value = parseAmount(edit?.value ?? '')
    if (value === null || value <= 0 || !category) return
    try {
      await update.mutateAsync({
        id: status.budgetId,
        body: { limitAmount: value, period: `${yearMonth}-01` },
      })
      notify.success('Лимит обновлён', `${category.categoryName}: ${formatMoney(value)}`)
      setEdit(null)
    } catch (e) {
      notify.error('Не удалось обновить лимит', errorMessage(e))
    }
  }

  async function onDelete() {
    if (!category) return
    try {
      await remove.mutateAsync(status.budgetId)
      notify.info('Бюджет удалён', category.categoryName)
    } catch (e) {
      notify.error('Не удалось удалить бюджет', errorMessage(e))
    }
  }

  const palette = progress.tone === 'over' ? 'red' : progress.tone === 'warn' ? 'orange' : 'green'

  return (
    <GlassCard>
      <Stack gap="3.5">
        <Flex align="center" gap="2.5">
          <CategoryIcon categoryId={category.id} name={category.categoryName} size={36} iconSize={18} />
          <Text flex="1" minW="0" fontWeight="600" lineClamp="1">
            {category.categoryName}
          </Text>
          <IconButton
            aria-label={`Изменить лимит «${category.categoryName}»`}
            variant="ghost"
            size="xs"
            onClick={() => setEdit({ budgetId: status.budgetId, value: centsToInput(toCents(status.assigned)) })}
          >
            <Pencil />
          </IconButton>
          <IconButton
            aria-label={`Удалить бюджет «${category.categoryName}»`}
            variant="ghost"
            size="xs"
            loading={remove.isPending}
            onClick={() => void onDelete()}
          >
            <Trash2 />
          </IconButton>
        </Flex>

        {edit !== null && (
          <Form
            display="flex"
            gap="1.5"
            onSubmit={(event) => {
              event.preventDefault()
              void save()
            }}
          >
            <Input
              size="sm"
              inputMode="decimal"
              autoFocus
              autoComplete="off"
              aria-label="Новый лимит"
              value={edit.value}
              onChange={(event) => setEdit({ ...edit, value: sanitizeAmountInput(event.currentTarget.value) })}
              onKeyDown={(event) => {
                if (event.key === 'Escape') setEdit(null)
              }}
            />
            <IconButton type="submit" size="sm" aria-label="Сохранить" loading={update.isPending}>
              <Check />
            </IconButton>
            <IconButton size="sm" variant="ghost" aria-label="Отмена" onClick={() => setEdit(null)}>
              <X />
            </IconButton>
          </Form>
        )}

        <Flex align="baseline" gap="1.5" wrap="wrap">
          <Text className="ct-num" fontFamily="heading" fontSize="2xl" fontWeight="600" letterSpacing="-0.02em">
            {formatMoney(status.spent)}
          </Text>
          <Text fontSize="sm" color="fg.muted">
            из {formatMoney(status.assigned)}
          </Text>
        </Flex>

        <ProgressBar
          percent={progress.fill}
          height={8}
          label={`Расход бюджета «${category.categoryName}»`}
          fill={progress.tone === 'over' ? OVERSPENT_GRADIENT : categoryBarGradient(category.id)}
        />

        <Flex justify="space-between" align="center">
          <Badge variant="subtle" colorPalette={palette}>
            {progress.tone === 'over'
              ? `Перерасход ${formatMoney(-progress.available)}`
              : `Осталось ${formatMoney(progress.available)}`}
          </Badge>
          <Text className="ct-num" fontSize="xs" color="fg.muted">
            {Math.round(progress.percent)}%
          </Text>
        </Flex>
      </Stack>
    </GlassCard>
  )
}

function BudgetsSkeleton() {
  return (
    <Stack gap="4" aria-busy="true" aria-label="Загрузка бюджетов">
      <Skeleton h="44" borderRadius="l3" />
      <Grid templateColumns="repeat(auto-fill, minmax(min(100%, 280px), 1fr))" gap="4">
        {[0, 1, 2].map((card) => (
          <Skeleton key={card} h="44" borderRadius="l3" />
        ))}
      </Grid>
    </Stack>
  )
}
