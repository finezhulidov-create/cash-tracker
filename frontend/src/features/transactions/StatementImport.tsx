import {
  Alert,
  Badge,
  Box,
  Button,
  Checkbox,
  CloseButton,
  Dialog,
  Flex,
  NativeSelect,
  Portal,
  Spinner,
  Stack,
  Text,
} from '@chakra-ui/react'
import { FileUp } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { errorMessage } from '../../api/errors'
import { useCategories, useParseStatement, useSaveImportedTransactions } from '../../api/hooks'
import type { ImportPreviewItem, TransactionRequest } from '../../api/types'
import { QueryError } from '../../components/QueryError'
import { useIsPhone } from '../../layout/useLayoutMode'
import { formatShortDate, formatTime } from '../../lib/dates'
import { formatMoney } from '../../lib/money'
import { notify } from '../../lib/notify'
import { FORMS, plural } from '../../lib/plural'

/** «Добавить 1 операцию / 2 операции / 5 операций» — винительный падеж. */
const OPERATION_ACCUSATIVE: [string, string, string] = ['операцию', 'операции', 'операций']

/** Совпадает с spring.servlet.multipart.max-file-size на сервере. */
export const MAX_STATEMENT_SIZE = 10 * 1024 * 1024

/** Строка предпросмотра: что пришло с сервера и что выбрал пользователь. */
interface Row {
  item: ImportPreviewItem
  selected: boolean
  categoryId: number
}

function toRows(items: ImportPreviewItem[]): Row[] {
  return items.map((item) => ({
    item,
    // дубль по умолчанию не добавляем, но пользователь может включить его сам
    selected: !item.duplicate,
    categoryId: item.splits[0]?.categoryId ?? 0,
  }))
}

function toRequest({ item, categoryId }: Row): TransactionRequest {
  return {
    description: item.description,
    amount: item.amount,
    dateTime: item.dateTime,
    splits: [{ categoryId, amount: item.amount }],
  }
}

function isPdf(file: File): boolean {
  return file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf')
}

/** Кнопка «Импорт из PDF»: выбор файла → разбор моделью → предпросмотр с галочками → сохранение пачкой. */
export function StatementImportButton() {
  const phone = useIsPhone()
  const input = useRef<HTMLInputElement>(null)
  const pending = useRef<AbortController | null>(null)
  const [open, setOpen] = useState(false)
  const [file, setFile] = useState<File | null>(null)
  const [rows, setRows] = useState<Row[]>([])
  const parse = useParseStatement()
  const save = useSaveImportedTransactions()
  const categories = useCategories()

  // ушли со страницы — ответ модели уже не нужен
  useEffect(() => () => pending.current?.abort(), [])

  function start(selected: File) {
    const controller = new AbortController()
    pending.current?.abort()
    pending.current = controller
    setFile(selected)
    setRows([])
    save.reset()
    setOpen(true)
    parse.mutate(
      { file: selected, signal: controller.signal },
      { onSuccess: (items) => setRows(toRows(items)) },
    )
  }

  function onFileChosen(event: React.ChangeEvent<HTMLInputElement>) {
    const selected = event.currentTarget.files?.[0]
    event.currentTarget.value = '' // тот же файл можно выбрать ещё раз
    if (!selected) return
    if (!isPdf(selected)) {
      notify.error('Нужен PDF-файл', 'Выгрузите выписку из банка в формате PDF')
      return
    }
    if (selected.size > MAX_STATEMENT_SIZE) {
      notify.error('Файл слишком большой', 'Можно загрузить выписку до 10 МБ')
      return
    }
    start(selected)
  }

  function close() {
    if (parse.isPending) pending.current?.abort()
    setOpen(false)
    parse.reset()
    save.reset()
    setRows([])
    setFile(null)
  }

  function toggle(index: number, selected: boolean) {
    setRows((previous) => previous.map((row, i) => (i === index ? { ...row, selected } : row)))
  }

  function setCategory(index: number, categoryId: number) {
    setRows((previous) => previous.map((row, i) => (i === index ? { ...row, categoryId } : row)))
  }

  const chosen = rows.filter((row) => row.selected)
  const duplicates = rows.filter((row) => row.item.duplicate).length

  function submit() {
    save.mutate(chosen.map(toRequest), {
      onSuccess: (created) => {
        notify.success(`Добавлено: ${created.length} ${plural(created.length, FORMS.operation)}`)
        close()
      },
    })
  }

  return (
    <>
      <input
        ref={input}
        type="file"
        accept="application/pdf,.pdf"
        hidden
        data-testid="statement-file"
        onChange={onFileChosen}
      />
      <Button variant="outline" onClick={() => input.current?.click()}>
        <FileUp aria-hidden />
        Импорт из PDF
      </Button>

      <Dialog.Root
        open={open}
        onOpenChange={(event) => {
          if (!event.open && !save.isPending) close()
        }}
        size={phone ? 'full' : 'lg'}
        scrollBehavior="inside"
        placement="center"
      >
        <Portal>
          <Dialog.Backdrop bg="rgba(0, 0, 0, 0.36)" />
          <Dialog.Positioner>
            <Dialog.Content bg="bg.panel" borderRadius={phone ? '0' : 'l3'}>
              <Dialog.Header>
                <Stack gap="0.5">
                  <Dialog.Title>Импорт выписки</Dialog.Title>
                  {file && (
                    <Text fontSize="sm" color="fg.muted" truncate>
                      {file.name}
                    </Text>
                  )}
                </Stack>
              </Dialog.Header>
              <Dialog.CloseTrigger asChild>
                <CloseButton size="sm" aria-label="Закрыть окно" disabled={save.isPending} />
              </Dialog.CloseTrigger>

              <Dialog.Body>
                {parse.isPending ? (
                  <Flex role="status" align="center" gap="4" py="6">
                    <Spinner size="md" />
                    <Box>
                      <Text fontWeight="500">Распознаём операции…</Text>
                      <Text fontSize="sm" color="fg.muted">
                        Модель читает каждую страницу выписки, это может занять пару минут.
                      </Text>
                    </Box>
                  </Flex>
                ) : parse.isError ? (
                  <QueryError
                    error={parse.error}
                    title="Не удалось разобрать выписку"
                    onRetry={file ? () => start(file) : undefined}
                  />
                ) : rows.length === 0 ? (
                  <Text color="fg.muted" py="4">
                    В выписке не нашлось трат. Поступления не импортируются: в приложении учитываются только расходы.
                  </Text>
                ) : (
                  <Stack gap="3">
                    <Text fontSize="sm" color="fg.muted">
                      Найдено {rows.length} {plural(rows.length, FORMS.operation)}
                      {duplicates > 0 && ` · уже есть в приложении: ${duplicates}, они не отмечены`}
                    </Text>

                    {save.isError && (
                      <Alert.Root status="error">
                        <Alert.Indicator />
                        <Alert.Title>{errorMessage(save.error)}</Alert.Title>
                      </Alert.Root>
                    )}

                    <Stack gap="1" role="list" aria-label="Операции из выписки">
                      {rows.map((row, index) => (
                        <PreviewRow
                          key={index}
                          row={row}
                          categories={categories.data ?? []}
                          disabled={save.isPending}
                          onToggle={(selected) => toggle(index, selected)}
                          onCategory={(categoryId) => setCategory(index, categoryId)}
                        />
                      ))}
                    </Stack>
                  </Stack>
                )}
              </Dialog.Body>

              <Dialog.Footer>
                {parse.isPending ? (
                  <Button variant="outline" onClick={close}>
                    Отменить
                  </Button>
                ) : (
                  <>
                    <Button variant="ghost" onClick={close} disabled={save.isPending}>
                      {rows.length === 0 ? 'Закрыть' : 'Отмена'}
                    </Button>
                    {rows.length > 0 && (
                      <Button
                        colorPalette="purple"
                        onClick={submit}
                        loading={save.isPending}
                        disabled={chosen.length === 0}
                      >
                        {chosen.length === 0
                          ? 'Ничего не выбрано'
                          : `Добавить ${chosen.length} ${plural(chosen.length, OPERATION_ACCUSATIVE)}`}
                      </Button>
                    )}
                  </>
                )}
              </Dialog.Footer>
            </Dialog.Content>
          </Dialog.Positioner>
        </Portal>
      </Dialog.Root>
    </>
  )
}

