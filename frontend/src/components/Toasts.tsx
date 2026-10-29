import { Toaster as ChakraToaster, Portal, Stack, Toast } from '@chakra-ui/react'
import { desktopToaster, phoneToaster } from '../lib/notify'

function ToasterView({ toaster, width }: { toaster: typeof desktopToaster; width?: string }) {
  return (
    <ChakraToaster toaster={toaster}>
      {(toast) => (
        <Toast.Root width={width}>
          <Toast.Indicator />
          <Stack gap="1" flex="1" maxWidth="100%">
            {toast.title && <Toast.Title>{toast.title}</Toast.Title>}
            {toast.description && <Toast.Description>{toast.description}</Toast.Description>}
          </Stack>
          {toast.closable && <Toast.CloseTrigger />}
        </Toast.Root>
      )}
    </ChakraToaster>
  )
}

/** Две очереди уведомлений: справа снизу на десктопе и по центру над таб-баром на телефоне. */
export function Toasts() {
  return (
    <Portal>
      <ToasterView toaster={desktopToaster} width="sm" />
      <ToasterView toaster={phoneToaster} />
    </Portal>
  )
}
