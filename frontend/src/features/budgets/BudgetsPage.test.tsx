import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { afterEach, beforeEach, vi } from 'vitest'
import type { CreateBudgetRequest, MonthSummaryDto, UpdateBudgetRequest } from '../../api/types'
import { budgetStatus, cafe, categories, food, page, summary, transport } from '../../test/fixtures'
import { renderWithProviders } from '../../test/render'
import { API, server } from '../../test/server'
import { BudgetsPage } from './BudgetsPage'

interface Calls {
  summaryRequests: string[]
  created: CreateBudgetRequest[]
  updated: { id: string; body: UpdateBudgetRequest }[]
  deleted: string[]
}

function mockBackend(byMonth: Record<string, MonthSummaryDto>, createStatus = 200): Calls {
  const calls: Calls = { summaryRequests: [], created: [], updated: [], deleted: [] }
  server.use(
    http.get(`${API}/v1/categories/mine`, () => HttpResponse.json(page(categories))),
    http.get(`${API}/v1/analytics/summary`, ({ request }) => {
      const yearMonth = new URL(request.url).searchParams.get('yearMonth')!
      calls.summaryRequests.push(yearMonth)
      return HttpResponse.json(byMonth[yearMonth] ?? { yearMonth, totalSpent: 0, overall: null, categories: [] })
    }),
    http.post(`${API}/v1/budgets`, async ({ request }) => {
      calls.created.push((await request.json()) as CreateBudgetRequest)
      if (createStatus === 409) {
        return HttpResponse.json(
          { status: 409, message: 'dup', errorCode: 'RESOURCE_ALREADY_EXISTS', timestamp: 'x' },
          { status: 409 },
        )
      }
      return HttpResponse.json({ id: 50, limitAmount: 1, period: '2026-10-01', category: null })
    }),
    http.put(`${API}/v1/budgets/:id`, async ({ request, params }) => {
      calls.updated.push({ id: String(params.id), body: (await request.json()) as UpdateBudgetRequest })
      return HttpResponse.json({ id: Number(params.id), limitAmount: 1, period: '2026-10-01', category: null })
    }),
    http.delete(`${API}/v1/budgets/:id`, ({ params }) => {
      calls.deleted.push(String(params.id))
      return new HttpResponse(null, { status: 200 })
    }),
  )
  return calls
}

beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(2026, 9, 29, 12, 0))
})
afterEach(() => vi.useRealTimers())

