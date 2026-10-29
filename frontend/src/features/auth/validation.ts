export type AuthMode = 'login' | 'register'

export interface AuthValues {
  name: string
  email: string
  password: string
}

export type AuthErrors = Partial<Record<keyof AuthValues, string>>

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/** Проверки повторяют ограничения бэкенда: имя 2–20 символов, пароль 8–72 (предел bcrypt). */
export function validateAuth(mode: AuthMode, values: AuthValues): AuthErrors {
  const errors: AuthErrors = {}
  const email = values.email.trim()

  if (!email) errors.email = 'Введите email'
  else if (!EMAIL_PATTERN.test(email)) errors.email = 'Введите корректный email'

  if (mode === 'register') {
    const name = values.name.trim()
    if (name.length < 2 || name.length > 20) errors.name = 'Имя — от 2 до 20 символов'
    if (values.password.length < 8 || values.password.length > 72) {
      errors.password = 'Пароль — от 8 до 72 символов'
    }
  } else if (!values.password) {
    errors.password = 'Введите пароль'
  }

  return errors
}
