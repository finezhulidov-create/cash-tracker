import { donutBackground } from './donut'

const seg = (id: number, value: number) => ({ id, value, c1: `#c1${id}`, c2: `#c2${id}` })

describe('donutBackground', () => {
  it('has nothing to draw without spending', () => {
    expect(donutBackground([])).toBeNull()
    expect(donutBackground([seg(1, 0)])).toBeNull()
  })

  it('draws a single segment around the whole ring without a gap', () => {
    expect(donutBackground([seg(1, 500)])).toBe(
      'conic-gradient(from -90deg, #c11 0%, #c21 100%, transparent 100%, transparent 100%)',
    )
  })

  it('splits the ring proportionally and leaves a gap between segments', () => {
    const css = donutBackground([seg(1, 75), seg(2, 25)])!
    expect(css.startsWith('conic-gradient(from -90deg, #c11 0%, #c21 74.2%')).toBe(true)
    // второй сегмент начинается там, где закончился первый, и заканчивается на 100%
    expect(css).toContain('#c12 75%')
    expect(css).toContain('transparent 100%')
  })

  it('shrinks the gap for tiny segments (at most a third of the segment)', () => {
    const css = donutBackground([seg(1, 99), seg(2, 1)])!
    const second = css.split(', ').filter((part) => part.includes('#c22'))[0]!
    const end = parseFloat(second.split(' ')[1]!)
    expect(end).toBeGreaterThan(99.6)
    expect(end).toBeLessThan(100)
  })

  it('skips zero segments', () => {
    const css = donutBackground([seg(1, 0), seg(2, 10)])!
    expect(css).not.toContain('#c11')
    expect(css).toContain('#c12 0%')
  })
})
