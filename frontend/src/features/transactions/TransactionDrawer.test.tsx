import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import type { TransactionDto, TransactionRequest } from '../../api/types'
import { cafe, categories, food, page, transaction, transport } from '../../test/fixtures'
import { renderWithProviders } from '../../test/render'
import { API, server } from '../../test/server'
import { useTransactionDrawer } from './drawerContext'
import { TransactionDrawerProvider } from './TransactionDrawerProvider'

function Openers({ existing }: { existing?: TransactionDto }) {
  const { openNew, openEdit } = useTransactionDrawer()
  return (
    <>
      <button onClick={openNew}>новая</button>
      {existing && <button onClick={() => openEdit(existing)}>изменить</button>}
    </>
  )
}

function renderDrawer(existing?: TransactionDto) {
  return renderWithProviders(
    <TransactionDrawerProvider>
      <Openers existing={existing} />
    </TransactionDrawerProvider>,
  )
}

/** Регистрирует список категорий и ловит тела запросов на создание/изменение/удаление. */
function mockApi(options: { categoryList?: typeof categories } = {}) {
  const calls: { created: TransactionRequest[]; updated: { id: string; body: TransactionRequest }[]; deleted: string[] } = {
    created: [],
    updated: [],
    deleted: [],
  }
  server.use(
    http.get(`${API}/v1/categories/mine`, () => HttpResponse.json(page(options.categoryList ?? categories))),
    http.post(`${API}/v1/transactions`, async ({ request }) => {
      const body = (await request.json()) as TransactionRequest
      calls.created.push(body)
      return HttpResponse.json(transaction(100, body.description, body.dateTime, []))
    }),
    http.put(`${API}/v1/transactions/:id`, async ({ request, params }) => {
      const body = (await request.json()) as TransactionRequest
      calls.updated.push({ id: String(params.id), body })
      return HttpResponse.json(transaction(Number(params.id), body.description, body.dateTime, []))
    }),
    http.delete(`${API}/v1/transactions/:id`, ({ params }) => {
      calls.deleted.push(String(params.id))
      return new HttpResponse(null, { status: 200 })
    }),
  )
  return calls
}

async function openNew() {
  await userEvent.click(screen.getByText('новая'))
  return screen.findByRole('dialog')
}

const amountInput = () => screen.getByLabelText('Сумма')
const save = () => screen.getByRole('button', { name: 'Сохранить' })

