export interface DonutSegment {
  id: number | string
  value: number
  /** Начало и конец градиента сегмента */
  c1: string
  c2: string
}

/**
 * conic-gradient для кольца: у каждого сегмента градиент c1→c2, между сегментами прозрачный зазор
 * не больше 0,8% окружности. Старт и порядок — как в прототипе (`from -90deg`).
 * Без данных возвращает null — тогда рисуется нейтральное кольцо.
 */
export function donutBackground(segments: readonly DonutSegment[]): string | null {
  const total = segments.reduce((sum, segment) => sum + Math.max(segment.value, 0), 0)
  if (total <= 0) return null

  const visible = segments.filter((segment) => segment.value > 0)
  let acc = 0
  const stops: string[] = []
  for (const segment of visible) {
    const start = acc
    const end = acc + (segment.value / total) * 100
    const gap = visible.length > 1 ? Math.min(0.8, (end - start) / 3) : 0
    stops.push(
      `${segment.c1} ${start}%`,
      `${segment.c2} ${end - gap}%`,
      `transparent ${end - gap}%`,
      `transparent ${end}%`,
    )
    acc = end
  }
  return `conic-gradient(from -90deg, ${stops.join(', ')})`
}
