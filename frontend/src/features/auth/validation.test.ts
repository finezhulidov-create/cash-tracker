import { validateAuth } from './validation'

const valid = { name: 'Алекс', email: 'alex@example.com', password: 'correct-horse' }

describe('validateAuth', () => {
  it('accepts correct login data (name is not needed)', () => {
    expect(validateAuth('login', { ...valid, name: '' })).toEqual({})
  })

  it('does not apply the registration password length to login', () => {
    expect(validateAuth('login', { ...valid, password: 'x' })).toEqual({})
    expect(validateAuth('login', { ...valid, password: '' }).password).toBe('Введите пароль')
  })

  it('checks the email', () => {
    expect(validateAuth('login', { ...valid, email: '' }).email).toBe('Введите email')
    expect(validateAuth('login', { ...valid, email: 'not-an-email' }).email).toBe('Введите корректный email')
    expect(validateAuth('login', { ...valid, email: '  alex@example.com ' })).toEqual({})
  })

  it('requires a 2–20 character name when registering', () => {
    expect(validateAuth('register', { ...valid, name: 'А' }).name).toBeDefined()
    expect(validateAuth('register', { ...valid, name: ' А ' }).name).toBeDefined()
    expect(validateAuth('register', { ...valid, name: 'a'.repeat(21) }).name).toBeDefined()
    expect(validateAuth('register', { ...valid, name: 'a'.repeat(20) })).toEqual({})
  })

  it('requires a 8–72 character password when registering', () => {
    expect(validateAuth('register', { ...valid, password: 'short' }).password).toBeDefined()
    expect(validateAuth('register', { ...valid, password: 'a'.repeat(72) })).toEqual({})
    expect(validateAuth('register', { ...valid, password: 'a'.repeat(73) }).password).toBeDefined()
  })
})
