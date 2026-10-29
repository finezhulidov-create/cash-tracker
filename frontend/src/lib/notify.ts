import { createToaster } from '@chakra-ui/react'

const DURATION = 2800
const PHONE_QUERY = '(max-width: 719px)'

/** Десктоп и планшет: справа снизу, отступ 24px. */
export const desktopToaster = createToaster({
  placement: 'bottom-end',
  duration: DURATION,
  pauseOnPageIdle: true,
  offsets: '24px',
})

/** Телефон: по центру над таб-баром (84px от низа). */
export const phoneToaster = createToaster({
  placement: 'bottom',
  duration: DURATION,
  pauseOnPageIdle: true,
  offsets: { top: '16px', right: '16px', bottom: '84px', left: '16px' },
})

function current() {
  return window.matchMedia(PHONE_QUERY).matches ? phoneToaster : desktopToaster
}

export const notify = {
  success: (title: string, description?: string) =>
    current().create({ type: 'success', title, description }),
  error: (title: string, description?: string) =>
    current().create({ type: 'error', title, description }),
  info: (title: string, description?: string) =>
    current().create({ type: 'info', title, description }),
}