describe('BudgetsPage', () => {
  it('shows the overall budget and a card for every category budget with the right status', async () => {
    const withOverspend = summary({
      categories: [
        budgetStatus(2, food, 15000, 8160),
        budgetStatus(3, cafe, 4000, 3900),
        budgetStatus(4, transport, 250, 299),
      ],
    })
    mockBackend({ '2026-10': withOverspend })
    renderWithProviders(<BudgetsPage />)

    expect(await screen.findByText('Общий бюджет на месяц')).toBeInTheDocument()
    expect(screen.getByText('Октябрь 2026')).toBeInTheDocument()
    expect(screen.getByText(/Осталось 6.840 ₽/)).toBeInTheDocument() // Еда: в норме
    expect(screen.getByText(/Осталось 100 ₽/)).toBeInTheDocument() // Кафе: на исходе (97,5%)
    expect(screen.getByText(/Перерасход 49 ₽/)).toBeInTheDocument() // Транспорт: перерасход
    expect(screen.getByText('120%')).toBeInTheDocument()
  })

  it('creates a category budget for the selected month (the overall option is hidden when it already exists)', async () => {
    const calls = mockBackend({ '2026-10': summary() })
    renderWithProviders(<BudgetsPage />)
    await screen.findByText('Общий бюджет на месяц')

    await userEvent.click(screen.getByRole('button', { name: /Новый бюджет/ }))
    const target = screen.getByLabelText('На что')
    const options = Array.from(target.querySelectorAll('option')).map((o) => o.textContent)
    expect(options).toEqual(['Транспорт']) // «Еда» и «Кафе» уже с бюджетами

    await userEvent.type(screen.getByLabelText('Лимит на месяц, ₽'), '8 000,5')
    await userEvent.click(screen.getByRole('button', { name: 'Создать' }))

    await waitFor(() => expect(calls.created).toHaveLength(1))
    expect(calls.created[0]).toEqual({ amount: 8000.5, period: '2026-10-01', categoryId: transport.id })
    expect(await screen.findByText('Бюджет создан')).toBeInTheDocument()
    expect(screen.queryByLabelText('На что')).not.toBeInTheDocument()
  })

  it('creates the overall budget when none is set', async () => {
    const calls = mockBackend({ '2026-10': summary({ overall: null }) })
    renderWithProviders(<BudgetsPage />)

    expect(await screen.findByText('Общий бюджет не задан')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Задать лимит' }))
    expect(screen.getByLabelText('На что')).toHaveValue('overall')

    await userEvent.type(screen.getByLabelText('Лимит на месяц, ₽'), '55000')
    await userEvent.click(screen.getByRole('button', { name: 'Создать' }))

    await waitFor(() => expect(calls.created).toEqual([{ amount: 55000, period: '2026-10-01', categoryId: null }]))
  })

  it('needs a positive limit before it lets you create', async () => {
    mockBackend({ '2026-10': summary() })
    renderWithProviders(<BudgetsPage />)
    await screen.findByText('Общий бюджет на месяц')
    await userEvent.click(screen.getByRole('button', { name: /Новый бюджет/ }))

    expect(screen.getByRole('button', { name: 'Создать' })).toBeDisabled()
    await userEvent.type(screen.getByLabelText('Лимит на месяц, ₽'), '0')
    expect(screen.getByRole('button', { name: 'Создать' })).toBeDisabled()
  })

  it('tells that a budget for this category and month already exists (409)', async () => {
    mockBackend({ '2026-10': summary() }, 409)
    renderWithProviders(<BudgetsPage />)
    await screen.findByText('Общий бюджет на месяц')
    await userEvent.click(screen.getByRole('button', { name: /Новый бюджет/ }))
    await userEvent.type(screen.getByLabelText('Лимит на месяц, ₽'), '1000')
    await userEvent.click(screen.getByRole('button', { name: 'Создать' }))

    expect(await screen.findByText('Такая запись уже есть')).toBeInTheDocument()
    expect(screen.getByLabelText('На что')).toBeInTheDocument()
  })

  it('edits the limit of a category budget', async () => {
    const calls = mockBackend({ '2026-10': summary() })
    renderWithProviders(<BudgetsPage />)
    await screen.findByText('Общий бюджет на месяц')

    await userEvent.click(screen.getByRole('button', { name: 'Изменить лимит «Еда»' }))
    const input = screen.getByLabelText('Новый лимит')
    expect(input).toHaveValue('15000')
    await userEvent.clear(input)
    await userEvent.type(input, '20000{Enter}')

    await waitFor(() => expect(calls.updated).toHaveLength(1))
    expect(calls.updated[0]).toEqual({ id: '2', body: { limitAmount: 20000, period: '2026-10-01' } })
    expect(await screen.findByText('Лимит обновлён')).toBeInTheDocument()
  })

  it('edits the overall limit', async () => {
    const calls = mockBackend({ '2026-10': summary() })
    renderWithProviders(<BudgetsPage />)
    await screen.findByText('Общий бюджет на месяц')

    await userEvent.click(screen.getByRole('button', { name: 'Изменить лимит' }))
    const input = screen.getByLabelText('Новый лимит')
    await userEvent.clear(input)
    await userEvent.type(input, '60000')
    await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }))

    await waitFor(() => expect(calls.updated).toEqual([{ id: '1', body: { limitAmount: 60000, period: '2026-10-01' } }]))
  })

  it('deletes a category budget', async () => {
    const calls = mockBackend({ '2026-10': summary() })
    renderWithProviders(<BudgetsPage />)
    await screen.findByText('Общий бюджет на месяц')

    await userEvent.click(screen.getByRole('button', { name: 'Удалить бюджет «Кафе»' }))

    await waitFor(() => expect(calls.deleted).toEqual(['3']))
    expect(await screen.findByText('Бюджет удалён')).toBeInTheDocument()
  })

  it('offers categories without a limit as quick-add chips', async () => {
    mockBackend({ '2026-10': summary() })
    renderWithProviders(<BudgetsPage />)
    await screen.findByText('Общий бюджет на месяц')

    expect(screen.getByText('Без лимита:')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: /Транспорт/ }))
    expect(screen.getByLabelText('На что')).toHaveValue(String(transport.id))
  })

  it('switches months and shows an empty state for a month without budgets', async () => {
    const calls = mockBackend({ '2026-10': summary() })
    renderWithProviders(<BudgetsPage />)
    await screen.findByText('Общий бюджет на месяц')

    await userEvent.click(screen.getByRole('button', { name: 'Следующий месяц' }))

    expect(await screen.findByText('Ноябрь 2026')).toBeInTheDocument()
    expect(await screen.findByText('На ноябрь бюджетов нет')).toBeInTheDocument()
    expect(calls.summaryRequests).toEqual(['2026-10', '2026-11'])
  })

  it('creates a budget for the month that is on the screen', async () => {
    const calls = mockBackend({})
    renderWithProviders(<BudgetsPage />)
    await screen.findByText('На октябрь бюджетов нет')
    await userEvent.click(screen.getByRole('button', { name: 'Предыдущий месяц' }))
    await screen.findByText('На сентябрь бюджетов нет')

    await userEvent.click(screen.getByRole('button', { name: /Новый бюджет/ }))
    await userEvent.type(screen.getByLabelText('Лимит на месяц, ₽'), '100')
    await userEvent.click(screen.getByRole('button', { name: 'Создать' }))

    await waitFor(() => expect(calls.created[0]).toMatchObject({ period: '2026-09-01' }))
  })
})
