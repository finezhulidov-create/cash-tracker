import type { CategoryDto, TransactionDto, TransactionRequest } from '../../api/types'
import { splitLocalDateTime, toIsoDate, toLocalDateTime, toTimeString } from '../../lib/dates'
import { centsToInput, fromCents, sanitizeAmountInput, toCents } from '../../lib/money'
import type { DraftSplit } from '../../lib/splits'
import {
  computeSplitStatus,
  evenSplit,
  isSplitStatusValid,
  nextUnusedCategory,
  totalCents,
} from '../../lib/splits'

export interface FormState {
  amount: string
  description: string
  /** YYYY-MM-DD */
  date: string
  /** HH:mm */
  time: string
  splits: DraftSplit[]
  /** Первый клик по «Удалить» просит подтверждения */
  confirmDelete: boolean
}

export type FormAction =
  | { type: 'amount'; value: string }
  | { type: 'description'; value: string }
  | { type: 'date'; value: string }
  | { type: 'time'; value: string }
  | { type: 'category'; key: string; categoryId: number }
  | { type: 'splitAmount'; key: string; value: string }
  | { type: 'addSplit'; key: string; categoryIds: readonly number[] }
  | { type: 'removeSplit'; key: string }
  | { type: 'evenSplit' }
  | { type: 'fillRemainder' }
  | { type: 'confirmDelete'; value: boolean }

let keySeq = 0
export function newSplitKey(): string {
  keySeq += 1
  return `split-${keySeq}`
}

/** Начальное состояние: для редактирования — из траты, для новой — сегодня, текущее время, первая категория. */
export function initialFormState(
  transaction: TransactionDto | null,
  categories: readonly CategoryDto[],
  now: Date = new Date(),
): FormState {
  if (transaction) {
    const { date, time } = splitLocalDateTime(transaction.dateTime)
    return {
      amount: centsToInput(toCents(transaction.amount)),
      description: transaction.description,
      date,
      time,
      splits: transaction.splits.map((split) => ({
        key: `split-${split.id}`,
        categoryId: split.category.id,
        amount: centsToInput(toCents(split.amount)),
      })),
      confirmDelete: false,
    }
  }
  return {
    amount: '',
    description: '',
    date: toIsoDate(now),
    time: toTimeString(now),
    splits: [{ key: newSplitKey(), categoryId: categories[0]?.id ?? null, amount: '' }],
    confirmDelete: false,
  }
}

export function formReducer(state: FormState, action: FormAction): FormState {
  switch (action.type) {
    case 'amount':
      return { ...state, amount: sanitizeAmountInput(action.value) }
    case 'description':
      return { ...state, description: action.value }
    case 'date':
      return { ...state, date: action.value }
    case 'time':
      return { ...state, time: action.value }
    case 'category':
      return {
        ...state,
        splits: state.splits.map((split) =>
          split.key === action.key ? { ...split, categoryId: action.categoryId } : split,
        ),
      }
    case 'splitAmount':
      return {
        ...state,
        splits: state.splits.map((split) =>
          split.key === action.key ? { ...split, amount: sanitizeAmountInput(action.value) } : split,
        ),
      }
    case 'addSplit': {
      const categoryId = nextUnusedCategory(action.categoryIds, state.splits)
      if (categoryId === null) return state
      // Из режима «вся сумма в одной категории» первая часть забирает всю сумму
      const existing =
        state.splits.length === 1
          ? state.splits.map((split) => ({ ...split, amount: state.amount }))
          : state.splits
      return { ...state, splits: [...existing, { key: action.key, categoryId, amount: '' }] }
    }
    case 'removeSplit': {
      const rest = state.splits.filter((split) => split.key !== action.key)
      if (rest.length === state.splits.length || rest.length === 0) return state
      // Одна оставшаяся часть снова означает «вся сумма», её собственная сумма не хранится
      return { ...state, splits: rest.length === 1 ? rest.map((s) => ({ ...s, amount: '' })) : rest }
    }
    case 'evenSplit': {
      const total = totalCents(state.amount)
      if (total <= 0 || state.splits.length < 2) return state
      const parts = evenSplit(total, state.splits.length)
      return {
        ...state,
        splits: state.splits.map((split, index) => ({ ...split, amount: centsToInput(parts[index] ?? 0) })),
      }
    }
    case 'fillRemainder': {
      const status = computeSplitStatus(state.amount, state.splits)
      const remaining =
        status.kind === 'remaining' ? status.cents : status.kind === 'zero-part' ? status.remainingCents : 0
      if (remaining <= 0) return state
      // Остаток уходит в последнюю часть без суммы, а если такой нет — в последнюю
      const emptyIndex = state.splits.findLastIndex((split) => totalCents(split.amount) <= 0)
      const targetIndex = emptyIndex === -1 ? state.splits.length - 1 : emptyIndex
      return {
        ...state,
        splits: state.splits.map((split, index) =>
          index === targetIndex
            ? { ...split, amount: centsToInput(totalCents(split.amount) + remaining) }
            : split,
        ),
      }
    }
    case 'confirmDelete':
      return { ...state, confirmDelete: action.value }
  }
}

export function isFormValid(state: FormState): boolean {
  return (
    isSplitStatusValid(computeSplitStatus(state.amount, state.splits)) &&
    state.description.trim() !== '' &&
    state.date !== ''
  )
}

/** Тело запроса к API или null, если форма ещё не готова к отправке. */
export function buildRequest(state: FormState): TransactionRequest | null {
  if (!isFormValid(state)) return null
  const total = totalCents(state.amount)
  const single = state.splits.length === 1
  return {
    description: state.description.trim(),
    amount: fromCents(total),
    dateTime: toLocalDateTime(state.date, state.time),
    splits: state.splits.map((split) => ({
      categoryId: split.categoryId as number,
      amount: fromCents(single ? total : totalCents(split.amount)),
    })),
  }
}
