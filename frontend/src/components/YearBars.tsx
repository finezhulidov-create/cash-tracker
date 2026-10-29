import { Box, Flex, Grid, Text } from '@chakra-ui/react'
import { monthName, monthShort } from '../lib/dates'
import { formatCompact, formatMoney } from '../lib/money'
import type { YearBar } from '../lib/yearBars'
import { CURRENT_BAR_GRADIENT, pastBarGradient } from '../lib/yearBars'

interface YearBarsProps {
  bars: readonly YearBar[]
  year: number
  /** Высота области столбцов, px */
  height: number
  /** Промежуток между столбцами, px */
  gap: number
  /** Подписывать столбцы значениями сверху («15,4к») */
  showValues?: boolean
  labelSize?: '2xs' | 'xs'
}

/** 12 столбцов: текущий месяц — перелив с кольцом, прошлые — приглушённый градиент, будущие — пустые. */
export function YearBars({ bars, year, height, gap, showValues = false, labelSize = '2xs' }: YearBarsProps) {
  return (
    <Grid
      templateColumns="repeat(12, minmax(0, 1fr))"
      alignItems="end"
      style={{ height: `${height}px`, gap: `${gap}px` }}
    >
      {bars.map((bar) => {
        const title = `${capitalize(monthName(bar.month))} ${year}: ${formatMoney(bar.value)}`
        return (
          <Flex key={bar.month} title={title} direction="column" justify="flex-end" align="stretch" gap="1.5" h="100%">
            {showValues && (
              <Text textAlign="center" fontSize="2xs" color="fg.muted" whiteSpace="nowrap" className="ct-num">
                {bar.value > 0 ? formatCompact(bar.value) : ''}
              </Text>
            )}
            <Box
              minH="4px"
              borderTopRadius="l2"
              borderBottomRadius="2px"
              transition="height 400ms"
              style={{
                height: `${bar.heightPct}%`,
                background: bar.current
                  ? CURRENT_BAR_GRADIENT
                  : bar.future
                    ? 'var(--chakra-colors-bg-muted)'
                    : pastBarGradient(bar.month),
                opacity: bar.current || bar.future ? 1 : 0.55,
                boxShadow: bar.current
                  ? '0 0 0 2px var(--chakra-colors-bg-panel), 0 0 0 3px #c084fc'
                  : 'none',
              }}
            />
            <Text
              textAlign="center"
              fontSize={labelSize}
              color={bar.current ? 'fg' : 'fg.muted'}
              fontWeight={bar.current ? '600' : '400'}
            >
              {monthShort(bar.month)}
            </Text>
          </Flex>
        )
      })}
    </Grid>
  )
}

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1)
}
