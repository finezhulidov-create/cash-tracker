import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { afterEach, beforeEach, vi } from 'vitest'
import { renderWithProviders } from '../../test/render'
import { API, server } from '../../test/server'
import { AnalyticsPage } from './AnalyticsPage'

function mockBackend(spending = [
  { categoryId: 1, categoryName: 'Еда', spent: 8000 },
  { categoryId: 4, categoryName: 'Кафе', spent: 2000 },
]) {
  const ranges: { from: string; to: string }[] = []
  const years: string[] = []
  server.use(
    http.get(`${API}/v1/analytics/by-category`, ({ request }) => {
      const params = new URL(request.url).searchParams
      ranges.push({ from: params.get('from')!, to: params.get('to')! })
      return HttpResponse.json(spending)
    }),
    http.get(`${API}/v1/analytics/monthly`, ({ request }) => {
      const year = new URL(request.url).searchParams.get('year')!
      years.push(year)
      return HttpResponse.json(
        Array.from({ length: 12 }, (_, i) => ({
          month: `${year}-${String(i + 1).padStart(2, '0')}`,
          spent: i === 2 ? 40000 : i === 5 ? 20000 : 0,
        })),
      )
    }),
  )
  return { ranges, years }
}

beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(2026, 9, 29, 12, 0))
})
afterEach(() => vi.useRealTimers())

describe('AnalyticsPage — by category', () => {
  it('starts with the current month and shows the total, shares and bars', async () => {
    const { ranges } = mockBackend()
    renderWithProviders(<AnalyticsPage />)

    expect(await screen.findByText('1 — 31 октября 2026')).toBeInTheDocument()
    expect(await screen.findByText(/10.000 ₽/)).toBeInTheDocument()
    expect(screen.getByText('80%')).toBeInTheDocument()
    expect(screen.getByText('20%')).toBeInTheDocument()
    expect(ranges[0]).toEqual({ from: '2026-10-01', to: '2026-10-31' })
    expect(screen.getByRole('progressbar', { name: 'Доля «Еда»' })).toHaveAttribute('aria-valuenow', '100')
    expect(screen.getByRole('progressbar', { name: 'Доля «Кафе»' })).toHaveAttribute('aria-valuenow', '25')
  })

  it('switches the period: three months and the whole year', async () => {
    const { ranges } = mockBackend()
    renderWithProviders(<AnalyticsPage />)
    await screen.findByText('1 — 31 октября 2026')

    await userEvent.click(screen.getByText('3 месяца'))
    expect(await screen.findByText('1 августа — 31 октября 2026')).toBeInTheDocument()

    await userEvent.click(screen.getByText('Год'))
    expect(await screen.findByText('1 января — 31 декабря 2026')).toBeInTheDocument()

    expect(ranges.map((r) => `${r.from}..${r.to}`)).toEqual([
      '2026-10-01..2026-10-31',
      '2026-08-01..2026-10-31',
      '2026-01-01..2026-12-31',
    ])
  })

  it('says so when there is no spending in the period', async () => {
    mockBackend([])
    renderWithProviders(<AnalyticsPage />)
    expect(await screen.findByText('За этот период трат нет')).toBeInTheDocument()
  })

  it('ignores categories without spending', async () => {
    mockBackend([
      { categoryId: 1, categoryName: 'Еда', spent: 500 },
      { categoryId: 2, categoryName: 'Транспорт', spent: 0 },
    ])
    renderWithProviders(<AnalyticsPage />)
    expect(await screen.findByText('Еда')).toBeInTheDocument()
    expect(screen.queryByText('Транспорт')).not.toBeInTheDocument()
  })
})

describe('AnalyticsPage — by month', () => {
  it('shows the year total, the monthly average and the most expensive month', async () => {
    mockBackend()
    renderWithProviders(<AnalyticsPage />)

    expect(await screen.findByText('За год')).toBeInTheDocument()
    expect(await screen.findByText(/60.000 ₽/)).toBeInTheDocument()
    expect(screen.getByText(/30.000 ₽/)).toBeInTheDocument() // среднее по двум месяцам с тратами
    expect(screen.getByText('Март · 40к ₽')).toBeInTheDocument()
    expect(screen.getByTitle(/Март 2026: 40.000 ₽/)).toBeInTheDocument()
  })

  it('goes back a year but not into the future', async () => {
    const { years } = mockBackend()
    renderWithProviders(<AnalyticsPage />)
    await screen.findByText('За год')

    expect(screen.getByRole('button', { name: 'Следующий год' })).toBeDisabled()
    await userEvent.click(screen.getByRole('button', { name: 'Предыдущий год' }))

    await waitFor(() => expect(years).toEqual(['2026', '2025']))
    expect(await screen.findByText('2025')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Следующий год' })).toBeEnabled()
  })
})
