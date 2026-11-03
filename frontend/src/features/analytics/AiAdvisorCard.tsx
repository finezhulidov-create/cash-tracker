import { Box, Button, Flex, Spinner, Stack, Text } from '@chakra-ui/react'
import { Sparkles } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { useAiAdvice, useAiStatus } from '../../api/hooks'
import type { AiAdviceResponse } from '../../api/types'
import { GlassCard } from '../../components/GlassCard'
import { IridescentButton } from '../../components/IridescentButton'
import { Markdown } from '../../components/Markdown'
import { PeriodStepper } from '../../components/PeriodStepper'
import { QueryError } from '../../components/QueryError'
import { addMonths, formatInstant, monthTitle, yearMonthOf } from '../../lib/dates'
import { MIN_YEAR } from './range'

/** Показывается только если ИИ-советник включён на сервере; иначе экран выглядит как раньше. */
export function AiAdvisorCard({ today }: { today: Date }) {
  const enabled = useAiStatus()
  if (!enabled.data) return null
  return <Advisor today={today} />
}

function Advisor({ today }: { today: Date }) {
  const currentMonth = yearMonthOf(today)
  const [month, setMonth] = useState(currentMonth)
  /** Полученные анализы по месяцам: можно листать месяцы туда-обратно, не гоняя запросы */
  const [answers, setAnswers] = useState<Record<string, AiAdviceResponse>>({})
  const advice = useAiAdvice()
  const pending = useRef<AbortController | null>(null)

  // ушли с экрана — ответ уже не нужен
  useEffect(() => () => pending.current?.abort(), [])

  function ask() {
    const requested = month
    const controller = new AbortController()
    pending.current = controller
    advice.mutate(
      { yearMonth: requested, signal: controller.signal },
      { onSuccess: (result) => setAnswers((previous) => ({ ...previous, [requested]: result })) },
    )
  }

  function cancel() {
    pending.current?.abort()
    advice.reset()
  }

  function goTo(delta: number) {
    setMonth((value) => addMonths(value, delta))
    advice.reset() // ошибка относилась к другому месяцу
  }

  const answer = answers[month]

  return (
    <GlassCard>
      <Stack gap="5">
        <Flex justify="space-between" align="center" wrap="wrap" gap="3">
          <Box>
            <Text fontWeight="600" fontSize="lg">
              ИИ-советник
            </Text>
            <Text fontSize="sm" color="fg.muted">
              Разбор трат и бюджетов за месяц
            </Text>
          </Box>
          <PeriodStepper
            label={monthTitle(month)}
            prevLabel="Предыдущий месяц для анализа"
            nextLabel="Следующий месяц для анализа"
            disablePrev={advice.isPending || month <= `${MIN_YEAR}-01`}
            disableNext={advice.isPending || month >= currentMonth}
            onPrev={() => goTo(-1)}
            onNext={() => goTo(1)}
          />
        </Flex>

        {advice.isPending ? (
          <Flex role="status" align="center" gap="4" wrap="wrap">
            <Spinner size="md" />
            <Box flex="1 1 240px">
              <Text fontWeight="500">Модель анализирует траты…</Text>
              <Text fontSize="sm" color="fg.muted">
                Обычно это занимает до минуты, иногда дольше: запросы идут по одному.
              </Text>
            </Box>
            <Button size="sm" variant="outline" onClick={cancel}>
              Отменить
            </Button>
          </Flex>
        ) : (
          <>
            {advice.isError && (
              <QueryError error={advice.error} onRetry={ask} title="Не удалось получить анализ" />
            )}

            {answer ? (
              <Stack gap="4">
                <Box>
                  <Markdown>{answer.text}</Markdown>
                </Box>
                <Flex justify="space-between" align="center" wrap="wrap" gap="3">
                  <Text fontSize="sm" color="fg.muted">
                    Сформировано {formatInstant(answer.generatedAt)}
                    {answer.cached && ' · данные не менялись, показан сохранённый анализ'}
                  </Text>
                  <Button size="sm" variant="outline" onClick={ask}>
                    Обновить анализ
                  </Button>
                </Flex>
              </Stack>
            ) : (
              !advice.isError && (
                <Flex align="center" justify="space-between" wrap="wrap" gap="4">
                  <Text color="fg.muted" flex="1 1 280px">
                    Нейросеть посмотрит, куда ушли деньги в этом месяце, сравнит с прошлым и бюджетами и подскажет,
                    что можно улучшить.
                  </Text>
                  <IridescentButton onClick={ask}>
                    <Sparkles />
                    Получить анализ
                  </IridescentButton>
                </Flex>
              )
            )}
          </>
        )}

        <Text fontSize="xs" color="fg.muted">
          Для анализа в GigaChat (Сбер) отправляются суммы, названия категорий и бюджеты за выбранный месяц, без email,
          имени и описаний покупок. Это подсказка, а не финансовая рекомендация: цифры стоит проверять.
        </Text>
      </Stack>
    </GlassCard>
  )
}
