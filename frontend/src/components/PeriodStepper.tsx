import { HStack, IconButton, Text } from '@chakra-ui/react'
import { ChevronLeft, ChevronRight } from 'lucide-react'

interface PeriodStepperProps {
  label: string
  onPrev: () => void
  onNext: () => void
  prevLabel: string
  nextLabel: string
  disablePrev?: boolean
  disableNext?: boolean
}

/** ‹ «Октябрь 2026» › — переключатель месяца или года. */
export function PeriodStepper({
  label,
  onPrev,
  onNext,
  prevLabel,
  nextLabel,
  disablePrev,
  disableNext,
}: PeriodStepperProps) {
  return (
    <HStack gap="2">
      <IconButton
        aria-label={prevLabel}
        variant="outline"
        size="sm"
        onClick={onPrev}
        disabled={disablePrev}
      >
        <ChevronLeft />
      </IconButton>
      <Text
        minW="150px"
        textAlign="center"
        fontSize="lg"
        fontWeight="600"
        aria-live="polite"
      >
        {label}
      </Text>
      <IconButton
        aria-label={nextLabel}
        variant="outline"
        size="sm"
        onClick={onNext}
        disabled={disableNext}
      >
        <ChevronRight />
      </IconButton>
    </HStack>
  )
}
