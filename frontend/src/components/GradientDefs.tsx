import { CATEGORY_PALETTE } from '../lib/categoryStyle'

export const ICON_GRADIENT_ID = 'ct-icon-gradient'

/**
 * Скрытый SVG с градиентами: иконки Lucide закрашиваются ими через stroke="url(#id)".
 * Координаты в пространстве иконки (viewBox 24×24), поэтому градиент идёт по всей иконке, а не по каждому штриху.
 * - ct-icon-gradient: pink.500 → purple.500 (40%) → cyan.500 (75%) → teal.400, для активного пункта меню;
 * - ct-cat-0…8: градиенты палитры категорий.
 */
export function GradientDefs() {
  return (
    <svg width="0" height="0" aria-hidden focusable="false" style={{ position: 'absolute' }}>
      <defs>
        <linearGradient id={ICON_GRADIENT_ID} gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="24" y2="24">
          <stop offset="0" stopColor="#ec4899" />
          <stop offset="0.4" stopColor="#a855f7" />
          <stop offset="0.75" stopColor="#06b6d4" />
          <stop offset="1" stopColor="#2dd4bf" />
        </linearGradient>
        {CATEGORY_PALETTE.map(({ c1, c2 }, index) => (
          <linearGradient
            key={index}
            id={`ct-cat-${index}`}
            gradientUnits="userSpaceOnUse"
            x1="0"
            y1="0"
            x2="24"
            y2="24"
          >
            <stop offset="0" stopColor={c1} />
            <stop offset="1" stopColor={c2} />
          </linearGradient>
        ))}
      </defs>
    </svg>
  )
}
