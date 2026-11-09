import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../test/render'
import { ThemeCycleButton, ThemeIconSegment, ThemeLabelSegment } from './ThemeControls'

describe('theme controls', () => {
  it('the icon switcher has readable names for screen readers', () => {
    renderWithProviders(<ThemeIconSegment />)
    for (const name of ['Светлая', 'Тёмная', 'Системная']) {
      expect(screen.getByRole('radio', { name })).toBeInTheDocument()
    }
  })

  it('selecting a mode changes the theme and marks the radio as checked', async () => {
    renderWithProviders(<ThemeLabelSegment />)
    expect(screen.getByRole('radio', { name: 'Системная' })).toBeChecked()

    await userEvent.click(screen.getByText('Тёмная'))

    expect(screen.getByRole('radio', { name: 'Тёмная' })).toBeChecked()
    expect(document.documentElement).toHaveClass('dark')
  })

  it('the cycle button tells the current mode and what the next click does', async () => {
    renderWithProviders(<ThemeCycleButton />)
    const button = screen.getByRole('button')
    expect(button).toHaveAccessibleName('Тема: системная. Переключить на «светлая»')

    await userEvent.click(button)
    expect(button).toHaveAccessibleName('Тема: светлая. Переключить на «тёмная»')
  })
})
