import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { afterEach, vi } from 'vitest'
import { statementImportApi } from '../../api/endpoints'
import type { ImportPreviewItem, TransactionRequest } from '../../api/types'
import { categories, food, page, transport } from '../../test/fixtures'
import { renderWithProviders } from '../../test/render'
import { API, server } from '../../test/server'
import { MAX_STATEMENT_SIZE, StatementImportButton } from './StatementImport'

const OTHER = { id: 9, categoryName: 'Прочее', userId: 1 }

function item(description: string, amount: number, dateTime: string, duplicate = false): ImportPreviewItem {
  return { description, amount, dateTime, duplicate, splits: [{ categoryId: OTHER.id, amount }] }
}

const coffee = item('Кофейня', 350, '2026-10-28T14:30:00', true)
const taxi = item('Такси', 1200, '2026-10-29T09:15:00')
const cinema = item('Кинотеатр', 800, '2026-10-30T19:00:00')

function pdf(name = 'statement.pdf') {
  return new File(['%PDF-1.7'], name, { type: 'application/pdf' })
}

afterEach(() => vi.restoreAllMocks())

/**
 * Мок разбора: считает запросы. Тело здесь не разбираем: в jsdom FormData — объект jsdom, и Node fetch
 * его не сериализует; что именно уходит в fetch, проверяет uploadedFiles().
 */
function mockParse(items: ImportPreviewItem[]) {
  const calls: number[] = []
  server.use(
    http.get(`${API}/v1/categories/mine`, () => HttpResponse.json(page([...categories, OTHER]))),
    http.post(`${API}/v1/parsing`, () => {
      calls.push(calls.length + 1)
      return HttpResponse.json(items)
    }),
  )
  return calls
}

/** Следит за fetch и возвращает имена файлов из поля «file» каждого запроса на разбор. */
function uploadedFiles() {
  const spy = vi.spyOn(globalThis, 'fetch')
  return () =>
    spy.mock.calls
      .filter(([url]) => String(url).endsWith('/v1/parsing'))
      .map(([, init]) => {
        const body = init?.body
        const file = body instanceof FormData ? body.get('file') : null
        return {
          name: file instanceof File ? file.name : null,
          contentType: (init?.headers as Record<string, string> | undefined)?.['Content-Type'],
        }
      })
}

/** Мок сохранения: запоминает тела запросов и отвечает созданными тратами. */
function mockSave() {
  const saved: TransactionRequest[][] = []
  server.use(
    http.post(`${API}/v1/parsing/save`, async ({ request }) => {
      const body = (await request.json()) as { transactions: TransactionRequest[] }
      saved.push(body.transactions)
      return HttpResponse.json(
        body.transactions.map((t, index) => ({ id: index + 1, userId: 1, ...t, splits: [] })),
      )
    }),
  )
  return saved
}

async function upload(file = pdf(), options: { applyAccept?: boolean } = {}) {
  const user = userEvent.setup(options)
  await user.upload(screen.getByTestId('statement-file'), file)
  return user
}

function rows() {
  return within(screen.getByRole('list', { name: 'Операции из выписки' })).getAllByRole('listitem')
}

