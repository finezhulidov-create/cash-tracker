import { Box, Button, Flex, Skeleton, Stack, Text } from '@chakra-ui/react'
import { Link, useNavigate } from 'react-router-dom'
import { Wallet } from 'lucide-react'
import { useMemo } from 'react'
import {
  useMe,
  useMonthSummary,
  useMonthlySpending,
  useRecentTransactions,
  useSpendingByCategory,
} from '../../api/hooks'
import type {
  BudgetStatusDto,
  CategorySpendingDto,
  MonthlySpendingDto,
  MonthSummaryDto,
  TransactionDto,
} from '../../api/types'
import { CategoryIcon } from '../../components/CategoryIcon'
import { Donut } from '../../components/Donut'
import { GlassCard } from '../../components/GlassCard'
import { Iridescent } from '../../components/Iridescent'
import { ProgressBar } from '../../components/ProgressBar'
import { QueryError } from '../../components/QueryError'
import { TransactionRow } from '../../components/TransactionRow'
import { YearBars } from '../../components/YearBars'
import { useIsPhone } from '../../layout/useLayoutMode'
import { budgetProgress, heroStats } from '../../lib/budget'
import {
  categoryBarGradient,
  categoryColors,
  categoryGradient,
  OVERSPENT_GRADIENT,
} from '../../lib/categoryStyle'
import { monthDateRange, monthName, monthPrepositional, yearMonthOf } from '../../lib/dates'
import type { DonutSegment } from '../../lib/donut'
import { formatMoney } from '../../lib/money'
import { FORMS, plural } from '../../lib/plural'
import { buildYearBars, yearStats } from '../../lib/yearBars'
import { useTransactionDrawer } from '../transactions/drawerContext'

export function DashboardPage() {
  const today = useMemo(() => new Date(), [])
  const yearMonth = yearMonthOf(today)
  const year = today.getFullYear()

  const summary = useMonthSummary(yearMonth)
  const { from, to } = monthDateRange(yearMonth)
  const spending = useSpendingByCategory(from, to)
  const recent = useRecentTransactions(6)
  const monthly = useMonthlySpending(year)

  return (
    <Stack gap="4">
      <Flex wrap="wrap" gap="4">
        <Hero summary={summary.data} error={summary.error} onRetry={summary.refetch} today={today} />
        <Box flex="1 1 300px" minW="0" display="flex" flexDirection="column">
          <CategoriesDonutCard spending={spending.data} loading={spending.isPending} yearMonth={yearMonth} />
        </Box>
      </Flex>

      <Flex wrap="wrap" gap="4">
        <Box flex="1 1 340px" minW="0" display="flex" flexDirection="column">
          <BudgetsCard summary={summary.data} loading={summary.isPending} error={summary.error} />
        </Box>
        <Box flex="1.4 1 420px" minW="0" display="flex" flexDirection="column">
          <RecentCard transactions={recent.data} loading={recent.isPending} error={recent.error} />
        </Box>
      </Flex>

      <YearCard
        year={year}
        today={today}
        data={monthly.data}
        loading={monthly.isPending}
        error={monthly.error}
      />
    </Stack>
  )
}

// ---------- hero ----------

interface HeroProps {
  summary: MonthSummaryDto | undefined
  error: unknown
  onRetry: () => unknown
  today: Date
}