interface PreviewRowProps {
  row: Row
  categories: { id: number; categoryName: string }[]
  disabled: boolean
  onToggle: (selected: boolean) => void
  onCategory: (categoryId: number) => void
}

function PreviewRow({ row, categories, disabled, onToggle, onCategory }: PreviewRowProps) {
  const { item, selected, categoryId } = row
  return (
    <Flex
      role="listitem"
      gap="3"
      align="flex-start"
      px="3"
      py="2.5"
      borderRadius="l2"
      bg={selected ? 'bg.subtle' : undefined}
      opacity={selected ? 1 : 0.7}
    >
      <Checkbox.Root
        checked={selected}
        disabled={disabled}
        onCheckedChange={(event) => onToggle(event.checked === true)}
        mt="0.5"
        aria-label={`Добавить: ${item.description}`}
      >
        <Checkbox.HiddenInput />
        <Checkbox.Control />
      </Checkbox.Root>

      <Stack gap="1.5" flex="1" minW="0">
        <Flex gap="2" align="baseline" justify="space-between">
          <Text fontWeight="500" truncate>
            {item.description}
          </Text>
          <Text className="ct-num" fontWeight="600" flexShrink={0}>
            {formatMoney(item.amount)}
          </Text>
        </Flex>
        <Flex gap="2" align="center" wrap="wrap">
          <Text fontSize="sm" color="fg.muted">
            {formatShortDate(item.dateTime)}, {formatTime(item.dateTime)}
          </Text>
          {item.duplicate && (
            <Badge colorPalette="orange" variant="subtle">
              Уже есть
            </Badge>
          )}
          <NativeSelect.Root size="xs" width="auto" ml="auto" disabled={disabled || !selected}>
            <NativeSelect.Field
              aria-label={`Категория: ${item.description}`}
              value={categoryId}
              onChange={(event) => onCategory(Number(event.currentTarget.value))}
            >
              {/* пока список категорий грузится, показываем хотя бы текущую */}
              {categories.length === 0 && <option value={categoryId}>Прочее</option>}
              {categories.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.categoryName}
                </option>
              ))}
            </NativeSelect.Field>
            <NativeSelect.Indicator />
          </NativeSelect.Root>
        </Flex>
      </Stack>
    </Flex>
  )
}
