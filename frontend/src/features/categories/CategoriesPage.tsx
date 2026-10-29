import {
  Alert,
  Box,
  Button,
  EmptyState,
  Flex,
  IconButton,
  Input,
  Skeleton,
  Stack,
  Text,
  VStack,
} from '@chakra-ui/react'
import { Check, Pencil, Tags, Trash2, X } from 'lucide-react'
import { useMemo, useState } from 'react'
import { isApiError, errorMessage } from '../../api/errors'
import {
  useCategories,
  useCategorySplitCount,
  useCreateCategory,
  useDeleteCategory,
  useRenameCategory,
  useSpendingByCategory,
} from '../../api/hooks'
import type { CategoryDto } from '../../api/types'
import { Form } from '../../components/Form'
import { CategoryIcon } from '../../components/CategoryIcon'
import { GlassCard } from '../../components/GlassCard'
import { QueryError } from '../../components/QueryError'
import { monthDateRange, monthPrepositional, yearMonthOf } from '../../lib/dates'
import { formatMoney } from '../../lib/money'
import { notify } from '../../lib/notify'
import { FORMS, plural } from '../../lib/plural'
import { findDuplicate, MAX_CATEGORY_NAME } from './categoryName'

export function CategoriesPage() {
  const categories = useCategories()
  const create = useCreateCategory()
  const [name, setName] = useState('')
  const [error, setError] = useState<string | null>(null)

  const currentMonth = useMemo(() => yearMonthOf(new Date()), [])
  const { from, to } = monthDateRange(currentMonth)
  const spending = useSpendingByCategory(from, to)
  const spentById = useMemo(
    () => new Map(spending.data?.map((item) => [item.categoryId, item.spent])),
    [spending.data],
  )

  async function onAdd(event: React.FormEvent) {
    event.preventDefault()
    const trimmed = name.trim()
    if (!trimmed) return
    if (findDuplicate(categories.data ?? [], trimmed)) {
      setError(`Категория «${trimmed}» уже есть`)
      return
    }
    try {
      await create.mutateAsync(trimmed)
      notify.success('Категория добавлена', trimmed)
      setName('')
      setError(null)
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <Stack gap="4" maxW="760px" w="full">
      <GlassCard>
        <Form display="flex" flexDirection="column" gap="2" onSubmit={onAdd} noValidate>
          <Text fontSize="sm" fontWeight="600">
            Новая категория
          </Text>
          <Flex gap="2">
            <Input
              placeholder="Например, Подарки"
              aria-label="Название новой категории"
              maxLength={MAX_CATEGORY_NAME}
              autoComplete="off"
              value={name}
              aria-invalid={error !== null}
              onChange={(event) => {
                setName(event.currentTarget.value)
                setError(null)
              }}
              borderColor={error ? 'border.error' : undefined}
            />
            <Button type="submit" disabled={name.trim() === ''} loading={create.isPending}>
              Добавить
            </Button>
          </Flex>
          {error && (
            <Text role="alert" fontSize="xs" fontWeight="500" color="fg.error">
              {error}
            </Text>
          )}
        </Form>
      </GlassCard>

      {categories.isError ? (
        <QueryError error={categories.error} onRetry={() => void categories.refetch()} />
      ) : categories.isPending ? (
        <ListSkeleton />
      ) : categories.data.length === 0 ? (
        <GlassCard>
          <EmptyState.Root>
            <EmptyState.Content>
              <EmptyState.Indicator>
                <Tags />
              </EmptyState.Indicator>
              <VStack textAlign="center">
                <EmptyState.Title>Категорий пока нет</EmptyState.Title>
                <EmptyState.Description>
                  Создайте первую — например, «Еда» или «Транспорт»
                </EmptyState.Description>
              </VStack>
            </EmptyState.Content>
          </EmptyState.Root>
        </GlassCard>
      ) : (
        <Box
          borderWidth="1px"
          borderColor="border"
          borderRadius="l3"
          bg="bg.panel/82"
          backdropFilter="blur(20px)"
          overflow="hidden"
        >
          {categories.data.map((category, index) => (
            <CategoryRow
              key={category.id}
              category={category}
              all={categories.data}
              first={index === 0}
              spent={spentById.get(category.id) ?? 0}
              spentLoading={spending.isPending}
              monthWord={monthPrepositional(currentMonth)}
            />
          ))}
        </Box>
      )}
    </Stack>
  )
}

interface CategoryRowProps {
  category: CategoryDto
  all: readonly CategoryDto[]
  first: boolean
  spent: number
  spentLoading: boolean
  /** «октябре» */
  monthWord: string
}

function CategoryRow({ category, all, first, spent, spentLoading, monthWord }: CategoryRowProps) {
  const count = useCategorySplitCount(category.id)
  const rename = useRenameCategory()
  const remove = useDeleteCategory()
  const [draft, setDraft] = useState<string | null>(null)
  const [renameError, setRenameError] = useState<string | null>(null)
  const [blocked, setBlocked] = useState(false)

  const parts = count.data
  const editing = draft !== null

  async function saveRename() {
    const trimmed = (draft ?? '').trim()
    if (!trimmed) return
    if (trimmed === category.categoryName) {
      setDraft(null)
      return
    }
    if (findDuplicate(all, trimmed, category.id)) {
      setRenameError(`Категория «${trimmed}» уже есть`)
      return
    }
    try {
      await rename.mutateAsync({ id: category.id, name: trimmed })
      notify.success('Изменения сохранены')
      setDraft(null)
      setRenameError(null)
    } catch (e) {
      setRenameError(errorMessage(e))
    }
  }

  async function onDelete() {
    // Счётчик уже известен — не гоняем запрос, чтобы заранее получить 409
    if (parts !== undefined && parts > 0) {
      setBlocked(true)
      return
    }
    try {
      await remove.mutateAsync(category.id)
      notify.info('Категория удалена', category.categoryName)
    } catch (e) {
      if (isApiError(e, 'CATEGORY_IN_USE')) {
        setBlocked(true)
        void count.refetch()
      } else {
        notify.error('Не удалось удалить', errorMessage(e))
      }
    }
  }

  return (
    <Stack gap="2" px="4" py="3" borderTopWidth={first ? '0' : '1px'} borderColor="border.muted">
      <Flex align="center" gap="3">
        <CategoryIcon categoryId={category.id} name={category.categoryName} />
        {editing ? (
          <Flex flex="1" minW="0" gap="1.5">
            <Input
              size="sm"
              autoFocus
              aria-label={`Новое название категории «${category.categoryName}»`}
              maxLength={MAX_CATEGORY_NAME}
              value={draft}
              onChange={(event) => {
                setDraft(event.currentTarget.value)
                setRenameError(null)
              }}
              onKeyDown={(event) => {
                if (event.key === 'Enter') void saveRename()
                if (event.key === 'Escape') setDraft(null)
              }}
            />
            <IconButton
              size="sm"
              aria-label="Сохранить"
              loading={rename.isPending}
              onClick={() => void saveRename()}
            >
              <Check />
            </IconButton>
            <IconButton
              size="sm"
              variant="ghost"
              aria-label="Отмена"
              onClick={() => {
                setDraft(null)
                setRenameError(null)
              }}
            >
              <X />
            </IconButton>
          </Flex>
        ) : (
          <>
            <Box flex="1" minW="0">
              <Text fontWeight="500" lineClamp="1">
                {category.categoryName}
              </Text>
              <Text as="div" fontSize="xs" color="fg.muted">
                {parts === undefined || spentLoading ? (
                  <Skeleton display="inline-block" h="3" w="140px" />
                ) : parts > 0 ? (
                  `${parts} ${plural(parts, FORMS.splitPart)} · ${formatMoney(spent)} в ${monthWord}`
                ) : (
                  'Пока без трат'
                )}
              </Text>
            </Box>
            <IconButton
              size="sm"
              variant="ghost"
              aria-label={`Переименовать «${category.categoryName}»`}
              onClick={() => {
                setDraft(category.categoryName)
                setBlocked(false)
              }}
            >
              <Pencil />
            </IconButton>
            <IconButton
              size="sm"
              variant="ghost"
              aria-label={`Удалить «${category.categoryName}»`}
              loading={remove.isPending}
              onClick={() => void onDelete()}
            >
              <Trash2 />
            </IconButton>
          </>
        )}
      </Flex>

      {renameError && (
        <Text role="alert" fontSize="xs" fontWeight="500" color="fg.error" pl="13">
          {renameError}
        </Text>
      )}

      {blocked && (
        <Alert.Root status="warning" borderRadius="l3">
          <Alert.Indicator />
          <Alert.Content>
            <Alert.Title>Нельзя удалить «{category.categoryName}»</Alert.Title>
            <Alert.Description>
              {parts !== undefined && parts > 0
                ? `К категории привязано ${parts} ${plural(parts, FORMS.splitPart)}. `
                : 'К категории привязаны траты. '}
              Перенесите их в другую категорию — так история останется целой.
            </Alert.Description>
          </Alert.Content>
          <IconButton size="xs" variant="ghost" aria-label="Скрыть сообщение" onClick={() => setBlocked(false)}>
            <X />
          </IconButton>
        </Alert.Root>
      )}
    </Stack>
  )
}

function ListSkeleton() {
  return (
    <Stack
      gap="0"
      borderWidth="1px"
      borderColor="border"
      borderRadius="l3"
      bg="bg.panel/82"
      aria-busy="true"
      aria-label="Загрузка категорий"
    >
      {[0, 1, 2, 3].map((row) => (
        <Flex key={row} align="center" gap="3" px="4" py="3">
          <Skeleton w="40px" h="40px" borderRadius="l3" />
          <Stack flex="1" gap="2">
            <Skeleton h="4" w="35%" />
            <Skeleton h="3" w="25%" />
          </Stack>
        </Flex>
      ))}
    </Stack>
  )
}
