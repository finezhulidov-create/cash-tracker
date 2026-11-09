import { Badge, chakra, Flex, Span, Text } from '@chakra-ui/react'
import { Receipt } from 'lucide-react'
import type { TransactionDto } from '../api/types'
import { categoryGradient } from '../lib/categoryStyle'
import { formatShortDate, formatTime } from '../lib/dates'
import { formatMoney } from '../lib/money'
import { FORMS, plural } from '../lib/plural'
import { useIsPhone } from '../layout/useLayoutMode'
import { CategoryIcon, SplitIcon } from './CategoryIcon'

interface TransactionRowProps {
  transaction: TransactionDto
  /** compact — «Последние операции» на обзоре (дата справа); list — группа дня на экране операций */
  variant: 'compact' | 'list'
  onOpen: (transaction: TransactionDto) => void
}

const RowButton = chakra('button')

/** Строка траты: иконка категории (или «сплит»), описание, чипы категорий, сумма. */
export function TransactionRow({ transaction, variant, onOpen }: TransactionRowProps) {
  const phone = useIsPhone()
  const parts = transaction.splits
  const multi = parts.length > 1
  const first = parts[0]?.category
  const second = parts[1]?.category
  const list = variant === 'list'

  return (
    <RowButton
      type="button"
      display="flex"
      alignItems="center"
      gap={list ? '3.5' : '3'}
      w="full"
      px={list ? '3' : '2'}
      py="2.5"
      borderRadius="l2"
      textAlign="left"
      color="inherit"
      cursor="pointer"
      transition="background 150ms"
      _hover={{ bg: 'bg.muted' }}
      onClick={() => onOpen(transaction)}
    >
      {multi && first && second ? (
        <SplitIcon categoryIds={[first.id, second.id]} />
      ) : first ? (
        <CategoryIcon categoryId={first.id} name={first.categoryName} />
      ) : (
        <Flex w="40px" h="40px" flexShrink={0} align="center" justify="center" borderRadius="l3" bg="bg.muted">
          <Receipt size={20} aria-hidden />
        </Flex>
      )}

      <Flex direction="column" gap="3px" flex="1" minW="0">
        <Text fontSize="sm" fontWeight="500" lineClamp="1">
          {transaction.description}
        </Text>
        <Flex wrap="wrap" columnGap="2.5" rowGap="0.5" fontSize="xs" color="fg.muted">
          {list && <span>{formatTime(transaction.dateTime)}</span>}
          {parts.map((part) => (
            <Span key={part.id} display="inline-flex" alignItems="center" gap="1.5">
              <Span
                aria-hidden
                w="7px"
                h="7px"
                borderRadius="full"
                style={{ background: categoryGradient(part.category.id) }}
              />
              {multi && !phone
                ? `${part.category.categoryName} · ${formatMoney(part.amount)}`
                : part.category.categoryName}
            </Span>
          ))}
        </Flex>
      </Flex>

      {list && multi && !phone && (
        <Badge variant="surface" colorPalette="purple" size="sm">
          {`${parts.length} ${plural(parts.length, FORMS.part)}`}
        </Badge>
      )}

      {list ? (
        <Text
          className="ct-num"
          fontSize="sm"
          fontWeight="600"
          flexShrink={0}
          textAlign="right"
          minW={phone ? '0' : '72px'}
        >
          {formatMoney(transaction.amount)}
        </Text>
      ) : (
        <Flex direction="column" align="flex-end" gap="3px" flexShrink={0}>
          <Text className="ct-num" fontSize="sm" fontWeight="600">
            {formatMoney(transaction.amount)}
          </Text>
          <Text fontSize="xs" color="fg.muted">
            {formatShortDate(transaction.dateTime)}
          </Text>
        </Flex>
      )}
    </RowButton>
  )
}
