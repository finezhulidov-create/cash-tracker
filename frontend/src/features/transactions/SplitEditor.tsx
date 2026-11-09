import { Box, Button, Flex, IconButton, Input, NativeSelect, Text } from '@chakra-ui/react'
import { Divide, Split, X } from 'lucide-react'
import type { Dispatch } from 'react'
import type { CategoryDto } from '../../api/types'
import { categoryColors, categoryGradient } from '../../lib/categoryStyle'
import type { DraftSplit } from '../../lib/splits'
import { computeSplitStatus, nextUnusedCategory, totalCents } from '../../lib/splits'
import type { FormAction } from './formState'
import { newSplitKey } from './formState'
import { describeSplitStatus } from './splitStatusView'

interface SplitEditorProps {
  categories: readonly CategoryDto[]
  splits: readonly DraftSplit[]
  amountText: string
  hasDescription: boolean
  dispatch: Dispatch<FormAction>
}

const HATCH =
  'repeating-linear-gradient(135deg, var(--chakra-colors-bg-emphasized) 0 4px, var(--chakra-colors-bg-muted) 4px 8px)'

/** Блок «Разбивка по категориям»: сегментированный бар, строки частей и статус распределения. */
export function SplitEditor({ categories, splits, amountText, hasDescription, dispatch }: SplitEditorProps) {
  const multi = splits.length > 1
  const total = totalCents(amountText)
  const status = computeSplitStatus(amountText, splits)
  const categoryById = new Map(categories.map((category) => [category.id, category]))
  const view = describeSplitStatus(
    status,
    (id) => categoryById.get(id)?.categoryName ?? '—',
    hasDescription,
  )

  const partCents = splits.map((split) => totalCents(split.amount))
  const partsSum = partCents.reduce((sum, cents) => sum + cents, 0)
  const remaining = total - partsSum
  const barDenominator = Math.max(total, partsSum) || 1

  const barSegments = multi
    ? [
        ...splits
          .map((split, index) => ({
            key: split.key,
            width: ((partCents[index] ?? 0) / barDenominator) * 100,
            background:
              split.categoryId === null
                ? 'var(--chakra-colors-bg-muted)'
                : `linear-gradient(90deg, ${categoryColors(split.categoryId).c1}, ${categoryColors(split.categoryId).c2})`,
          }))
          .filter((segment) => segment.width > 0),
        ...(remaining > 0
          ? [{ key: 'rest', width: (remaining / barDenominator) * 100, background: HATCH }]
          : []),
      ]
    : [
        {
          key: 'whole',
          width: 100,
          background:
            splits[0]?.categoryId == null
              ? 'var(--chakra-colors-bg-muted)'
              : categoryGradient(splits[0].categoryId),
        },
      ]

  const noMoreCategories = nextUnusedCategory(
    categories.map((category) => category.id),
    splits,
  ) === null

  return (
    <Flex
      direction="column"
      gap="3"
      p="4"
      borderRadius="l3"
      bg="bg.subtle"
      borderWidth="1px"
      borderColor="border.muted"
    >
      <Flex justify="space-between" align="center" gap="2">
        <Text fontSize="sm" fontWeight="600">
          Разбивка по категориям
        </Text>
        {multi && (
          <Button variant="ghost" size="xs" onClick={() => dispatch({ type: 'evenSplit' })}>
            <Divide aria-hidden />
            Поровну
          </Button>
        )}
      </Flex>

      <Flex gap="3px" h="8px" aria-hidden>
        {barSegments.map((segment) => (
          <Box
            key={segment.key}
            h="100%"
            borderRadius="3px"
            transition="width 300ms"
            style={{ width: `${segment.width}%`, background: segment.background }}
          />
        ))}
      </Flex>

      {splits.map((split, index) => {
        const usedByOthers = new Set(
          splits.filter((other) => other.key !== split.key).map((other) => other.categoryId),
        )
        return (
          <Flex key={split.key} align="center" gap="2">
            <Box
              aria-hidden
              w="10px"
              h="10px"
              flexShrink={0}
              borderRadius="3px"
              style={{
                background:
                  split.categoryId === null
                    ? 'var(--chakra-colors-bg-muted)'
                    : categoryGradient(split.categoryId),
              }}
            />
            <Box flex="1" minW="0">
              <NativeSelect.Root>
                <NativeSelect.Field
                  aria-label={multi ? `Категория части ${index + 1}` : 'Категория'}
                  value={split.categoryId === null ? '' : String(split.categoryId)}
                  onChange={(event) =>
                    dispatch({
                      type: 'category',
                      key: split.key,
                      categoryId: Number(event.currentTarget.value),
                    })
                  }
                >
                  {split.categoryId === null && <option value="">Выберите категорию</option>}
                  {categories.map((category) => (
                    <option
                      key={category.id}
                      value={category.id}
                      disabled={usedByOthers.has(category.id)}
                    >
                      {category.categoryName}
                    </option>
                  ))}
                </NativeSelect.Field>
                <NativeSelect.Indicator />
              </NativeSelect.Root>
            </Box>
            {multi && (
              <>
                <Input
                  w="112px"
                  flexShrink={0}
                  textAlign="right"
                  placeholder="0"
                  inputMode="decimal"
                  autoComplete="off"
                  aria-label={`Сумма части ${index + 1}`}
                  className="ct-num"
                  value={split.amount}
                  onChange={(event) =>
                    dispatch({ type: 'splitAmount', key: split.key, value: event.currentTarget.value })
                  }
                />
                <IconButton
                  aria-label={`Убрать часть ${index + 1}`}
                  variant="ghost"
                  onClick={() => dispatch({ type: 'removeSplit', key: split.key })}
                >
                  <X />
                </IconButton>
              </>
            )}
          </Flex>
        )
      })}

      <Button
        variant="outline"
        size="sm"
        borderStyle="dashed"
        disabled={noMoreCategories}
        onClick={() =>
          dispatch({
            type: 'addSplit',
            key: newSplitKey(),
            categoryIds: categories.map((category) => category.id),
          })
        }
      >
        <Split aria-hidden />
        Разделить на ещё одну категорию
      </Button>

      <Flex align="center" gap="2" wrap="wrap" fontSize="sm" fontWeight="500" color={view.color} role="status">
        <view.Icon size={16} aria-hidden style={{ flexShrink: 0 }} />
        <Text as="span" flex="1" minW="0">
          {view.text}
        </Text>
        {view.canFill && (
          <Button
            variant="subtle"
            colorPalette="orange"
            size="xs"
            onClick={() => dispatch({ type: 'fillRemainder' })}
          >
            Добавить остаток
          </Button>
        )}
      </Flex>
    </Flex>
  )
}
