import { screen } from '@testing-library/react'
import { Route, Routes } from 'react-router-dom'
import { renderWithProviders } from '../test/render'
import { PublicOnly, RequireAuth } from './guards'

function routes() {
  return (
    <Routes>
      <Route element={<PublicOnly />}>
        <Route path="/login" element={<div>Экран входа</div>} />
      </Route>
      <Route element={<RequireAuth />}>
        <Route path="/" element={<div>Главная</div>} />
        <Route path="/budgets" element={<div>Бюджеты</div>} />
      </Route>
    </Routes>
  )
}

describe('route guards', () => {
  it('sends an anonymous visitor to the login screen', () => {
    renderWithProviders(routes(), { route: '/budgets', authenticated: false })
    expect(screen.getByText('Экран входа')).toBeInTheDocument()
  })

  it('lets a signed-in user through', () => {
    renderWithProviders(routes(), { route: '/budgets' })
    expect(screen.getByText('Бюджеты')).toBeInTheDocument()
  })

  it('keeps a signed-in user away from the login screen', () => {
    renderWithProviders(routes(), { route: '/login' })
    expect(screen.getByText('Главная')).toBeInTheDocument()
  })
})