function Hero({ summary, error, onRetry, today }: HeroProps) {
  const phone = useIsPhone()
  const me = useMe()
  const navigate = useNavigate()

  if (error) {
    return (
      <Box flex="2 1 520px" minW="0">
        <QueryError error={error} onRetry={() => void onRetry()} />
      </Box>
    )
  }

  const stats = summary ? heroStats(summary.totalSpent, summary.overall, today) : null
  const monthWord = monthPrepositional(yearMonthOf(today))

  return (
    <Iridescent
      flex="2 1 520px"
      minW="0"
      minH="248px"
      borderRadius="l3"
      boxShadow="lg"
      display="flex"
    >
      <Flex
        flex="1"
        direction="column"
        justify="space-between"
        gap="7"
        p={phone ? '20px' : '28px'}
        position="relative"
      >
        <Flex justify="space-between" align="flex-start" gap="4" wrap="wrap">
          <Flex direction="column" gap="1">
            <Text fontSize="sm" fontWeight="500" color="var(--ct-on-flow-muted)">
              Потрачено в {monthWord}
            </Text>
            {stats ? (
              <Text
                className="ct-num"
                fontFamily="heading"
                fontSize={phone ? '40px' : '56px'}
                lineHeight="1.05"
                fontWeight="600"
                letterSpacing="-0.025em"
              >
                {formatMoney(stats.spent)}
              </Text>
            ) : (
              <Skeleton h={phone ? '10' : '14'} w="240px" opacity="0.5" />
            )}
            {stats && (
              <Text fontSize="sm" color="var(--ct-on-flow-muted)">
                {stats.limit !== null
                  ? `из ${formatMoney(stats.limit)} общего бюджета`
                  : 'Общий бюджет на месяц не задан'}
              </Text>
            )}
          </Flex>
          <Box
            as="button"
            onClick={() => navigate('/profile')}
            display="flex"
            flexDirection="column"
            alignItems="flex-start"
            gap="0.5"
            px="3.5"
            py="2.5"
            border="0"
            borderRadius="l3"
            color="inherit"
            cursor="pointer"
            backdropFilter="blur(8px)"
            style={{ background: 'var(--ct-chip-bg)' }}
            aria-label="Баланс — открыть профиль"
          >
            <Text fontSize="xs" color="var(--ct-on-flow-muted)">
              Баланс
            </Text>
            <Text className="ct-num" fontSize="md" fontWeight="600">
              {me.data ? formatMoney(me.data.balance) : '…'}
            </Text>
          </Box>
        </Flex>

        {stats && stats.limit !== null ? (
          <Flex direction="column" gap="2.5">
            <ProgressBar
              percent={stats.fill}
              height={10}
              label="Расход общего бюджета"
              fill="var(--ct-bar-fill)"
              track="var(--ct-bar-track)"
            />
            <Flex justify="space-between" wrap="wrap" columnGap="4" rowGap="1.5" fontSize="sm">
              <span>
                <b style={{ fontWeight: 600 }}>
                  {stats.overspent > 0
                    ? `Перерасход ${formatMoney(stats.overspent)}`
                    : `Осталось ${formatMoney(stats.remaining)}`}
                </b>{' '}
                <Text as="span" color="var(--ct-on-flow-muted)">
                  · {Math.round(stats.percent)}% бюджета
                </Text>
              </span>
              <Text as="span" color="var(--ct-on-flow-muted)">
                ≈ {formatMoney(stats.perDay)} в день · ещё {stats.daysLeft} {plural(stats.daysLeft, FORMS.day)}
              </Text>
            </Flex>
          </Flex>
        ) : stats ? (
          <Link to="/budgets">
            <Flex
              align="center"
              gap="2.5"
              px="3.5"
              py="3"
              borderRadius="l3"
              fontSize="sm"
              fontWeight="500"
              style={{ background: 'var(--ct-chip-bg)' }}
            >
              <Wallet size={18} aria-hidden />
              <span>Задайте общий бюджет на месяц, чтобы видеть остаток и расход в день</span>
            </Flex>
          </Link>
        ) : (
          <Skeleton h="10" opacity="0.5" />
        )}
      </Flex>
    </Iridescent>
  )
}

// ---------- донат по категориям ----------

function CategoriesDonutCard({
  spending,
  loading,
  yearMonth,
}: {
  spending: CategorySpendingDto[] | undefined
  loading: boolean
  yearMonth: string
}) {
  const items = useMemo(
    () => (spending ?? []).filter((item) => item.spent > 0).sort((a, b) => b.spent - a.spent),
    [spending],
  )
  const total = items.reduce((sum, item) => sum + item.spent, 0)
  const segments: DonutSegment[] = items.map((item) => ({
    id: item.categoryId,
    value: item.spent,
    ...categoryColors(item.categoryId),
  }))
  const monthTitle = capitalizeFirst(monthName(Number(yearMonth.split('-')[1])))

  return (
    <GlassCard title="По категориям" action={<Text fontSize="xs" color="fg.muted">{monthTitle}</Text>} flex="1 1 auto">
      {loading ? (
        <Flex gap="5" align="center" wrap="wrap">
          <Skeleton w="140px" h="140px" borderRadius="full" />
          <Stack flex="1 1 140px" gap="2.5">
            {[0, 1, 2, 3].map((row) => (
              <Skeleton key={row} h="4" />
            ))}
          </Stack>
        </Flex>
      ) : items.length === 0 ? (
        <Text fontSize="sm" color="fg.muted">
          В этом месяце трат пока нет
        </Text>
      ) : (
        <Flex align="center" gap="5" wrap="wrap">
          <Donut segments={segments} size={140} thickness={18} label="Траты месяца по категориям">
            <Text fontSize="xs" color="fg.muted">
              {items.length} {plural(items.length, FORMS.category)}
            </Text>
            <Text className="ct-num" fontSize="sm" fontWeight="600">
              {formatMoney(total)}
            </Text>
          </Donut>
          <Stack flex="1 1 140px" minW="0" gap="2">
            {items.map((item) => (
              <Flex key={item.categoryId} align="center" gap="2" fontSize="sm">
                <Box
                  aria-hidden
                  w="10px"
                  h="10px"
                  flexShrink={0}
                  borderRadius="3px"
                  style={{ background: categoryGradient(item.categoryId) }}
                />
                <Text flex="1" minW="0" lineClamp="1">
                  {item.categoryName}
                </Text>
                <Text className="ct-num" color="fg.muted">
                  {Math.round((item.spent / total) * 100)}%
                </Text>
              </Flex>
            ))}
          </Stack>
        </Flex>
      )}
    </GlassCard>
  )
}

// ---------- бюджеты ----------