describe('StatementImportButton — preview', () => {
  it('uploads the chosen PDF as the "file" part and shows found operations', async () => {
    mockParse([coffee, taxi])
    const sent = uploadedFiles()
    renderWithProviders(<StatementImportButton />)

    await upload(pdf('октябрь.pdf'))

    expect(await screen.findByText('Такси')).toBeInTheDocument()
    // multipart: Content-Type с границей частей ставит сам браузер, JSON-заголовка быть не должно
    expect(sent()).toEqual([{ name: 'октябрь.pdf', contentType: undefined }])
    expect(screen.getByText('октябрь.pdf')).toBeInTheDocument()
    expect(rows()).toHaveLength(2)
    expect(screen.getByText(/Найдено 2 операции/)).toBeInTheDocument()
  })

  it('leaves duplicates unchecked and marks them', async () => {
    mockParse([coffee, taxi])
    renderWithProviders(<StatementImportButton />)

    await upload()

    expect(await screen.findByRole('checkbox', { name: 'Добавить: Такси' })).toBeChecked()
    expect(screen.getByRole('checkbox', { name: 'Добавить: Кофейня' })).not.toBeChecked()
    expect(within(rows()[0]!).getByText('Уже есть')).toBeInTheDocument()
    expect(within(rows()[1]!).queryByText('Уже есть')).not.toBeInTheDocument()
    expect(screen.getByText(/уже есть в приложении: 1/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Добавить 1 операцию' })).toBeEnabled()
  })

  it('shows date, time and amount of each operation', async () => {
    mockParse([taxi])
    renderWithProviders(<StatementImportButton />)

    await upload()

    const row = (await screen.findByText('Такси')).closest('[role="listitem"]') as HTMLElement
    expect(within(row).getByText('29 окт, 09:15')).toBeInTheDocument()
    expect(within(row).getByText(/1\s200/)).toBeInTheDocument()
  })

  it('explains when the statement has no expenses', async () => {
    mockParse([])
    renderWithProviders(<StatementImportButton />)

    await upload()

    expect(await screen.findByText(/В выписке не нашлось трат/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^Добавить/ })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Закрыть' })).toBeInTheDocument()
  })
})

describe('StatementImportButton — saving', () => {
  it('saves only checked operations and closes with a toast', async () => {
    mockParse([coffee, taxi, cinema])
    const saved = mockSave()
    renderWithProviders(<StatementImportButton />)
    const user = await upload()

    await user.click(await screen.findByRole('button', { name: 'Добавить 2 операции' }))

    await waitFor(() => expect(saved).toHaveLength(1))
    expect(saved[0]).toEqual([
      { description: 'Такси', amount: 1200, dateTime: '2026-10-29T09:15:00', splits: [{ categoryId: OTHER.id, amount: 1200 }] },
      { description: 'Кинотеатр', amount: 800, dateTime: '2026-10-30T19:00:00', splits: [{ categoryId: OTHER.id, amount: 800 }] },
    ])
    expect(await screen.findByText('Добавлено: 2 операции')).toBeInTheDocument()
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  })

  it('a duplicate can be added on purpose', async () => {
    mockParse([coffee])
    const saved = mockSave()
    renderWithProviders(<StatementImportButton />)
    const user = await upload()

    expect(await screen.findByRole('button', { name: 'Ничего не выбрано' })).toBeDisabled()
    await user.click(screen.getByRole('checkbox', { name: 'Добавить: Кофейня' }))
    await user.click(screen.getByRole('button', { name: 'Добавить 1 операцию' }))

    await waitFor(() => expect(saved[0]?.map((t) => t.description)).toEqual(['Кофейня']))
  })

  it('sends the category picked for an operation', async () => {
    mockParse([taxi, cinema])
    const saved = mockSave()
    renderWithProviders(<StatementImportButton />)
    const user = await upload()
    const select = await screen.findByRole('combobox', { name: 'Категория: Такси' })
    await waitFor(() => expect(within(select).getByRole('option', { name: transport.categoryName })).toBeInTheDocument())

    await user.selectOptions(select, String(transport.id))
    await user.click(screen.getByRole('button', { name: 'Добавить 2 операции' }))

    await waitFor(() => expect(saved).toHaveLength(1))
    expect(saved[0]![0]!.splits).toEqual([{ categoryId: transport.id, amount: 1200 }])
    expect(saved[0]![1]!.splits).toEqual([{ categoryId: OTHER.id, amount: 800 }])
  })

  it('offers all categories of the user', async () => {
    mockParse([taxi])
    renderWithProviders(<StatementImportButton />)

    await upload()

    const select = await screen.findByRole('combobox', { name: 'Категория: Такси' })
    await waitFor(() => expect(within(select).getAllByRole('option')).toHaveLength(categories.length + 1))
    expect(within(select).getByRole('option', { name: food.categoryName })).toBeInTheDocument()
    expect(select).toHaveValue(String(OTHER.id))
  })

  it('keeps the preview open and explains a failed save', async () => {
    mockParse([taxi])
    server.use(
      http.post(`${API}/v1/parsing/save`, () =>
        HttpResponse.json(
          { status: 404, errorCode: 'ACCESS_DENIED', message: 'Resource not Found', timestamp: '2026-10-31T12:00:00' },
          { status: 404 },
        ),
      ),
    )
    renderWithProviders(<StatementImportButton />)
    const user = await upload()

    await user.click(await screen.findByRole('button', { name: 'Добавить 1 операцию' }))

    expect(await screen.findByText(/Не найдено/)).toBeInTheDocument()
    expect(screen.getByText('Такси')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Добавить 1 операцию' })).toBeEnabled()
  })
})

describe('StatementImportButton — file checks and errors', () => {
  it('rejects a file that is not PDF without calling the server', async () => {
    const calls = mockParse([taxi])
    renderWithProviders(<StatementImportButton />)

    await upload(new File(['hello'], 'notes.txt', { type: 'text/plain' }), { applyAccept: false })

    expect(await screen.findByText('Нужен PDF-файл')).toBeInTheDocument()
    expect(calls).toEqual([])
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('rejects a file larger than the server limit', async () => {
    const calls = mockParse([taxi])
    renderWithProviders(<StatementImportButton />)
    const big = pdf()
    Object.defineProperty(big, 'size', { value: MAX_STATEMENT_SIZE + 1 })

    await upload(big)

    expect(await screen.findByText('Файл слишком большой')).toBeInTheDocument()
    expect(calls).toEqual([])
  })

  it('shows the server message for a broken PDF and retries with the same file', async () => {
    server.use(
      http.get(`${API}/v1/categories/mine`, () => HttpResponse.json(page(categories))),
      http.post(`${API}/v1/parsing`, () =>
        HttpResponse.json(
          { status: 400, errorCode: 'INVALID_REQUEST', message: 'Поддержка только пдф файлов', timestamp: '2026-10-31T12:00:00' },
          { status: 400 },
        ),
      ),
    )
    const sent = uploadedFiles()
    renderWithProviders(<StatementImportButton />)
    const user = await upload(pdf('битый.pdf'))

    expect(await screen.findByText('Не удалось разобрать выписку')).toBeInTheDocument()
    expect(screen.getByText('Поддержка только пдф файлов')).toBeInTheDocument()

    mockParse([taxi])
    await user.click(screen.getByRole('button', { name: 'Повторить' }))

    expect(await screen.findByText('Такси')).toBeInTheDocument()
    expect(sent().map((request) => request.name)).toEqual(['битый.pdf', 'битый.pdf'])
  })

  it('shows a wait message while the model works and can be cancelled', async () => {
    // висящий запрос с FormData из jsdom Node fetch обрывает как сетевую ошибку, поэтому «модель»
    // подменяется на уровне API: она отвечает только на отмену
    server.use(http.get(`${API}/v1/categories/mine`, () => HttpResponse.json(page(categories))))
    let aborted = false
    vi.spyOn(statementImportApi, 'parse').mockImplementation(
      (_file, signal) =>
        new Promise((_, reject) => {
          signal?.addEventListener('abort', () => {
            aborted = true
            reject(new DOMException('Aborted', 'AbortError'))
          })
        }),
    )
    renderWithProviders(<StatementImportButton />)
    const user = await upload()

    expect(await screen.findByText('Распознаём операции…')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Отменить' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(aborted).toBe(true)
    expect(screen.queryByText('Не удалось разобрать выписку')).not.toBeInTheDocument()
  })
})
