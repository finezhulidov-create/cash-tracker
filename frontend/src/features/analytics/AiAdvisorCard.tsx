import { Box, Button, CloseButton, Dialog, Flex, Portal, Spinner, Stack, Text } from '@chakra-ui/react'
import { Sparkles } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { useAiAdvice, useAiStatus } from '../../api/hooks'
import type { AiAdviceResponse } from '../../api/types'
import { GlassCard } from '../../components/GlassCard'
import { IridescentButton } from '../../components/IridescentButton'
import { Markdown } from '../../components/Markdown'
import { PeriodStepper } from '../../components/PeriodStepper'
import { QueryError } from '../../components/QueryError'
import { useIsPhone } from '../../layout/useLayoutMode'
import { addMonths, formatInstant, monthTitle, yearMonthOf } from '../../lib/dates'
import { MIN_YEAR } from './range'

/** Показывается только если ИИ-советник включён на сервере; иначе экран выглядит как раньше. */
export function AiAdvisorCard({ today }: { today: Date }) {
  const enabled = useAiStatus()
  if (!enabled.data) return null
  return <Advisor today={today} />
}

function Advisor({ today }: { today: Date }) {
  const phone = useIsPhone()
  const currentMonth = yearMonthOf(today)
  const [month, setMonth] = useState(currentMonth)
  const [open, setOpen] = useState(false)
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
    setOpen(true)
    advice.mutate(
      { yearMonth: requested, signal: controller.signal },
      { onSuccess: (result) => setAnswers((previous) => ({ ...previous, [requested]: result })) },
    )
  }

  /** Закрыли окно во время запроса — запрос отменяется; ошибка тоже больше не нужна */
  function close() {
    setOpen(false)
    if (advice.isPending) pending.current?.abort()
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

        <Flex align="center" justify="space-between" wrap="wrap" gap="4">
          <Text color="fg.muted" flex="1 1 280px">
            {answer
              ? `Анализ за ${monthTitle(month).toLowerCase()} готов — откройте, чтобы прочитать.`
              : 'Нейросеть посмотрит, куда ушли деньги в этом месяце, сравнит с прошлым и бюджетами и подскажет, что можно улучшить.'}
          </Text>
          {answer ? (
            <IridescentButton onClick={() => setOpen(true)}>
              <Sparkles />
              Открыть анализ
            </IridescentButton>
          ) : (
            <IridescentButton onClick={ask} disabled={advice.isPending}>
              <Sparkles />
              Получить анализ
            </IridescentButton>
          )}
        </Flex>

        <Text fontSize="xs" color="fg.muted">
          Для анализа в GigaChat (Сбер) отправляются суммы, названия категорий и бюджеты за выбранный месяц, без email,
          имени и описаний покупок. Это подсказка, а не финансовая рекомендация: цифры стоит проверять.
        </Text>
      </Stack>

      <Dialog.Root
        open={open}
        onOpenChange={(event) => {
          if (!event.open) close()
        }}
        size={phone ? 'full' : 'lg'}
        scrollBehavior="inside"
        placement="center"
      >
        <Portal>
          <Dialog.Backdrop bg="rgba(0, 0, 0, 0.36)" />
          <Dialog.Positioner>
            <Dialog.Content bg="bg.panel" borderRadius={phone ? '0' : 'l3'}>
              <Dialog.Header>
                <Dialog.Title>ИИ-советник · {monthTitle(month)}</Dialog.Title>
              </Dialog.Header>
              <Dialog.CloseTrigger asChild>
                <CloseButton size="sm" aria-label="Закрыть" />
              </Dialog.CloseTrigger>

              <Dialog.Body>
                {advice.isPending ? (
                  <Flex role="status" align="center" gap="4" wrap="wrap" py="6">
                    <Spinner size="md" />
                    <Box flex="1 1 240px">
                      <Text fontWeight="500">Модель анализирует траты…</Text>
                      <Text fontSize="sm" color="fg.muted">
                        Обычно это занимает до минуты, иногда дольше: запросы идут по одному.
                      </Text>
                    </Box>
                  </Flex>
                ) : advice.isError ? (
                  <QueryError error={advice.error} onRetry={ask} title="Не удалось получить анализ" />
                ) : answer ? (
                  <Markdown>{answer.text}</Markdown>
                ) : (
                  <Text color="fg.muted">Анализ ещё не запрошен.</Text>
                )}
              </Dialog.Body>

              <Dialog.Footer justifyContent="space-between" flexWrap="wrap" gap="3">
                {advice.isPending ? (
                  <>
                    <span />
                    <Button size="sm" variant="outline" onClick={close}>
                      Отменить
                    </Button>
                  </>
                ) : (
                  <>
                    <Text fontSize="sm" color="fg.muted">
                      {answer && !advice.isError && (
                        <>
                          Сформировано {formatInstant(answer.generatedAt)}
                          {answer.cached && ' · данные не менялись, показан сохранённый анализ'}
                        </>
                      )}
                    </Text>
                    <Flex gap="2">
                      {answer && !advice.isError && (
                        <Button size="sm" variant="outline" onClick={ask}>
                          Обновить анализ
                        </Button>
                      )}
                      <Button size="sm" onClick={close}>
                        Закрыть
                      </Button>
                    </Flex>
                  </>
                )}
              </Dialog.Footer>
            </Dialog.Content>
          </Dialog.Positioner>
        </Portal>
      </Dialog.Root>
    </GlassCard>
  )
}
