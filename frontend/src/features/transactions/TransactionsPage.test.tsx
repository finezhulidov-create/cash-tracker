import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { useLocation } from 'react-router-dom'
import { afterEach, beforeEach, vi } from 'vitest'
import type { TransactionDto } from '../../api/types'
import { cafe, categories, food, page, transaction, transport } from '../../test/fixtures'
import { renderWithProviders } from '../../test/render'
import { API, server } from '../../test/server'
import { TransactionDrawerProvider } from './TransactionDrawerProvider'
import { TransactionsPage } from './TransactionsPage'

const dinner = transaction(1, 'Ужин в ресторане', '2026-10-28T20:40:00', [[cafe, 1800], [food, 800]])
const metro = transaction(2, 'Метро и такси', '2026-10-28T09:10:00', [[transport, 860]])
const rent = transaction(3, 'Коммунальные услуги', '2026-10-24T12:00:00', [[food, 11800]])

/** Список трат отдаётся страницами; все запросы к нему запоминаются. */
function mockTransactions(pages: TransactionDto[][], totalElements?: number) {
  const requests: URLSearchParams[] = []
  server.use(
    http.get(`${API}/v1/categories/mine`, () => HttpResponse.json(page(categories))),
    http.get(`${API}/v1/transactions`, ({ request }) => {
      const params = new URL(request.url).searchParams
      requests.push(params)
      const number = Number(params.get('page') ?? 0)
      const content = pages[number] ?? []
      return HttpResponse.json(
        page(content, number, pages.length, totalElements ?? pages.reduce((n, p) => n + p.length, 0)),
      )
    }),
  )
  return requests
}

function LocationProbe() {
  const { search } = useLocation()
  return <output data-testid="search">{search}</output>
}

function renderPage(route = '/transactions') {
  return renderWithProviders(
    <TransactionDrawerProvider>
      <TransactionsPage />
      <LocationProbe />
    </TransactionDrawerProvider>,
    { route },
  )
}

const lastRequest = (requests: URLSearchParams[]) => requests[requests.length - 1]!

beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(2026, 9, 29, 12, 0))
})
afterEach(() => vi.useRealTimers())

