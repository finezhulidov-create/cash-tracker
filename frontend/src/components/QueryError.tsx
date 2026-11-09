import { Alert, Button } from '@chakra-ui/react'
import { errorMessage } from '../api/errors'

interface QueryErrorProps {
  error: unknown
  onRetry?: () => void
  title?: string
}

/** Блок «не удалось загрузить» с кнопкой повтора — вместо бесконечных скелетонов при ошибке. */
export function QueryError({ error, onRetry, title = 'Не удалось загрузить данные' }: QueryErrorProps) {
  return (
    <Alert.Root status="error" variant="subtle" borderRadius="l3" alignItems="center">
      <Alert.Indicator />
      <Alert.Content>
        <Alert.Title>{title}</Alert.Title>
        <Alert.Description>{errorMessage(error)}</Alert.Description>
      </Alert.Content>
      {onRetry && (
        <Button size="sm" variant="outline" onClick={onRetry}>
          Повторить
        </Button>
      )}
    </Alert.Root>
  )
}
