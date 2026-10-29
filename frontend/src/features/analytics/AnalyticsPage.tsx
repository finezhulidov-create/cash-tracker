import { Box, EmptyState, Flex, Grid, SegmentGroup, Skeleton, Stack, Text, VStack } from '@chakra-ui/react'
import { ChartPie } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useMonthlySpending, useSpendingByCategory } from '../../api/hooks'
import { CategoryIcon } from '../../components/CategoryIcon'
import { Donut } from '../../components/Donut'
import { GlassCard } from '../../components/GlassCard'
import { PeriodStepper } from '../../components/PeriodStepper'
import { ProgressBar } from '../../components/ProgressBar'
import { QueryError } from '../../components/QueryError'
import { YearBars } from '../../components/YearBars'
import { useIsPhone } from '../../layout/useLayoutMode'
import { categoryBarGradient, categoryColors } from '../../lib/categoryStyle'
import { formatPeriod, monthName } from '../../lib/dates'
import type { DonutSegment } from '../../lib/donut'
import { formatCompact, formatMoney } from '../../lib/money'
import { buildYearBars, yearStats } from '../../lib/yearBars'
import type { Preset } from './range'
import { isPreset, MIN_YEAR, PRESETS, presetRange } from './range'

export function AnalyticsPage() {
  const today = useMemo(() => new Date(), [])
  return (
    <Stack gap="4">
      <ByCategoryCard today={today} />
      <ByMonthCard today={today} />
    </Stack>
  )
}

// ---------- траты по категориям ----------

function ByCategoryCard({ today }: { today: Date }) {
  const phone = useIsPhone()
  const [preset, setPreset] = useState<Preset>('month')
  const { from, to } = presetRange(preset, today)
  const query = useSpendingByCategory(from, to)

  const items = useMemo(
    () => (query.data ?? []).filter((item) => item.spent > 0).sort((a, b) => b.spent - a.spent),
    [query.data],
  )
  const total = items.reduce((sum, item) => sum + item.spent, 0)
  const max = items[0]?.spent ?? 1
  const segments: DonutSegment[] = items.map((item) => ({
    id: item.categoryId,
    value: item.spent,
    ...categoryColors(item.categoryId),
  }))

  return (
    <GlassCard>
      <Stack gap="6">
        <Flex justify="space-between" align="center" wrap="wrap" gap="3">
          <Box>
            <Text fontWeight="600" fontSize="lg">
              Траты по категориям
            </Text>
            <Text fontSize="sm" color="fg.muted">
              {formatPeriod(from, to)}
            </Text>
          </Box>
          <SegmentGroup.Root
            size="sm"
            value={preset}
            onValueChange={(event) => {
              if (isPreset(event.value)) setPreset(event.value)
            }}
          >
            <SegmentGroup.Indicator />
            <SegmentGroup.Items items={PRESETS.map(({ value, label }) => ({ value, label }))} />
          </SegmentGroup.Root>
        </Flex>

        {query.isError ? (
          <QueryError error={query.error} onRetry={() => void query.refetch()} />
        ) : query.isPending ? (
          <Flex wrap="wrap" gap="8" align="center">
            <Skeleton w={phone ? '200px' : '240px'} h={phone ? '200px' : '240px'} borderRadius="full" mx="auto" />
            <Stack flex="1 1 300px" gap="3.5">
              {[0, 1, 2, 3].map((row) => (
                <Skeleton key={row} h="8" />
              ))}
            </Stack>
          </Flex>
        ) : items.length === 0 ? (
          <EmptyState.Root>
            <EmptyState.Content>
              <EmptyState.Indicator>
                <ChartPie />
              </EmptyState.Indicator>
              <VStack textAlign="center">
                <EmptyState.Title>За этот период трат нет</EmptyState.Title>
                <EmptyState.Description>Выберите другой период или добавьте траты</EmptyState.Description>
              </VStack>
            </EmptyState.Content>
          </EmptyState.Root>
        ) : (
          <Flex wrap="wrap" gap="8" align="center">
            <Box mx="auto">
              <Donut
                segments={segments}
                size={phone ? 200 : 240}
                thickness={26}
                label="Траты по категориям за выбранный период"
              >
                <Text fontSize="xs" color="fg.muted">
                  Всего
                </Text>
                <Text className="ct-num" fontFamily="heading" fontSize="2xl" fontWeight="600" letterSpacing="-0.02em">
                  {formatMoney(total)}
                </Text>
              </Donut>
            </Box>
            <Stack flex="1 1 300px" minW="0" gap="3.5">
              {items.map((item) => (
                <Flex key={item.categoryId} align="center" gap="3">
                  <CategoryIcon categoryId={item.categoryId} name={item.categoryName} size={32} iconSize={16} />
                  <Stack flex="1" minW="0" gap="1.5">
                    <Flex justify="space-between" gap="2" fontSize="sm">
                      <span>
                        <Text as="span" fontWeight="500">
                          {item.categoryName}
                        </Text>{' '}
                        <Text as="span" color="fg.muted">
                          {Math.round((item.spent / total) * 100)}%
                        </Text>
                      </span>
                      <Text as="span" className="ct-num" fontWeight="600">
                        {formatMoney(item.spent)}
                      </Text>
                    </Flex>
                    <ProgressBar
                      percent={(item.spent / max) * 100}
                      height={6}
                      label={`Доля «${item.categoryName}»`}
                      fill={categoryBarGradient(item.categoryId)}
                    />
                  </Stack>
                </Flex>
              ))}
            </Stack>
          </Flex>
        )}
      </Stack>
    </GlassCard>
  )
}

