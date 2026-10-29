import { createSystem, defaultConfig, defineConfig } from '@chakra-ui/react'

/**
 * Дефолтная тема Chakra UI v3 + то, что добавляет дизайн: шрифты, keyframes и CSS-переменные «перелива».
 * Цвета перелива заданы hex-значениями токенов Chakra из спецификации, чтобы их можно было
 * подставлять в обычный `background` без ссылок на токены.
 */
const config = defineConfig({
  globalCss: {
    ':root': {
      // Светлая тема, «ярко»: pink.300 → purple.300 → blue.300 → cyan.300 → teal.300 → yellow.300 → orange.300
      '--ct-flow':
        'linear-gradient(115deg, #f9a8d4 0%, #d8b4fe 16%, #a3cfff 32%, #67e8f9 48%, #5eead4 62%, #fde047 78%, #fdba74 88%, #f9a8d4 100%)',
      '--ct-on-flow': '#111111',
      '--ct-on-flow-muted': 'rgba(9, 9, 11, 0.68)',
      '--ct-chip-bg': 'rgba(255, 255, 255, 0.45)',
      '--ct-bar-track': 'rgba(0, 0, 0, 0.12)',
      '--ct-bar-fill': '#111111',
      '--ct-aura-1': 'var(--chakra-colors-pink-200)',
      '--ct-aura-2': 'var(--chakra-colors-cyan-100)',
      '--ct-aura-3': 'var(--chakra-colors-yellow-100)',
      '--ct-aura-opacity': '0.7',
    },
    '.dark': {
      '--ct-flow':
        'linear-gradient(115deg, #a41752 0%, #641ba3 16%, #173da6 32%, #134152 48%, #0c5d56 62%, #4a1772 80%, #a41752 100%)',
      '--ct-on-flow': '#ffffff',
      '--ct-on-flow-muted': 'rgba(255, 255, 255, 0.78)',
      '--ct-chip-bg': 'rgba(255, 255, 255, 0.14)',
      '--ct-bar-track': 'rgba(255, 255, 255, 0.18)',
      '--ct-bar-fill': '#ffffff',
      '--ct-aura-1': 'var(--chakra-colors-purple-800)',
      '--ct-aura-2': 'var(--chakra-colors-cyan-900)',
      '--ct-aura-3': 'var(--chakra-colors-teal-900)',
      '--ct-aura-opacity': '0.55',
    },
    body: {
      fontFeatureSettings: '"cv11"',
    },
    // Крупные суммы и значения выравниваются по цифрам
    '.ct-num': {
      fontVariantNumeric: 'tabular-nums',
    },
  },
  theme: {
    tokens: {
      fonts: {
        body: { value: "'Inter', system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" },
        heading: {
          value: "'Wix Madefor Text', 'Inter', system-ui, -apple-system, 'Segoe UI', sans-serif",
        },
      },
    },
    keyframes: {
      ctFlow: {
        '0%': { backgroundPosition: '0% 50%' },
        '100%': { backgroundPosition: '100% 50%' },
      },
      ctHue: {
        from: { filter: 'hue-rotate(0deg)' },
        to: { filter: 'hue-rotate(360deg)' },
      },
      ctSkeletonFade: {
        from: { opacity: '0' },
        to: { opacity: '1' },
      },
    },
  },
})

export const system = createSystem(defaultConfig, config)
