import { Button, Flex, Heading, Text } from '@chakra-ui/react'
import type { ErrorInfo, ReactNode } from 'react'
import { Component } from 'react'

interface ErrorBoundaryProps {
  children: ReactNode
}

interface ErrorBoundaryState {
  failed: boolean
}

/** Последняя линия обороны: сбой отрисовки показывает понятный экран вместо белой страницы. */
export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { failed: false }

  static getDerivedStateFromError(): ErrorBoundaryState {
    return { failed: true }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Необработанная ошибка интерфейса', error, info.componentStack)
  }

  render() {
    if (!this.state.failed) return this.props.children
    return (
      <Flex role="alert" direction="column" align="center" justify="center" gap="4" minH="100dvh" px="6" textAlign="center">
        <Heading as="h1" size="xl">
          Что-то пошло не так
        </Heading>
        <Text color="fg.muted" maxW="420px">
          Приложение столкнулось с неожиданной ошибкой. Данные не потеряны — обновите страницу, и всё продолжится с того же места.
        </Text>
        <Button onClick={() => window.location.reload()}>Обновить страницу</Button>
      </Flex>
    )
  }
}
