import { Alert, Box, Button, Drawer, Flex, Portal, Skeleton, Stack } from '@chakra-ui/react'
import { Link } from 'react-router-dom'
import { useCategories } from '../../api/hooks'
import type { TransactionDto } from '../../api/types'
import { QueryError } from '../../components/QueryError'
import { useIsPhone } from '../../layout/useLayoutMode'
import { TransactionForm } from './TransactionForm'

interface TransactionDrawerProps {
  open: boolean
  transaction: TransactionDto | null
  onClose: () => void
}

const EASING = 'cubic-bezier(0.21, 1.02, 0.73, 1)'

/**
 * Выезжающая панель траты. Десктоп и планшет — справа (до 480px), телефон — нижняя шторка от 6vh.
 * Фокус-ловушка, Esc, блокировка прокрутки и role="dialog" — от Chakra Drawer.
 */
export function TransactionDrawer({ open, transaction, onClose }: TransactionDrawerProps) {
  const phone = useIsPhone()

  return (
    <Drawer.Root
      open={open}
      onOpenChange={(event) => {
        if (!event.open) onClose()
      }}
      placement={phone ? 'bottom' : 'end'}
      lazyMount
      unmountOnExit
    >
      <Portal>
        <Drawer.Backdrop bg="rgba(0, 0, 0, 0.36)" />
        <Drawer.Positioner>
          <Drawer.Content
            // Заголовок формы появляется только после загрузки категорий, а у диалога имя нужно сразу
            aria-label={transaction ? 'Редактирование траты' : 'Новая трата'}
            bg="bg.panel"
            boxShadow="2xl"
            display="flex"
            flexDirection="column"
            animationDuration="380ms"
            animationTimingFunction={EASING}
            _closed={{ animationDuration: '250ms' }}
            {...(phone
              ? { h: '94dvh', maxH: '94dvh', borderTopRadius: '2xl' }
              : { w: 'min(480px, 100vw)', maxW: '100vw' })}
          >
            {phone && (
              <Flex justify="center" pt="2" aria-hidden>
                <Box w="40px" h="4px" borderRadius="full" bg="bg.emphasized" />
              </Flex>
            )}
            <DrawerBody transaction={transaction} onClose={onClose} />
          </Drawer.Content>
        </Drawer.Positioner>
      </Portal>
    </Drawer.Root>
  )
}

/** Ждёт категории: без них разбивку не собрать. */
function DrawerBody({
  transaction,
  onClose,
}: {
  transaction: TransactionDto | null
  onClose: () => void
}) {
  const categories = useCategories()

  if (categories.isPending) {
    return (
      <Stack p="6" gap="5" aria-busy="true">
        <Skeleton h="8" w="40%" />
        <Skeleton h="16" />
        <Skeleton h="10" />
        <Skeleton h="40" />
      </Stack>
    )
  }

  if (categories.isError) {
    return (
      <Box p="6">
        <QueryError
          error={categories.error}
          onRetry={() => void categories.refetch()}
          title="Не удалось загрузить категории"
        />
      </Box>
    )
  }

  if (categories.data.length === 0) {
    return (
      <Stack p="6" gap="4">
        <Alert.Root status="info" borderRadius="l3">
          <Alert.Indicator />
          <Alert.Content>
            <Alert.Title>Сначала создайте категорию</Alert.Title>
            <Alert.Description>
              Каждая трата относится к одной или нескольким категориям — например, «Еда» или «Транспорт».
            </Alert.Description>
          </Alert.Content>
        </Alert.Root>
        <Button asChild alignSelf="flex-start" onClick={onClose}>
          <Link to="/categories">Перейти к категориям</Link>
        </Button>
      </Stack>
    )
  }

  return (
    <TransactionForm
      key={transaction?.id ?? 'new'}
      transaction={transaction}
      categories={categories.data}
      onClose={onClose}
    />
  )
}
