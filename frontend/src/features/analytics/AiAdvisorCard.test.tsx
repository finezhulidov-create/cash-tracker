import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { delay, http, HttpResponse } from 'msw'
import { keys } from '../../api/hooks'
import { renderWithProviders } from '../../test/render'
import { API, server } from '../../test/server'
import { AiAdvisorCard } from './AiAdvisorCard'

const TODAY = new Date(2026, 9, 31, 12, 0)
const ANSWER = '### 📊 Итог месяца\n\nПотрачено **12 000 ₽**, это на 20% больше, чем в сентябре.'

function aiEnabled(enabled = true) {
  server.use(http.get(`${API}/v1/ai/status`, () => HttpResponse.json({ enabled })))
}

/** Мок советника: запоминает, за какие месяцы просили анализ. */
function mockAdvice(text = ANSWER, extra: { cached?: boolean } = {}) {
  const asked: string[] = []
  server.use(
    http.post(`${API}/v1/ai/advice`, async ({ request }) => {
      const body = (await request.json()) as { yearMonth: string }
      asked.push(body.yearMonth)
      return HttpResponse.json({
        text,
        generatedAt: '2026-10-15T12:00:00Z',
        cached: extra.cached ?? false,
      })
    }),
  )
  return asked
}

function failAdvice(status: number, errorCode: string, headers: Record<string, string> = {}) {
  server.use(
    http.post(
      `${API}/v1/ai/advice`,
      () => HttpResponse.json({ status, errorCode, message: 'технический текст', timestamp: '2026-10-31T12:00:00' }, { status, headers }),
    ),
  )
}

async function openCard() {
  aiEnabled()
  const view = renderWithProviders(<AiAdvisorCard today={TODAY} />)
  await screen.findByText('ИИ-советник')
  return view
}

describe('AiAdvisorCard — switched off', () => {
  it('is not shown when the server has the advisor disabled', async () => {
    aiEnabled(false)
    const { queryClient } = renderWithProviders(<AiAdvisorCard today={TODAY} />)

    await waitFor(() => expect(queryClient.getQueryState(keys.aiStatus)?.status).toBe('success'))

    expect(screen.queryByText('ИИ-советник')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Получить анализ/ })).not.toBeInTheDocument()
  })

  it('is not shown when the status cannot be loaded', async () => {
    server.use(http.get(`${API}/v1/ai/status`, () => HttpResponse.error()))
    const { queryClient } = renderWithProviders(<AiAdvisorCard today={TODAY} />)

    await waitFor(() => expect(queryClient.getQueryState(keys.aiStatus)?.status).toBe('error'))

    expect(screen.queryByText('ИИ-советник')).not.toBeInTheDocument()
  })
})

describe('AiAdvisorCard — analysis', () => {
  it('starts with the current month and an invitation, without asking the model by itself', async () => {
    const asked = mockAdvice()
    await openCard()

    expect(screen.getByText('Октябрь 2026')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Получить анализ/ })).toBeEnabled()
    expect(screen.getByText(/без email, имени и описаний покупок/)).toBeInTheDocument()
    expect(asked).toEqual([])
  })

  it('asks for the shown month and displays the markdown answer with its time', async () => {
    const asked = mockAdvice()
    await openCard()

    await userEvent.click(screen.getByRole('button', { name: /Получить анализ/ }))

    expect(await screen.findByRole('heading', { name: '📊 Итог месяца' })).toBeInTheDocument()
    expect(screen.getByText('12 000 ₽').tagName).toBe('STRONG')
    expect(screen.getByText(/Сформировано \d{1,2} октября, \d{2}:\d{2}/)).toBeInTheDocument()
    expect(screen.queryByText(/сохранённый анализ/)).not.toBeInTheDocument()
    expect(asked).toEqual(['2026-10'])
  })

  it('says when the answer is the saved one because the data did not change', async () => {
    mockAdvice(ANSWER, { cached: true })
    await openCard()

    await userEvent.click(screen.getByRole('button', { name: /Получить анализ/ }))

    expect(await screen.findByText(/данные не менялись, показан сохранённый анализ/)).toBeInTheDocument()
  })

  it('can ask again with "Обновить анализ"', async () => {
    const asked = mockAdvice()
    await openCard()
    await userEvent.click(screen.getByRole('button', { name: /Получить анализ/ }))
    await userEvent.click(await screen.findByRole('button', { name: 'Обновить анализ' }))

    await waitFor(() => expect(asked).toEqual(['2026-10', '2026-10']))
  })

  it('shows a wait message while the model is working and lets the user cancel', async () => {
    server.use(http.post(`${API}/v1/ai/advice`, async () => {
      await delay('infinite')
      return HttpResponse.json({})
    }))
    await openCard()

    await userEvent.click(screen.getByRole('button', { name: /Получить анализ/ }))

    expect(await screen.findByText('Модель анализирует траты…')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Получить анализ/ })).not.toBeInTheDocument()
    // пока идёт запрос, месяц менять нельзя
    expect(screen.getByRole('button', { name: 'Предыдущий месяц для анализа' })).toBeDisabled()

    await userEvent.click(screen.getByRole('button', { name: 'Отменить' }))

    expect(screen.queryByText('Модель анализирует траты…')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Получить анализ/ })).toBeEnabled()
    expect(screen.queryByText('Не удалось получить анализ')).not.toBeInTheDocument()
  })
})