function BudgetsCard({
  summary,
  loading,
  error,
}: {
  summary: MonthSummaryDto | undefined
  loading: boolean
  error: unknown
}) {
  const rows = useMemo(
    () =>
      [...(summary?.categories ?? [])].sort((a, b) =>
        (a.category?.categoryName ?? '').localeCompare(b.category?.categoryName ?? '', 'ru'),
      ),
    [summary],
  )

  return (
    <GlassCard
      title="Бюджеты"
      flex="1 1 auto"
      action={
        <Button asChild variant="ghost" size="xs">
          <Link to="/budgets">Все бюджеты</Link>
        </Button>
      }
    >
      {error ? (
        <QueryError error={error} />
      ) : loading ? (
        <Stack gap="4.5">
          {[0, 1, 2, 3].map((row) => (
            <Stack key={row} gap="2">
              <Skeleton h="5" />
              <Skeleton h="1.5" />
            </Stack>
          ))}
        </Stack>
      ) : rows.length === 0 ? (
        <Text fontSize="sm" color="fg.muted">
          Бюджетов по категориям на этот месяц пока нет. Задайте лимиты на экране «Бюджеты».
        </Text>
      ) : (
        <Stack gap="4.5">
          {rows.map((row) => (
            <BudgetLine key={row.budgetId} status={row} />
          ))}
        </Stack>
      )}
    </GlassCard>
  )
}

function BudgetLine({ status }: { status: BudgetStatusDto }) {
  const category = status.category
  const progress = budgetProgress(status)
  if (!category) return null

  const statusColor =
    progress.tone === 'over' ? 'fg.error' : progress.tone === 'warn' ? 'fg.warning' : 'fg.muted'

  return (
    <Stack gap="2">
      <Flex align="center" gap="2.5">
        <CategoryIcon categoryId={category.id} name={category.categoryName} size={28} iconSize={16} />
        <Text flex="1" minW="0" fontSize="sm" fontWeight="500" lineClamp="1">
          {category.categoryName}
        </Text>
        <Text className="ct-num" fontSize="sm">
          {formatMoney(status.spent)}{' '}
          <Text as="span" color="fg.muted">
            / {formatMoney(status.assigned)}
          </Text>
        </Text>
      </Flex>
      <ProgressBar
        percent={progress.fill}
        height={6}
        label={`Расход бюджета «${category.categoryName}»`}
        fill={progress.tone === 'over' ? OVERSPENT_GRADIENT : categoryBarGradient(category.id)}
      />
      <Text
        fontSize="xs"
        color={statusColor}
        fontWeight={progress.tone === 'ok' ? '400' : '600'}
      >
        {progress.tone === 'over'
          ? `Перерасход ${formatMoney(-progress.available)}`
          : `Осталось ${formatMoney(progress.available)}`}
      </Text>
    </Stack>
  )
}

// ---------- последние операции ----------

function RecentCard({
  transactions,
  loading,
  error,
}: {
  transactions: TransactionDto[] | undefined
  loading: boolean
  error: unknown
}) {
  const { openEdit } = useTransactionDrawer()

  return (
    <GlassCard
      title="Последние операции"
      flex="1 1 auto"
      action={
        <Button asChild variant="ghost" size="xs">
          <Link to="/transactions">Все операции</Link>
        </Button>
      }
    >
      {error ? (
        <QueryError error={error} />
      ) : loading ? (
        <Stack gap="1">
          {[0, 1, 2, 3, 4, 5].map((row) => (
            <Flex key={row} align="center" gap="3" py="2.5">
              <Skeleton w="40px" h="40px" borderRadius="l3" />
              <Stack flex="1" gap="2">
                <Skeleton h="4" w="50%" />
                <Skeleton h="3" w="28%" />
              </Stack>
              <Skeleton h="4" w="64px" />
            </Flex>
          ))}
        </Stack>
      ) : !transactions || transactions.length === 0 ? (
        <Text fontSize="sm" color="fg.muted">
          Трат пока нет — добавьте первую кнопкой «Добавить трату».
        </Text>
      ) : (
        <Flex direction="column" mx="-2">
          {transactions.map((transaction) => (
            <TransactionRow
              key={transaction.id}
              transaction={transaction}
              variant="compact"
              onOpen={openEdit}
            />
          ))}
        </Flex>
      )}
    </GlassCard>
  )
}

// ---------- динамика за год ----------

function YearCard({
  year,
  today,
  data,
  loading,
  error,
}: {
  year: number
  today: Date
  data: MonthlySpendingDto[] | undefined
  loading: boolean
  error: unknown
}) {
  const phone = useIsPhone()
  const bars = useMemo(() => (data ? buildYearBars(data, year, today) : null), [data, year, today])
  const stats = bars ? yearStats(bars) : null

  return (
    <GlassCard
      title={`Динамика за ${year}`}
      action={
        stats ? (
          <Text fontSize="sm" color="fg.muted">
            В среднем {formatMoney(stats.average)} в месяц
          </Text>
        ) : undefined
      }
    >
      {error ? (
        <QueryError error={error} />
      ) : loading || !bars ? (
        <Skeleton h="150px" />
      ) : (
        <YearBars bars={bars} year={year} height={150} gap={phone ? 4 : 10} />
      )}
    </GlassCard>
  )
}

function capitalizeFirst(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1)
}
