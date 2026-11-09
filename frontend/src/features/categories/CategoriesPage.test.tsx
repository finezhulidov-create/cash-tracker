import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { afterEach, beforeEach, vi } from 'vitest'
import { categories, food, page } from '../../test/fixtures'
import { renderWithProviders } from '../../test/render'
import { API, server } from '../../test/server'
import { CategoriesPage } from './CategoriesPage'

interface Backend {
  /** Сколько частей трат у каждой категории (по id) */
  parts: Record<number, number>
  created: string[]
  renamed: { id: string; name: string }[]
  deleted: string[]
  /** Если задано, DELETE отвечает 409 CATEGORY_IN_USE */
  deleteConflict?: boolean
}

function mockBackend(parts: Record<number, number>, options: Partial<Backend> = {}): Backend {
  const state: Backend = { parts, created: [], renamed: [], deleted: [], ...options }
  server.use(
    http.get(`${API}/v1/categories/mine`, () => HttpResponse.json(page(categories))),
    http.get(`${API}/v1/categories/:id/splits`, ({ params }) =>
      HttpResponse.json(page([], 0, 1, state.parts[Number(params.id)] ?? 0)),
    ),
    http.get(`${API}/v1/analytics/by-category`, () =>
      HttpResponse.json([{ categoryId: 1, categoryName: 'Еда', spent: 8160 }]),
    ),
    http.post(`${API}/v1/categories`, async ({ request }) => {
      const body = (await request.json()) as { categoryName: string }
      state.created.push(body.categoryName)
      return HttpResponse.json({ id: 99, categoryName: body.categoryName, userId: 1 })
    }),
    http.put(`${API}/v1/categories/:id`, async ({ request, params }) => {
      const body = (await request.json()) as { categoryName: string }
      state.renamed.push({ id: String(params.id), name: body.categoryName })
      return HttpResponse.json({ id: Number(params.id), categoryName: body.categoryName, userId: 1 })
    }),
    http.delete(`${API}/v1/categories/:id`, ({ params }) => {
      state.deleted.push(String(params.id))
      if (state.deleteConflict) {
        return HttpResponse.json(
          { status: 409, message: 'in use', errorCode: 'CATEGORY_IN_USE', timestamp: 'x' },
          { status: 409 },
        )
      }
      return new HttpResponse(null, { status: 200 })
    }),
  )
  return state
}

beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(2026, 9, 29, 12, 0))
})
afterEach(() => vi.useRealTimers())