describe('AiAdvisorCard — months', () => {
  it('does not go into the future', async () => {
    mockAdvice()
    await openCard()

    expect(screen.getByRole('button', { name: 'Следующий месяц для анализа' })).toBeDisabled()
    await userEvent.click(screen.getByRole('button', { name: 'Предыдущий месяц для анализа' }))

    expect(screen.getByText('Сентябрь 2026')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Следующий месяц для анализа' })).toBeEnabled()
  })

  it('keeps the answer of each month while switching between them', async () => {
    const asked = mockAdvice()
    await openCard()
    await userEvent.click(screen.getByRole('button', { name: /Получить анализ/ }))
    await screen.findByRole('heading', { name: '📊 Итог месяца' })

    await userEvent.click(screen.getByRole('button', { name: 'Предыдущий месяц для анализа' }))
    // у сентября своего анализа ещё нет
    expect(screen.queryByRole('heading', { name: '📊 Итог месяца' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Получить анализ/ })).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Следующий месяц для анализа' }))
    expect(screen.getByRole('heading', { name: '📊 Итог месяца' })).toBeInTheDocument()
    expect(asked).toEqual(['2026-10'])
  })

  it('asks for the month that is shown after switching', async () => {
    const asked = mockAdvice()
    await openCard()
    await userEvent.click(screen.getByRole('button', { name: 'Предыдущий месяц для анализа' }))

    await userEvent.click(screen.getByRole('button', { name: /Получить анализ/ }))

    await screen.findByRole('heading', { name: '📊 Итог месяца' })
    expect(asked).toEqual(['2026-09'])
  })
})

describe('AiAdvisorCard — errors', () => {
  it('explains that the personal limit is spent and when it renews', async () => {
    failAdvice(429, 'AI_RATE_LIMIT_EXCEEDED', { 'Retry-After': '1800' })
    await openCard()

    await userEvent.click(screen.getByRole('button', { name: /Получить анализ/ }))

    expect(await screen.findByText('Не удалось получить анализ')).toBeInTheDocument()
    expect(screen.getByText('Лимит анализов исчерпан. Новый можно запросить через 30 минут.')).toBeInTheDocument()
    expect(screen.queryByText('технический текст')).not.toBeInTheDocument()
  })

  it('explains that the model is busy', async () => {
    failAdvice(429, 'AI_BUSY', { 'Retry-After': '30' })
    await openCard()

    await userEvent.click(screen.getByRole('button', { name: /Получить анализ/ }))

    expect(await screen.findByText('Модель сейчас отвечает на другой запрос. Попробуйте через минуту.')).toBeInTheDocument()
  })

  it.each([
    ['AI_UNAVAILABLE', 503, 'Сервис ИИ временно недоступен. Попробуйте позже.'],
    ['AI_AUTH_FAILED', 503, 'Сервер не смог войти в сервис ИИ. Администратору нужно проверить ключ.'],
    ['AI_QUOTA_EXHAUSTED', 503, 'У сервиса ИИ закончились токены. Сообщите администратору.'],
    ['AI_BAD_RESPONSE', 502, 'Модель вернула непригодный ответ. Попробуйте ещё раз.'],
    ['AI_DISABLED', 503, 'ИИ-советник не настроен на этом сервере'],
  ])('shows a readable message for %s', async (errorCode, status, message) => {
    failAdvice(status, errorCode)
    await openCard()

    await userEvent.click(screen.getByRole('button', { name: /Получить анализ/ }))

    expect(await screen.findByText(message)).toBeInTheDocument()
  })

  it('retries from the error block', async () => {
    failAdvice(503, 'AI_UNAVAILABLE')
    await openCard()
    await userEvent.click(screen.getByRole('button', { name: /Получить анализ/ }))
    await screen.findByText('Не удалось получить анализ')

    mockAdvice()
    await userEvent.click(screen.getByRole('button', { name: 'Повторить' }))

    expect(await screen.findByRole('heading', { name: '📊 Итог месяца' })).toBeInTheDocument()
    expect(screen.queryByText('Не удалось получить анализ')).not.toBeInTheDocument()
  })

  it('reports a lost connection', async () => {
    server.use(http.post(`${API}/v1/ai/advice`, () => HttpResponse.error()))
    await openCard()

    await userEvent.click(screen.getByRole('button', { name: /Получить анализ/ }))

    expect(await screen.findByText(/Нет связи с сервером/)).toBeInTheDocument()
  })

  it('forgets the error of one month when another is opened', async () => {
    failAdvice(503, 'AI_UNAVAILABLE')
    await openCard()
    await userEvent.click(screen.getByRole('button', { name: /Получить анализ/ }))
    await screen.findByText('Не удалось получить анализ')

    await userEvent.click(screen.getByRole('button', { name: 'Предыдущий месяц для анализа' }))

    expect(screen.queryByText('Не удалось получить анализ')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Получить анализ/ })).toBeInTheDocument()
  })
})
