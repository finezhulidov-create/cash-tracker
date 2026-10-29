import { countLabel, FORMS, plural } from './plural'

describe('plural', () => {
  it.each([
    [0, 'трат'],
    [1, 'трата'],
    [2, 'траты'],
    [4, 'траты'],
    [5, 'трат'],
    [11, 'трат'],
    [12, 'трат'],
    [14, 'трат'],
    [21, 'трата'],
    [22, 'траты'],
    [25, 'трат'],
    [101, 'трата'],
    [111, 'трат'],
    [112, 'трат'],
  ])('%d → %s', (n, expected) => {
    expect(plural(n, FORMS.transaction)).toBe(expected)
  })

  it('handles negative numbers by their absolute value', () => {
    expect(plural(-2, FORMS.day)).toBe('дня')
  })
})

describe('countLabel', () => {
  it('joins the number and the correct word form', () => {
    expect(countLabel(1, FORMS.part)).toBe('1 часть')
    expect(countLabel(3, FORMS.part)).toBe('3 части')
    expect(countLabel(7, FORMS.day)).toBe('7 дней')
  })
})