describe('TransactionsPage', () => {
  it('groups expenses by day with a heading and the day total', async () => {
    mockTransactions([[dinner, metro, rent]])
    renderPage()

    expect(await screen.findByText('Вчера, 28 октября')).toBeInTheDocument()
    expect(screen.getByText('24 октября, суббота')).toBeInTheDocument()
    expect(screen.getByText(/3 операции/)).toBeInTheDocument()
    expect(screen.getByText(/3.460 ₽/)).toBeInTheDocument() // итог дня: 2600 + 860
    expect(screen.getByText(/Итого/)).toBeInTheDocument()
    expect(screen.getByText('2 части')).toBeInTheDocument()
  })

  it('asks the server for the newest first, 20 per page', async () => {
    const requests = mockTransactions([[dinner]])
    renderPage()
    await screen.findByText('Ужин в ресторане')

    expect(requests[0]!.get('sort')).toBe('dateTime,desc')
    expect(requests[0]!.get('size')).toBe('20')
    expect(requests[0]!.get('page')).toBe('0')
    expect(requests[0]!.has('q')).toBe(false)
  })

  it('searches by description with a pause after typing', async () => {
    const requests = mockTransactions([[dinner, metro]])
    renderPage()
    await screen.findByText('Ужин в ресторане')

    await userEvent.type(screen.getByLabelText('Поиск по описанию'), ' ужин ')

    await waitFor(() => expect(lastRequest(requests).get('q')).toBe('ужин'))
    // не по запросу на каждую букву
    expect(requests.filter((r) => r.has('q')).length).toBeLessThanOrEqual(2)
  })

  it('applies category, amount and date filters to the request, ending the day at 23:59:59.999999', async () => {
    const requests = mockTransactions([[dinner]])
    renderPage()
    await screen.findByText('Ужин в ресторане')

    await userEvent.click(screen.getByRole('button', { name: /Фильтры/ }))
    await userEvent.selectOptions(screen.getByLabelText('Категория'), 'Еда')
    await userEvent.type(screen.getByLabelText('Сумма от, ₽'), '100,5')
    await userEvent.type(screen.getByLabelText('Сумма до, ₽'), '5000')
    await userEvent.type(screen.getByLabelText('Дата с'), '2026-10-01')
    await userEvent.type(screen.getByLabelText('Дата по'), '2026-10-31')

    await waitFor(() => expect(lastRequest(requests).get('maxAmount')).toBe('5000'))
    const params = lastRequest(requests)
    expect(params.get('categoryId')).toBe(String(food.id))
    expect(params.get('minAmount')).toBe('100.5')
    expect(params.get('from')).toBe('2026-10-01T00:00:00')
    expect(params.get('to')).toBe('2026-10-31T23:59:59.999999')
    expect(screen.getByRole('button', { name: /Фильтры/ })).toHaveTextContent('5')
  })

  it('keeps the applied filters in the URL so the view can be shared and survives a reload', async () => {
    mockTransactions([[dinner]])
    renderPage()
    await screen.findByText('Ужин в ресторане')

    await userEvent.type(screen.getByLabelText('Поиск по описанию'), 'ужин')
    await userEvent.click(screen.getByRole('button', { name: /Фильтры/ }))
    await userEvent.selectOptions(screen.getByLabelText('Категория'), 'Кафе')

    await waitFor(() => expect(screen.getByTestId('search')).toHaveTextContent(`?q=%D1%83%D0%B6%D0%B8%D0%BD&cat=${cafe.id}`))
  })

  it('starts from the filters in the link', async () => {
    const requests = mockTransactions([[dinner]])
    renderPage('/transactions?cat=1&min=100')
    await screen.findByText('Ужин в ресторане')

    expect(requests[0]!.get('categoryId')).toBe('1')
    expect(requests[0]!.get('minAmount')).toBe('100')
    expect(screen.getByLabelText('Категория')).toHaveValue('1')
    expect(screen.getByRole('button', { name: /Фильтры/ })).toHaveTextContent('2')
  })

  it('does not call the server for contradictory dates and says why', async () => {
    const requests = mockTransactions([[dinner]])
    renderPage('/transactions?from=2026-10-31&to=2026-10-01')

    expect(await screen.findByText('Дата «с» не может быть позже даты «по»')).toBeInTheDocument()
    expect(requests).toHaveLength(0)
    expect(screen.getByText('Ничего не найдено')).toBeInTheDocument()
  })

  it('loads the next page with "Показать ещё" and only then calls the sum "Итого"', async () => {
    const requests = mockTransactions([[dinner, metro], [rent]], 3)
    renderPage()
    await screen.findByText('Ужин в ресторане')

    expect(screen.getByText(/Загружено на/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Показать ещё' }))

    expect(await screen.findByText('Коммунальные услуги')).toBeInTheDocument()
    expect(requests.map((r) => r.get('page'))).toEqual(['0', '1'])
    expect(screen.queryByRole('button', { name: 'Показать ещё' })).not.toBeInTheDocument()
    expect(screen.queryByText(/Загружено на/)).not.toBeInTheDocument()
    expect(screen.getByText(/Итого/)).toBeInTheDocument()
  })

  it('explains an empty result and resets everything', async () => {
    const requests = mockTransactions([[]])
    renderPage('/transactions?q=нет-такого&cat=1')

    expect(await screen.findByText('Ничего не найдено')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Сбросить всё' }))

    await waitFor(() => expect(lastRequest(requests).has('q')).toBe(false))
    expect(lastRequest(requests).has('categoryId')).toBe(false)
    expect(screen.getByLabelText('Поиск по описанию')).toHaveValue('')
  })

  it('invites to add the first expense when there are none at all', async () => {
    mockTransactions([[]])
    renderPage()

    expect(await screen.findByText('Трат пока нет')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Добавить трату' })).toBeInTheDocument()
  })

  it('opens an expense for editing on click', async () => {
    mockTransactions([[metro]])
    renderPage()

    await userEvent.click(await screen.findByText('Метро и такси'))

    expect(await screen.findByRole('dialog', { name: 'Редактирование траты' })).toBeInTheDocument()
    expect(await screen.findByDisplayValue('Метро и такси')).toBeInTheDocument()
  })

  it('offers a retry when the list cannot be loaded', async () => {
    server.use(
      http.get(`${API}/v1/categories/mine`, () => HttpResponse.json(page(categories))),
      http.get(`${API}/v1/transactions`, () => HttpResponse.error()),
    )
    renderPage()

    expect(await screen.findByText('Не удалось загрузить данные')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Повторить' })).toBeInTheDocument()
  })
})
