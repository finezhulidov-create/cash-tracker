import type { ButtonProps } from '@chakra-ui/react'
import { Button } from '@chakra-ui/react'

const BG = 'linear-gradient(115deg, #f9a8d4, #d8b4fe 30%, #67e8f9 65%, #fef08a)'

/** Основная кнопка действия: iridescent-градиент, тёмный текст, фиолетовая тень. */
export function IridescentButton(props: ButtonProps) {
  return (
    <Button
      background={BG}
      color="#111111"
      fontWeight="600"
      boxShadow="0 4px 14px rgba(168, 85, 247, 0.3)"
      _hover={{ background: BG, filter: 'brightness(1.05)' }}
      _active={{ background: BG, filter: 'brightness(0.96)' }}
      _disabled={{ opacity: 0.45, boxShadow: 'none', filter: 'none' }}
      {...props}
    />
  )
}