describe('CategoriesPage', () => {
  it('lists categories with the number of expense parts and this month spending', async () => {
    mockBackend({ 1: 5, 2: 1 })
    renderWithProviders(<CategoriesPage />)

    expect(await screen.findByText(/5 частей трат · 8.160 ₽ в октябре/)).toBeInTheDocument()
    expect(await screen.findByText(/1 часть трат/)).toBeInTheDocument()
    expect(await screen.findByText('Пока без трат')).toBeInTheDocument()
  })

  it('adds a category and clears the field', async () => {
    const backend = mockBackend({})
    renderWithProviders(<CategoriesPage />)
    const input = await screen.findByLabelText('Название новой категории')

    expect(screen.getByRole('button', { name: 'Добавить' })).toBeDisabled()
    await userEvent.type(input, '  Подарки ')
    await userEvent.click(screen.getByRole('button', { name: 'Добавить' }))

    await waitFor(() => expect(backend.created).toEqual(['Подарки']))
    expect(await screen.findByText('Категория добавлена')).toBeInTheDocument()
    expect(input).toHaveValue('')
  })

  it('refuses a duplicate name on the client, ignoring case', async () => {
    const backend = mockBackend({})
    renderWithProviders(<CategoriesPage />)
    await userEvent.type(await screen.findByLabelText('Название новой категории'), 'еДа')
    await userEvent.click(screen.getByRole('button', { name: 'Добавить' }))

    expect(await screen.findByText('Категория «еДа» уже есть')).toBeInTheDocument()
    expect(backend.created).toEqual([])
  })

  it('renames inline: Enter saves, Escape cancels', async () => {
    const backend = mockBackend({})
    renderWithProviders(<CategoriesPage />)
    await screen.findByText('Транспорт')

    await userEvent.click(screen.getByRole('button', { name: 'Переименовать «Еда»' }))
    const input = screen.getByLabelText('Новое название категории «Еда»')
    await userEvent.clear(input)
    await userEvent.type(input, 'Продукты{Enter}')

    await waitFor(() => expect(backend.renamed).toEqual([{ id: '1', name: 'Продукты' }]))

    await userEvent.click(screen.getByRole('button', { name: 'Переименовать «Транспорт»' }))
    await userEvent.type(screen.getByLabelText('Новое название категории «Транспорт»'), 'X{Escape}')
    expect(screen.queryByLabelText(/Новое название категории/)).not.toBeInTheDocument()
    expect(backend.renamed).toHaveLength(1)
  })

  it('refuses to rename to an existing name', async () => {
    const backend = mockBackend({})
    renderWithProviders(<CategoriesPage />)
    await screen.findByText('Транспорт')

    await userEvent.click(screen.getByRole('button', { name: 'Переименовать «Транспорт»' }))
    const input = screen.getByLabelText('Новое название категории «Транспорт»')
    await userEvent.clear(input)
    await userEvent.type(input, 'Еда{Enter}')

    expect(await screen.findByText('Категория «Еда» уже есть')).toBeInTheDocument()
    expect(backend.renamed).toEqual([])
  })

  it('does not delete a category that has expenses and explains why', async () => {
    const backend = mockBackend({ 1: 5 })
    renderWithProviders(<CategoriesPage />)
    await screen.findByText(/5 частей трат/)

    await userEvent.click(screen.getByRole('button', { name: 'Удалить «Еда»' }))

    expect(await screen.findByText('Нельзя удалить «Еда»')).toBeInTheDocument()
    expect(screen.getByText(/К категории привязано 5 частей трат/)).toBeInTheDocument()
    expect(backend.deleted).toEqual([])
  })

  it('deletes an unused category', async () => {
    const backend = mockBackend({})
    renderWithProviders(<CategoriesPage />)
    await screen.findAllByText('Пока без трат')

    await userEvent.click(screen.getByRole('button', { name: 'Удалить «Кафе»' }))

    await waitFor(() => expect(backend.deleted).toEqual(['4']))
    expect(await screen.findByText('Категория удалена')).toBeInTheDocument()
  })

  it('shows the warning when the server says the category is in use although the counter said zero', async () => {
    const backend = mockBackend({}, { deleteConflict: true })
    renderWithProviders(<CategoriesPage />)
    await screen.findAllByText('Пока без трат')

    await userEvent.click(screen.getByRole('button', { name: 'Удалить «Еда»' }))

    expect(await screen.findByText('Нельзя удалить «Еда»')).toBeInTheDocument()
    expect(backend.deleted).toEqual(['1'])
  })

  it('has a way to dismiss the warning', async () => {
    mockBackend({ [food.id]: 2 })
    renderWithProviders(<CategoriesPage />)
    await screen.findByText(/2 части трат/)
    await userEvent.click(screen.getByRole('button', { name: 'Удалить «Еда»' }))
    await screen.findByText('Нельзя удалить «Еда»')
    await userEvent.click(screen.getByRole('button', { name: 'Скрыть сообщение' }))
    expect(screen.queryByText('Нельзя удалить «Еда»')).not.toBeInTheDocument()
  })

  it('offers a retry when categories fail to load', async () => {
    server.use(http.get(`${API}/v1/categories/mine`, () => HttpResponse.error()))
    renderWithProviders(<CategoriesPage />)
    expect(await screen.findByText('Не удалось загрузить данные')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Повторить' })).toBeInTheDocument()
  })
})
