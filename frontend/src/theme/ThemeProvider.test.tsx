import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ThemeProvider } from './ThemeProvider'
import { THEME_STORAGE_KEY, useThemeMode } from './themeContext'

type Listener = (event: { matches: boolean }) => void

/** matchMedia с управляемой системной темой. */
function mockSystemTheme(initialDark: boolean) {
  let dark = initialDark
  const listeners = new Set<Listener>()
  window.matchMedia = ((query: string) => ({
    get matches() {
      return query.includes('dark') ? dark : false
    },
    media: query,
    addEventListener: (_: string, listener: Listener) => listeners.add(listener),
    removeEventListener: (_: string, listener: Listener) => listeners.delete(listener),
  })) as unknown as typeof window.matchMedia
  return (nextDark: boolean) => {
    dark = nextDark
    listeners.forEach((listener) => listener({ matches: nextDark }))
  }
}

function Probe() {
  const { mode, resolved, setMode } = useThemeMode()
  return (
    <div>
      <span data-testid="state">{`${mode}/${resolved}`}</span>
      <button onClick={() => setMode('dark')}>dark</button>
      <button onClick={() => setMode('light')}>light</button>
      <button onClick={() => setMode('system')}>system</button>
    </div>
  )
}

const renderProbe = () =>
  render(
    <ThemeProvider>
      <Probe />
    </ThemeProvider>,
  )

afterEach(() => {
  document.documentElement.classList.remove('light', 'dark')
  document.documentElement.style.colorScheme = ''
})

describe('ThemeProvider', () => {
  it('follows the system theme by default', () => {
    mockSystemTheme(true)
    renderProbe()
    expect(screen.getByTestId('state')).toHaveTextContent('system/dark')
    expect(document.documentElement).toHaveClass('dark')
    expect(document.documentElement.style.colorScheme).toBe('dark')
  })

  it('restores the saved choice', () => {
    mockSystemTheme(true)
    localStorage.setItem(THEME_STORAGE_KEY, 'light')
    renderProbe()
    expect(screen.getByTestId('state')).toHaveTextContent('light/light')
    expect(document.documentElement).toHaveClass('light')
    expect(document.documentElement).not.toHaveClass('dark')
  })

  it('ignores a garbage saved value', () => {
    mockSystemTheme(false)
    localStorage.setItem(THEME_STORAGE_KEY, 'purple')
    renderProbe()
    expect(screen.getByTestId('state')).toHaveTextContent('system/light')
  })

  it('switches the class and remembers the choice', async () => {
    mockSystemTheme(false)
    renderProbe()
    await userEvent.click(screen.getByText('dark'))
    expect(document.documentElement).toHaveClass('dark')
    expect(document.documentElement).not.toHaveClass('light')
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark')
  })

  it('reacts to the device theme changing while in system mode', () => {
    const setSystemDark = mockSystemTheme(false)
    renderProbe()
    expect(document.documentElement).toHaveClass('light')
    act(() => setSystemDark(true))
    expect(screen.getByTestId('state')).toHaveTextContent('system/dark')
    expect(document.documentElement).toHaveClass('dark')
  })

  it('ignores the device theme once a manual choice is made', async () => {
    const setSystemDark = mockSystemTheme(false)
    renderProbe()
    await userEvent.click(screen.getByText('light'))
    act(() => setSystemDark(true))
    expect(screen.getByTestId('state')).toHaveTextContent('light/light')
  })

  it('picks up a change made in another tab', () => {
    mockSystemTheme(false)
    renderProbe()
    act(() => {
      window.dispatchEvent(new StorageEvent('storage', { key: THEME_STORAGE_KEY, newValue: 'dark' }))
    })
    expect(screen.getByTestId('state')).toHaveTextContent('dark/dark')
  })

  it('refuses to be used without a provider', () => {
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {})
    expect(() => render(<Probe />)).toThrow(/ThemeProvider/)
    spy.mockRestore()
  })
})