describe('TransactionDrawer — new expense', () => {
  it('creates a single-category expense; commas and dots both work', async () => {
    const calls = mockApi()
    renderDrawer()
    const dialog = await openNew()
    await within(dialog).findByRole('combobox', { name: 'Категория' })

    expect(save()).toBeDisabled()
    await userEvent.type(amountInput(), '1500.5')
    expect(amountInput()).toHaveValue('1500,5')
    expect(screen.getByText('Добавьте описание траты')).toBeInTheDocument()

    await userEvent.type(screen.getByPlaceholderText('Например, Супермаркет'), '  Ужин ')
    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Категория' }), 'Кафе')
    expect(screen.getByText('Вся сумма — в категории «Кафе»')).toBeInTheDocument()
    expect(save()).toBeEnabled()

    await userEvent.click(save())

    await waitFor(() => expect(calls.created).toHaveLength(1))
    expect(calls.created[0]).toEqual({
      description: 'Ужин',
      amount: 1500.5,
      dateTime: expect.stringMatching(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:00$/),
      splits: [{ categoryId: cafe.id, amount: 1500.5 }],
    })
    expect(await screen.findByText('Трата добавлена')).toBeInTheDocument()
    expect(screen.getByText(/Ужин · 1.500,5/)).toBeInTheDocument()
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  })

  it('splits an expense between two categories and refuses to save until the sum matches', async () => {
    const calls = mockApi()
    renderDrawer()
    const dialog = await openNew()
    await within(dialog).findByRole('combobox', { name: 'Категория' })

    await userEvent.type(amountInput(), '1000')
    await userEvent.type(screen.getByPlaceholderText('Например, Супермаркет'), 'Магазин')
    await userEvent.click(screen.getByRole('button', { name: /Разделить на ещё одну категорию/ }))

    // Первая часть получила всю сумму, вторая — следующую свободную категорию без суммы
    const part1 = screen.getByRole('textbox', { name: 'Сумма части 1' })
    const part2 = screen.getByRole('textbox', { name: 'Сумма части 2' })
    expect(part1).toHaveValue('1000')
    expect(screen.getByRole('combobox', { name: 'Категория части 2' })).toHaveValue(String(transport.id))
    expect(screen.getByText('Укажите сумму для каждой части')).toBeInTheDocument()
    expect(save()).toBeDisabled()

    await userEvent.clear(part1)
    await userEvent.type(part1, '700')
    expect(screen.getByText(/Укажите суммы частей · осталось 300/)).toBeInTheDocument()

    await userEvent.type(part2, '400')
    expect(screen.getByText(/Части больше суммы на 100/)).toBeInTheDocument()
    expect(save()).toBeDisabled()

    await userEvent.clear(part2)
    await userEvent.type(part2, '300')
    expect(screen.getByText('Сумма частей совпадает с суммой траты')).toBeInTheDocument()

    await userEvent.click(save())
    await waitFor(() => expect(calls.created).toHaveLength(1))
    expect(calls.created[0]!.splits).toEqual([
      { categoryId: food.id, amount: 700 },
      { categoryId: transport.id, amount: 300 },
    ])
    expect(calls.created[0]!.amount).toBe(1000)
  })

  it('"Поровну" divides the sum and gives the extra kopecks to the last part', async () => {
    mockApi()
    renderDrawer()
    const dialog = await openNew()
    await within(dialog).findByRole('combobox', { name: 'Категория' })

    await userEvent.type(amountInput(), '10')
    await userEvent.click(screen.getByRole('button', { name: /Разделить на ещё одну категорию/ }))
    await userEvent.click(screen.getByRole('button', { name: /Разделить на ещё одну категорию/ }))
    await userEvent.click(screen.getByRole('button', { name: /Поровну/ }))

    expect(screen.getByRole('textbox', { name: 'Сумма части 1' })).toHaveValue('3,33')
    expect(screen.getByRole('textbox', { name: 'Сумма части 2' })).toHaveValue('3,33')
    expect(screen.getByRole('textbox', { name: 'Сумма части 3' })).toHaveValue('3,34')
  })

  it('"Добавить остаток" puts the remainder into the empty part', async () => {
    mockApi()
    renderDrawer()
    const dialog = await openNew()
    await within(dialog).findByRole('combobox', { name: 'Категория' })

    await userEvent.type(amountInput(), '1000')
    await userEvent.type(screen.getByPlaceholderText('Например, Супермаркет'), 'Магазин')
    await userEvent.click(screen.getByRole('button', { name: /Разделить на ещё одну категорию/ }))
    const part1 = screen.getByRole('textbox', { name: 'Сумма части 1' })
    await userEvent.clear(part1)
    await userEvent.type(part1, '600')

    await userEvent.click(screen.getByRole('button', { name: 'Добавить остаток' }))

    expect(screen.getByRole('textbox', { name: 'Сумма части 2' })).toHaveValue('400')
    expect(screen.getByText('Сумма частей совпадает с суммой траты')).toBeInTheDocument()
  })

  it('removing the second part returns to the single-category mode', async () => {
    mockApi()
    renderDrawer()
    const dialog = await openNew()
    await within(dialog).findByRole('combobox', { name: 'Категория' })

    await userEvent.type(amountInput(), '500')
    await userEvent.click(screen.getByRole('button', { name: /Разделить на ещё одну категорию/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Убрать часть 2' }))

    expect(screen.queryByRole('textbox', { name: /Сумма части/ })).not.toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: 'Категория' })).toBeInTheDocument()
  })

  it('does not let the same category be picked twice', async () => {
    mockApi()
    renderDrawer()
    const dialog = await openNew()
    await within(dialog).findByRole('combobox', { name: 'Категория' })

    await userEvent.type(amountInput(), '500')
    await userEvent.click(screen.getByRole('button', { name: /Разделить на ещё одну категорию/ }))

    const second = screen.getByRole('combobox', { name: 'Категория части 2' })
    expect(within(second).getByRole('option', { name: 'Еда' })).toBeDisabled()
    expect(within(second).getByRole('option', { name: 'Кафе' })).toBeEnabled()
  })

  it('keeps the drawer open and explains the failure when the server rejects the expense', async () => {
    mockApi()
    server.use(
      http.post(`${API}/v1/transactions`, () =>
        HttpResponse.json(
          { status: 400, message: 'bad', errorCode: 'NOT_VALID_TRANSACTION_SPLIT_SUM', timestamp: 'x' },
          { status: 400 },
        ),
      ),
    )
    renderDrawer()
    const dialog = await openNew()
    await within(dialog).findByRole('combobox', { name: 'Категория' })

    await userEvent.type(amountInput(), '100')
    await userEvent.type(screen.getByPlaceholderText('Например, Супермаркет'), 'Кофе')
    await userEvent.click(save())

    expect(await screen.findByText('Не удалось сохранить')).toBeInTheDocument()
    expect(screen.getByText('Сумма частей должна совпадать с суммой траты')).toBeInTheDocument()
    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(amountInput()).toHaveValue('100')
  })

  it('asks to create a category first when there are none', async () => {
    mockApi({ categoryList: [] })
    renderDrawer()
    await openNew()

    expect(await screen.findByText('Сначала создайте категорию')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Перейти к категориям' })).toHaveAttribute('href', '/categories')
  })
})

describe('TransactionDrawer — editing', () => {
  const existing = transaction(7, 'Ужин в ресторане', '2026-10-27T20:40:00', [
    [cafe, 1800],
    [food, 800],
  ])

  it('opens with the saved values and saves changes with PUT', async () => {
    const calls = mockApi()
    renderDrawer(existing)
    await userEvent.click(screen.getByText('изменить'))
    const dialog = await screen.findByRole('dialog', { name: 'Редактирование траты' })
    await within(dialog).findByRole('textbox', { name: 'Сумма части 1' })

    expect(amountInput()).toHaveValue('2600')
    expect(screen.getByPlaceholderText('Например, Супермаркет')).toHaveValue('Ужин в ресторане')
    expect(screen.getByDisplayValue('2026-10-27')).toBeInTheDocument()
    expect(screen.getByDisplayValue('20:40')).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'Сумма части 1' })).toHaveValue('1800')
    expect(screen.getByRole('textbox', { name: 'Сумма части 2' })).toHaveValue('800')
    expect(screen.getByText('Сумма частей совпадает с суммой траты')).toBeInTheDocument()

    const description = screen.getByPlaceholderText('Например, Супермаркет')
    await userEvent.clear(description)
    await userEvent.type(description, 'Ужин с коллегами')
    await userEvent.click(save())

    await waitFor(() => expect(calls.updated).toHaveLength(1))
    expect(calls.updated[0]).toEqual({
      id: '7',
      body: {
        description: 'Ужин с коллегами',
        amount: 2600,
        dateTime: '2026-10-27T20:40:00',
        splits: [
          { categoryId: cafe.id, amount: 1800 },
          { categoryId: food.id, amount: 800 },
        ],
      },
    })
    expect(await screen.findByText('Изменения сохранены')).toBeInTheDocument()
  })

  it('deletes only after a second confirming click', async () => {
    const calls = mockApi()
    renderDrawer(existing)
    await userEvent.click(screen.getByText('изменить'))
    await screen.findByRole('textbox', { name: 'Сумма части 1' })

    await userEvent.click(screen.getByRole('button', { name: 'Удалить' }))
    expect(calls.deleted).toEqual([])
    expect(screen.getByRole('button', { name: 'Точно удалить?' })).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Точно удалить?' }))
    await waitFor(() => expect(calls.deleted).toEqual(['7']))
    expect(await screen.findByText('Трата удалена')).toBeInTheDocument()
  })

  it('the new-expense form has no delete button', async () => {
    mockApi()
    renderDrawer()
    const dialog = await openNew()
    await within(dialog).findByRole('combobox', { name: 'Категория' })
    expect(screen.queryByRole('button', { name: 'Удалить' })).not.toBeInTheDocument()
  })
})
