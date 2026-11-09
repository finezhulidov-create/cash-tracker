import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { Route, Routes } from 'react-router-dom'
import { peekSession } from '../../auth/authStore'
import { renderWithProviders } from '../../test/render'
import { API, server } from '../../test/server'
import { LoginPage } from './LoginPage'

function renderLogin() {
  return renderWithProviders(
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={<div>Главная</div>} />
    </Routes>,
    { route: '/login', authenticated: false },
  )
}

function error(status: number, errorCode: string, message = 'msg') {
  return HttpResponse.json({ status, message, errorCode, timestamp: '2026-10-29T10:00:00' }, { status })
}

async function fill(label: RegExp, value: string) {
  await userEvent.type(screen.getByLabelText(label), value)
}

describe('LoginPage', () => {
  it('logs in, stores the session and opens the app', async () => {
    let body: unknown
    server.use(
      http.post(`${API}/auth/login`, async ({ request }) => {
        body = await request.json()
        return HttpResponse.json({ accessToken: 'jwt-1', tokenType: 'Bearer', expiresIn: 86400 })
      }),
    )
    renderLogin()

    await fill(/email/i, '  alex@example.com ')
    await fill(/пароль/i, 'secret-pass')
    await userEvent.click(screen.getByRole('button', { name: 'Войти' }))

    expect(await screen.findByText('Главная')).toBeInTheDocument()
    expect(body).toEqual({ email: 'alex@example.com', password: 'secret-pass' })
    expect(peekSession()).toMatchObject({ accessToken: 'jwt-1', tokenType: 'Bearer' })
    expect(peekSession()!.expiresAt - Date.now()).toBeGreaterThan(86_000_000)
  })

  it('shows field errors without calling the API when the form is empty', async () => {
    renderLogin()
    await userEvent.click(screen.getByRole('button', { name: 'Войти' }))

    expect(await screen.findByText('Введите email')).toBeInTheDocument()
    expect(screen.getByText('Введите пароль')).toBeInTheDocument()
    expect(peekSession()).toBeNull()
  })

  it('explains wrong credentials under the password field', async () => {
    server.use(http.post(`${API}/auth/login`, () => error(401, 'INVALID_CREDENTIALS')))
    renderLogin()

    await fill(/email/i, 'alex@example.com')
    await fill(/пароль/i, 'wrong-pass')
    await userEvent.click(screen.getByRole('button', { name: 'Войти' }))

    expect(await screen.findByText('Неверный email или пароль')).toBeInTheDocument()
    expect(peekSession()).toBeNull()
    expect(screen.queryByText('Главная')).not.toBeInTheDocument()
  })

  it('tells to wait on rate limiting (429)', async () => {
    server.use(http.post(`${API}/auth/login`, () => error(429, 'RATE_LIMIT_EXCEEDED')))
    renderLogin()

    await fill(/email/i, 'alex@example.com')
    await fill(/пароль/i, 'secret-pass')
    await userEvent.click(screen.getByRole('button', { name: 'Войти' }))

    expect(await screen.findByText('Слишком много попыток, попробуйте через минуту')).toBeInTheDocument()
  })

  it('reports an unreachable server', async () => {
    server.use(http.post(`${API}/auth/login`, () => HttpResponse.error()))
    renderLogin()

    await fill(/email/i, 'alex@example.com')
    await fill(/пароль/i, 'secret-pass')
    await userEvent.click(screen.getByRole('button', { name: 'Войти' }))

    expect(await screen.findByText(/Нет связи с сервером/)).toBeInTheDocument()
  })

  it('registers with the name, validating it against backend limits first', async () => {
    let body: unknown
    server.use(
      http.post(`${API}/auth/register`, async ({ request }) => {
        body = await request.json()
        return HttpResponse.json({ accessToken: 'jwt-2', tokenType: 'Bearer', expiresIn: 86400 })
      }),
    )
    renderLogin()
    await userEvent.click(screen.getByText('Регистрация'))
    expect(screen.getByRole('heading', { name: 'Создать аккаунт' })).toBeInTheDocument()

    await fill(/имя/i, 'А')
    await fill(/email/i, 'alex@example.com')
    await fill(/пароль/i, 'short')
    await userEvent.click(screen.getByRole('button', { name: 'Создать аккаунт' }))
    expect(await screen.findByText('Имя — от 2 до 20 символов')).toBeInTheDocument()
    expect(screen.getByText('Пароль — от 8 до 72 символов')).toBeInTheDocument()
    expect(body).toBeUndefined()

    await userEvent.type(screen.getByLabelText(/имя/i), 'лекс')
    await userEvent.type(screen.getByLabelText(/пароль/i), '-long-enough')
    await userEvent.click(screen.getByRole('button', { name: 'Создать аккаунт' }))

    expect(await screen.findByText('Главная')).toBeInTheDocument()
    expect(body).toEqual({ email: 'alex@example.com', username: 'Алекс', password: 'short-long-enough' })
  })

  it('shows "email already registered" under the email field', async () => {
    server.use(http.post(`${API}/auth/register`, () => error(409, 'EMAIL_ALREADY_EXISTS')))
    renderLogin()
    await userEvent.click(screen.getByText('Регистрация'))

    await fill(/имя/i, 'Алекс')
    await fill(/email/i, 'taken@example.com')
    await fill(/пароль/i, 'long-enough-pass')
    await userEvent.click(screen.getByRole('button', { name: 'Создать аккаунт' }))

    await waitFor(() => expect(screen.getByText('Этот email уже зарегистрирован')).toBeInTheDocument())
    expect(screen.getByLabelText(/email/i)).toBeInvalid()
  })
})
