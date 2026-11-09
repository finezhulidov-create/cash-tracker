import { Button, chakra, Drawer, Field, Flex, Grid, Input, Span } from '@chakra-ui/react'
import { Trash2, X } from 'lucide-react'
import { useReducer } from 'react'
import { ApiError } from '../../api/client'
import { errorMessage } from '../../api/errors'
import {
  useCreateTransaction,
  useDeleteTransaction,
  useUpdateTransaction,
} from '../../api/hooks'
import type { CategoryDto, TransactionDto } from '../../api/types'
import { Form } from '../../components/Form'
import { IridescentButton } from '../../components/IridescentButton'
import { formatMoney } from '../../lib/money'
import { notify } from '../../lib/notify'
import { buildRequest, formReducer, initialFormState } from './formState'
import { SplitEditor } from './SplitEditor'

interface TransactionFormProps {
  /** Редактируемая трата; null — новая */
  transaction: TransactionDto | null
  categories: readonly CategoryDto[]
  onClose: () => void
}

/** Ошибки валидации бэкенда по полям формы (имя поля → сообщение). */
function fieldErrorsOf(error: unknown): Record<string, string> {
  return error instanceof ApiError ? error.fieldErrors : {}
}

/** Содержимое шторки: сумма, описание, дата и время, разбивка по категориям; футер с действиями. */
export function TransactionForm({ transaction, categories, onClose }: TransactionFormProps) {
  const [state, dispatch] = useReducer(formReducer, undefined, () =>
    initialFormState(transaction, categories),
  )
  const create = useCreateTransaction()
  const update = useUpdateTransaction()
  const remove = useDeleteTransaction()

  const request = buildRequest(state)
  const saving = create.isPending || update.isPending
  const fieldErrors = fieldErrorsOf(create.error ?? update.error)

  async function save() {
    if (!request) return
    try {
      if (transaction) {
        await update.mutateAsync({ id: transaction.id, body: request })
        notify.success('Изменения сохранены')
      } else {
        await create.mutateAsync(request)
        notify.success('Трата добавлена', `${request.description} · ${formatMoney(request.amount)}`)
      }
      onClose()
    } catch (error) {
      notify.error('Не удалось сохранить', errorMessage(error))
    }
  }

  async function onDeleteClick() {
    if (!transaction) return
    if (!state.confirmDelete) {
      dispatch({ type: 'confirmDelete', value: true })
      return
    }
    try {
      await remove.mutateAsync(transaction.id)
      notify.success('Трата удалена')
      onClose()
    } catch (error) {
      notify.error('Не удалось удалить', errorMessage(error))
      dispatch({ type: 'confirmDelete', value: false })
    }
  }

  return (
    <Form
      display="flex"
      flexDirection="column"
      flex="1"
      minH="0"
      onSubmit={(event) => {
        event.preventDefault()
        void save()
      }}
    >
      <Drawer.Header pt="4" pb="3" pl="6" pr="5" gap="3">
        <Drawer.Title fontFamily="heading" fontSize="xl" fontWeight="600" flex="1">
          {transaction ? 'Редактирование траты' : 'Новая трата'}
        </Drawer.Title>
        <Drawer.CloseTrigger asChild position="static">
          <chakra.button
            type="button"
            aria-label="Закрыть"
            display="inline-flex"
            alignItems="center"
            justifyContent="center"
            w="9"
            h="9"
            borderRadius="l2"
            color="fg.muted"
            cursor="pointer"
            _hover={{ bg: 'bg.muted', color: 'fg' }}
          >
            <X size={18} aria-hidden />
          </chakra.button>
        </Drawer.CloseTrigger>
      </Drawer.Header>

      <Drawer.Body px="6" pt="1" pb="6" display="flex" flexDirection="column" gap="5">
        <Field.Root invalid={Boolean(fieldErrors.amount)}>
          <Field.Label>Сумма</Field.Label>
          <Flex align="center" gap="2" w="full">
            <Input
              variant="flushed"
              h="64px"
              fontFamily="heading"
              fontSize="40px"
              fontWeight="600"
              letterSpacing="-0.02em"
              className="ct-num"
              placeholder="0"
              inputMode="decimal"
              autoComplete="off"
              autoFocus={!transaction}
              value={state.amount}
              onChange={(event) => dispatch({ type: 'amount', value: event.currentTarget.value })}
            />
            <Span fontFamily="heading" fontSize="36px" fontWeight="600" color="fg.muted" aria-hidden>
              ₽
            </Span>
          </Flex>
          <Field.ErrorText>{fieldErrors.amount}</Field.ErrorText>
        </Field.Root>

        <Field.Root invalid={Boolean(fieldErrors.description)}>
          <Field.Label>Описание</Field.Label>
          <Input
            placeholder="Например, Супермаркет"
            maxLength={255}
            autoComplete="off"
            value={state.description}
            onChange={(event) => dispatch({ type: 'description', value: event.currentTarget.value })}
          />
          <Field.ErrorText>{fieldErrors.description}</Field.ErrorText>
        </Field.Root>

        <Grid templateColumns="minmax(0, 1.4fr) minmax(0, 1fr)" gap="3">
          <Field.Root invalid={Boolean(fieldErrors.dateTime)}>
            <Field.Label>Дата</Field.Label>
            <Input
              type="date"
              value={state.date}
              onChange={(event) => dispatch({ type: 'date', value: event.currentTarget.value })}
            />
          </Field.Root>
          <Field.Root>
            <Field.Label>Время</Field.Label>
            <Input
              type="time"
              value={state.time}
              onChange={(event) => dispatch({ type: 'time', value: event.currentTarget.value })}
            />
          </Field.Root>
        </Grid>

        <SplitEditor
          categories={categories}
          splits={state.splits}
          amountText={state.amount}
          hasDescription={state.description.trim() !== ''}
          dispatch={dispatch}
        />
      </Drawer.Body>

      <Drawer.Footer
        gap="2"
        px="6"
        pt="4"
        borderTopWidth="1px"
        borderColor="border.muted"
        style={{ paddingBottom: 'calc(16px + env(safe-area-inset-bottom))' }}
      >
        {transaction && (
          <Button
            type="button"
            mr="auto"
            colorPalette="red"
            variant={state.confirmDelete ? 'solid' : 'ghost'}
            loading={remove.isPending}
            onClick={() => void onDeleteClick()}
          >
            <Trash2 aria-hidden />
            {state.confirmDelete ? 'Точно удалить?' : 'Удалить'}
          </Button>
        )}
        <Button type="button" variant="outline" onClick={onClose}>
          Отмена
        </Button>
        <IridescentButton type="submit" disabled={!request} loading={saving}>
          Сохранить
        </IridescentButton>
      </Drawer.Footer>
    </Form>
  )
}
