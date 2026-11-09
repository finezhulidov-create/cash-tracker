import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { afterEach, beforeEach, vi } from 'vitest'
import type { MonthSummaryDto } from '../../api/types'
import {
  budgetStatus,
  cafe,
  categories,
  food,
  page,
  summary,
  transaction,
  transport,
  user,
} from '../../test/fixtures'
import { renderWithProviders } from '../../test/render'
import { API, server } from '../../test/server'
import { TransactionDrawerProvider } from '../transactions/TransactionDrawerProvider'
import { DashboardPage } from './DashboardPage'

function mockBackend(monthSummary: MonthSummaryDto = summary()) {
  server.use(
    http.get(`${API}/v1/users/me`, () => HttpResponse.json(user)),
    http.get(`${API}/v1/categories/mine`, () => HttpResponse.json(page(categories))),
    http.get(`${API}/v1/analytics/summary`, () => HttpResponse.json(monthSummary)),
    http.get(`${API}/v1/analytics/by-category`, () =>
      HttpResponse.json([
        { categoryId: food.id, categoryName: 'Еда', spent: 8000 },
        { categoryId: cafe.id, categoryName: 'Кафе', spent: 2000 },
        { categoryId: transport.id, categoryName: 'Транспорт', spent: 0 },
      ]),
    ),
    http.get(`${API}/v1/analytics/monthly`, () =>
      HttpResponse.json(
        Array.from({ length: 12 }, (_, i) => ({
          month: `2026-${String(i + 1).padStart(2, '0')}`,
          spent: i === 8 ? 30000 : i === 9 ? 10000 : 0,
        })),
      ),
    ),
    http.get(`${API}/v1/transactions`, () =>
      HttpResponse.json(
        page([
          transaction(1, 'Супермаркет «Лента»', '2026-10-28T18:20:00', [[food, 3240]]),
          transaction(2, 'Ужин', '2026-10-27T20:40:00', [[cafe, 1800], [food, 800]]),
        ]),
      ),
    ),
  )
}

function renderDashboard() {
  return renderWithProviders(
    <TransactionDrawerProvider>
      <DashboardPage />
    </TransactionDrawerProvider>,
  )
}

beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(2026, 9, 29, 12, 0)) // 3 дня до конца месяца, считая сегодня
})
afterEach(() => vi.useRealTimers())

describe('DashboardPage', () => {
  it('shows spending, the overall limit, what is left and the daily allowance', async () => {
    mockBackend()
    renderDashboard()

    expect(await screen.findByText('Потрачено в октябре')).toBeInTheDocument()
    expect((await screen.findAllByText(/29.809 ₽/)).length).toBeGreaterThan(0)
    expect(await screen.findByText(/из 55.000 ₽ общего бюджета/)).toBeInTheDocument()
    expect(await screen.findByText(/Осталось 25.191 ₽/)).toBeInTheDocument()
    expect(screen.getByText(/54% бюджета/)).toBeInTheDocument()
    expect(screen.getByText(/8.397 ₽ в день · ещё 3 дня/)).toBeInTheDocument()
    expect(await screen.findByText(/152.400 ₽/)).toBeInTheDocument()
  })

  it('without an overall budget asks to set one instead of showing a bar', async () => {
    mockBackend(summary({ overall: null }))
    renderDashboard()

    expect(await screen.findByText('Общий бюджет на месяц не задан')).toBeInTheDocument()
    const link = screen.getByRole('link', { name: /Задайте общий бюджет/ })
    expect(link).toHaveAttribute('href', '/budgets')
    expect(screen.queryByRole('progressbar', { name: 'Расход общего бюджета' })).not.toBeInTheDocument()
  })

  it('reports an overspend of the overall budget', async () => {
    mockBackend(summary({ totalSpent: 60000, overall: budgetStatus(1, null, 55000, 60000) }))
    renderDashboard()
    expect(await screen.findByText(/Перерасход 5.000 ₽/)).toBeInTheDocument()
  })

  it('splits the month by category, skipping categories without spending', async () => {
    mockBackend()
    renderDashboard()

    expect(await screen.findByText('2 категории')).toBeInTheDocument()
    expect(screen.getByText('80%')).toBeInTheDocument()
    expect(screen.getByText('20%')).toBeInTheDocument()
  })

  it('lists category budgets with their status and links to the full list', async () => {
    mockBackend(
      summary({
        categories: [budgetStatus(2, food, 15000, 8160), budgetStatus(3, cafe, 250, 299)],
      }),
    )
    renderDashboard()

    expect(await screen.findByText(/Осталось 6.840 ₽/)).toBeInTheDocument()
    expect(screen.getByText(/Перерасход 49 ₽/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Все бюджеты' })).toHaveAttribute('href', '/budgets')
  })

  it('shows recent expenses and opens one for editing', async () => {
    mockBackend()
    renderDashboard()

    const recent = (await screen.findByRole('heading', { name: 'Последние операции' })).closest('[class*="card"]') as HTMLElement
    expect(await within(recent).findByText('Супермаркет «Лента»')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Все операции' })).toHaveAttribute('href', '/transactions')

    await userEvent.click(within(recent).getByText('Ужин'))
    expect(await screen.findByRole('dialog', { name: 'Редактирование траты' })).toBeInTheDocument()
  })

  it('draws the year with the average over months that had spending', async () => {
    mockBackend()
    renderDashboard()

    expect(await screen.findByText('Динамика за 2026')).toBeInTheDocument()
    expect(await screen.findByText(/В среднем 20.000 ₽ в месяц/)).toBeInTheDocument()
    expect(screen.getByTitle(/Сентябрь 2026: 30.000 ₽/)).toBeInTheDocument()
  })

  it('shows a retry when the month summary cannot be loaded', async () => {
    mockBackend()
    server.use(http.get(`${API}/v1/analytics/summary`, () => HttpResponse.error()))
    renderDashboard()

    expect((await screen.findAllByText('Не удалось загрузить данные')).length).toBeGreaterThan(0)
  })
})
