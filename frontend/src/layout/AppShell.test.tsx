import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, vi } from 'vitest'
import { categories, page, user } from '../test/fixtures'
import { renderWithProviders } from '../test/render'
import { API, server } from '../test/server'
import { AppShell } from './AppShell'

function mockBackend() {
  server.use(
    http.get(`${API}/v1/users/me`, () => HttpResponse.json(user)),
    http.get(`${API}/v1/categories/mine`, () => HttpResponse.json(page(categories))),
    http.get(`${API}/v1/transactions`, () => HttpResponse.json(page([], 0, 1, 7))),
  )
}

const original = window.matchMedia

/** Ширина окна: phone — меньше 720, desktop — от 1100, иначе планшет. */
function setViewport(mode: 'phone' | 'tablet' | 'desktop') {
  window.matchMedia = ((query: string) => ({
    matches: query.includes('max-width: 719px') ? mode === 'phone' : query.includes('min-width: 1100px') ? mode === 'desktop' : false,
    media: query,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  })) as unknown as typeof window.matchMedia
}

function renderShell(route = '/') {
  return renderWithProviders(
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<div>Обзор-контент</div>} />
        <Route path="transactions" element={<div>Операции-контент</div>} />
        <Route path="profile" element={<div>Профиль-контент</div>} />
      </Route>
    </Routes>,
    { route },
  )
}

beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(2026, 9, 29, 12, 0))
  mockBackend()
})
afterEach(() => {
  window.matchMedia = original
  vi.useRealTimers()
})

describe('AppShell', () => {
  it('desktop: side menu with labels, screen title and a greeting', async () => {
    setViewport('desktop')
    renderShell()

    const menu = screen.getByRole('navigation', { name: 'Основное меню' })
    for (const label of ['Обзор', 'Операции', 'Бюджеты', 'Аналитика', 'Категории', 'Профиль']) {
      expect(within(menu).getByRole('link', { name: label })).toBeInTheDocument()
    }
    expect(within(menu).getByRole('link', { name: 'Обзор' })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('heading', { level: 1, name: 'Обзор' })).toBeInTheDocument()
    expect(await screen.findByText('Октябрь 2026 · привет, Алекс')).toBeInTheDocument()
    expect(screen.getByText('Обзор-контент')).toBeInTheDocument()
  })

  it('puts the screen name into the browser tab title and restores it on leaving', async () => {
    setViewport('desktop')
    const { unmount } = renderShell('/transactions')

    expect(document.title).toBe('Операции · Cash Tracker')
    unmount()
    expect(document.title).toBe('Cash Tracker')
  })

  it('tablet: icons only, labels live in the title attribute', () => {
    setViewport('tablet')
    renderShell()

    const menu = screen.getByRole('navigation', { name: 'Основное меню' })
    expect(within(menu).queryByText('Операции')).not.toBeInTheDocument()
    expect(within(menu).getByTitle('Операции')).toBeInTheDocument()
  })

  it('shows the number of expenses in the month on the operations screen', async () => {
    setViewport('desktop')
    renderShell('/transactions')

    expect(screen.getByRole('heading', { level: 1, name: 'Операции' })).toBeInTheDocument()
    expect(await screen.findByText('7 трат в октябре')).toBeInTheDocument()
  })

  it('"Добавить трату" in the header opens the new expense panel', async () => {
    setViewport('desktop')
    renderShell()

    await userEvent.click(screen.getByRole('button', { name: 'Добавить трату' }))

    expect(await screen.findByRole('dialog', { name: 'Новая трата' })).toBeInTheDocument()
  })

  it('phone: bottom tab bar with a central add button, profile through the avatar', async () => {
    setViewport('phone')
    renderShell()

    const tabs = screen.getByRole('navigation', { name: 'Основное меню' })
    expect(within(tabs).getAllByRole('link').map((link) => link.textContent)).toEqual([
      'Обзор',
      'Операции',
      'Бюджеты',
      'Аналитика',
    ])
    expect(screen.getByRole('link', { name: 'Профиль' })).toHaveAttribute('href', '/profile')

    await userEvent.click(within(tabs).getByRole('button', { name: 'Добавить трату' }))
    expect(await screen.findByRole('dialog', { name: 'Новая трата' })).toBeInTheDocument()
  })

  it('phone: the theme button cycles light → dark → system', async () => {
    setViewport('phone')
    renderShell()

    await userEvent.click(screen.getByRole('button', { name: /Тема: системная/ }))
    expect(await screen.findByRole('button', { name: /Тема: светлая/ })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: /Тема: светлая/ }))
    expect(await screen.findByRole('button', { name: /Тема: тёмная/ })).toBeInTheDocument()
    expect(document.documentElement).toHaveClass('dark')
  })
})
