import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { Route, Routes } from 'react-router-dom'
import { peekSession } from '../../auth/authStore'
import { categories, page, user } from '../../test/fixtures'
import { renderWithProviders } from '../../test/render'
import { API, server } from '../../test/server'
import { ProfilePage } from './ProfilePage'

function mockBackend() {
  const balances: number[] = []
  server.use(
    http.get(`${API}/v1/users/me`, () => HttpResponse.json({ ...user, balance: balances.at(-1) ?? user.balance })),
    http.get(`${API}/v1/categories/mine`, () => HttpResponse.json(page(categories))),
    http.put(`${API}/v1/users/me/balance`, async ({ request }) => {
      const body = (await request.json()) as { balance: number }
      balances.push(body.balance)
      return HttpResponse.json({ balance: body.balance })
    }),
  )
  return balances
}

function renderProfile() {
  return renderWithProviders(
    <Routes>
      <Route path="/profile" element={<ProfilePage />} />
      <Route path="/login" element={<div>Экран входа</div>} />
      <Route path="/categories" element={<div>Категории экран</div>} />
    </Routes>,
    { route: '/profile' },
  )
}

describe('ProfilePage', () => {
  it('shows who is signed in, the balance and how many categories there are', async () => {
    mockBackend()
    renderProfile()

    expect(await screen.findByText('Алекс')).toBeInTheDocument()
    expect(screen.getByText('alex@example.com')).toBeInTheDocument()
    expect(screen.getByText(/152.400 ₽/)).toBeInTheDocument()
    expect(await screen.findByText('3')).toBeInTheDocument()
    expect(screen.getByText('Задаётся вручную и не меняется от трат')).toBeInTheDocument()
  })

  it('changes the balance (decimal comma accepted)', async () => {
    const balances = mockBackend()
    renderProfile()
    await screen.findByText(/152.400 ₽/)

    await userEvent.click(screen.getByRole('button', { name: 'Изменить' }))
    const input = screen.getByLabelText('Новый баланс')
    expect(input).toHaveValue('152400')
    await userEvent.clear(input)
    await userEvent.type(input, '1250,5')
    await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }))

    await waitFor(() => expect(balances).toEqual([1250.5]))
    expect(await screen.findByText('Баланс обновлён')).toBeInTheDocument()
    // новая сумма — в карточке баланса и в описании уведомления
    expect(await screen.findAllByText(/1.250,5 ₽/)).toHaveLength(2)
    expect(screen.queryByLabelText('Новый баланс')).not.toBeInTheDocument()
  })

  it('does not send an empty balance', async () => {
    const balances = mockBackend()
    renderProfile()
    await screen.findByText(/152.400 ₽/)

    await userEvent.click(screen.getByRole('button', { name: 'Изменить' }))
    await userEvent.clear(screen.getByLabelText('Новый баланс'))
    await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }))

    expect(await screen.findByText(/Введите сумму/)).toBeInTheDocument()
    expect(balances).toEqual([])
  })

  it('cancels editing with Escape', async () => {
    mockBackend()
    renderProfile()
    await screen.findByText(/152.400 ₽/)
    await userEvent.click(screen.getByRole('button', { name: 'Изменить' }))
    await userEvent.type(screen.getByLabelText('Новый баланс'), '{Escape}')
    expect(screen.queryByLabelText('Новый баланс')).not.toBeInTheDocument()
  })

  it('shows the theme choice and explains the system mode', async () => {
    mockBackend()
    renderProfile()
    expect(await screen.findByText(/Повторяет настройки устройства — сейчас светлая/)).toBeInTheDocument()

    await userEvent.click(screen.getByText('Тёмная'))
    expect(await screen.findByText('Выбрано вручную')).toBeInTheDocument()
    expect(document.documentElement).toHaveClass('dark')
  })

  it('signs out: the session is gone and the login screen opens', async () => {
    mockBackend()
    renderProfile()
    await screen.findByText('Алекс')
    expect(peekSession()).not.toBeNull()

    await userEvent.click(screen.getByRole('button', { name: 'Выйти' }))

    expect(peekSession()).toBeNull()
    expect(localStorage.getItem('ct.session')).toBeNull()
  })
})