// ---------- траты по месяцам ----------

function ByMonthCard({ today }: { today: Date }) {
  const phone = useIsPhone()
  const currentYear = today.getFullYear()
  const [year, setYear] = useState(currentYear)
  const query = useMonthlySpending(year)

  const bars = useMemo(() => (query.data ? buildYearBars(query.data, year, today) : null), [query.data, year, today])
  const stats = bars ? yearStats(bars) : null

  const metrics: readonly [string, string][] = stats
    ? [
        ['За год', formatMoney(stats.total)],
        ['В среднем за месяц', formatMoney(stats.average)],
        [
          'Самый затратный',
          stats.peakMonth === null
            ? '—'
            : `${capitalize(monthName(stats.peakMonth))} · ${formatCompact(stats.peakValue)} ₽`,
        ],
      ]
    : []

  return (
    <GlassCard>
      <Stack gap="6">
        <Flex justify="space-between" align="center" wrap="wrap" gap="3">
          <Text fontWeight="600" fontSize="lg">
            Траты по месяцам
          </Text>
          <PeriodStepper
            label={String(year)}
            prevLabel="Предыдущий год"
            nextLabel="Следующий год"
            disablePrev={year <= MIN_YEAR}
            disableNext={year >= currentYear}
            onPrev={() => setYear((value) => value - 1)}
            onNext={() => setYear((value) => value + 1)}
          />
        </Flex>

        {query.isError ? (
          <QueryError error={query.error} onRetry={() => void query.refetch()} />
        ) : query.isPending || !bars ? (
          <Stack gap="5">
            <Skeleton h="12" />
            <Skeleton h={phone ? '200px' : '260px'} />
          </Stack>
        ) : (
          <>
            <Grid templateColumns="repeat(auto-fit, minmax(min(100%, 150px), 1fr))" gap="4">
              {metrics.map(([label, value]) => (
                <Flex key={label} direction="column" gap="0.5">
                  <Text fontSize="xs" color="fg.muted">
                    {label}
                  </Text>
                  <Text className="ct-num" fontFamily="heading" fontSize="xl" fontWeight="600">
                    {value}
                  </Text>
                </Flex>
              ))}
            </Grid>
            <YearBars
              bars={bars}
              year={year}
              height={phone ? 200 : 260}
              gap={phone ? 4 : 10}
              showValues={!phone}
              labelSize="xs"
            />
          </>
        )}
      </Stack>
    </GlassCard>
  )
}

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1)
}
