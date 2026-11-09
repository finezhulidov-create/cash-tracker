import { Box, Code, Heading, Separator, Table, Text } from '@chakra-ui/react'
import type { ReactNode } from 'react'
import type { Components } from 'react-markdown'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'

/**
 * Каждый элемент markdown превращается в компонент Chakra, поэтому текст стилизуется как остальной интерфейс.
 * Текст приходит от нейросети и считается недоверенным: HTML в нём отбрасывается (skipHtml), картинки не
 * показываются (иначе адрес картинки — канал утечки данных на чужой сервер), ссылки остаются просто текстом.
 */
const components: Components = {
  h1: ({ children }) => <SectionHeading>{children}</SectionHeading>,
  h2: ({ children }) => <SectionHeading>{children}</SectionHeading>,
  h3: ({ children }) => <SectionHeading>{children}</SectionHeading>,
  h4: ({ children }) => (
    <Heading as="h4" size="sm" fontWeight="600" mt="4" mb="1.5">
      {children}
    </Heading>
  ),
  h5: ({ children }) => (
    <Heading as="h4" size="sm" fontWeight="600" mt="4" mb="1.5">
      {children}
    </Heading>
  ),
  h6: ({ children }) => (
    <Heading as="h4" size="sm" fontWeight="600" mt="4" mb="1.5">
      {children}
    </Heading>
  ),
  p: ({ children }) => (
    <Text mb="3" lineHeight="1.65">
      {children}
    </Text>
  ),
  strong: ({ children }) => (
    <Text as="strong" fontWeight="600">
      {children}
    </Text>
  ),
  em: ({ children }) => <Text as="em">{children}</Text>,
  del: ({ children }) => <Text as="del">{children}</Text>,
  ul: ({ children }) => (
    <Box as="ul" listStyleType="disc" ps="5" mb="3" lineHeight="1.65">
      {children}
    </Box>
  ),
  ol: ({ children }) => (
    <Box as="ol" listStyleType="decimal" ps="5" mb="3" lineHeight="1.65">
      {children}
    </Box>
  ),
  li: ({ children }) => (
    <Box as="li" mb="1.5" ps="0.5">
      {children}
    </Box>
  ),
  blockquote: ({ children }) => (
    <Box borderStartWidth="3px" borderStartColor="border" ps="4" mb="3" color="fg.muted">
      {children}
    </Box>
  ),
  code: ({ children }) => <Code>{children}</Code>,
  pre: ({ children }) => (
    <Box as="pre" overflowX="auto" mb="3" fontSize="sm">
      {children}
    </Box>
  ),
  hr: () => <Separator my="4" />,
  a: ({ children }) => <>{children}</>,
  img: () => null,
  table: ({ children }) => (
    <Table.ScrollArea mb="4" borderWidth="1px" borderRadius="l2">
      <Table.Root size="sm" variant="line">
        {children}
      </Table.Root>
    </Table.ScrollArea>
  ),
  thead: ({ children }) => <Table.Header>{children}</Table.Header>,
  tbody: ({ children }) => <Table.Body>{children}</Table.Body>,
  tr: ({ children }) => <Table.Row>{children}</Table.Row>,
  th: ({ children, style }) => <Table.ColumnHeader style={style}>{children}</Table.ColumnHeader>,
  td: ({ children, style }) => <Table.Cell style={style}>{children}</Table.Cell>,
}

function SectionHeading({ children }: { children?: ReactNode }) {
  return (
    <Heading as="h3" size="md" fontWeight="600" mt="6" mb="2" _first={{ mt: '0' }}>
      {children}
    </Heading>
  )
}

/** Модели любят «<br>» для переноса строки в ячейках таблицы; остальной HTML отбрасывается, а этот тег — в пробел. */
const LINE_BREAK_TAG = /<br\s*\/?>/gi

/** Безопасный показ markdown-текста (таблицы, списки, заголовки, выделение) в стиле интерфейса. */
export function Markdown({ children }: { children: string }) {
  return (
    <ReactMarkdown remarkPlugins={[remarkGfm]} components={components} skipHtml>
      {children.replace(LINE_BREAK_TAG, ' ')}
    </ReactMarkdown>
  )
}
