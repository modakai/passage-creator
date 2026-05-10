const WINDOWS_LINE_END_RE = /\r\n/g
const INLINE_CODE_RE = /`([^`]+)`/g
const INLINE_BOLD_RE = /\*\*([^*]+)\*\*/g
const INLINE_EM_RE = /\*([^*]+)\*/g
const HEADING_MARK_RE = /^(#{1,6})/
const ORDERED_LIST_MARK_RE = /^\d+\./
const HTTP_IMAGE_URL_RE = /^https?:\/\//i
const DATA_IMAGE_URL_RE = /^data:image\/[a-z0-9.+-]+;base64,/i

/**
 * 转义 HTML，避免 AI 生成内容通过 v-html 注入脚本。
 */
function escapeHtml(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll('\'', '&#39;')
}

/**
 * 只允许安全图片地址进入 img src，避免把任意协议写入 v-html。
 */
function normalizeImageUrl(value: string) {
  const url = value.trim()
  if (HTTP_IMAGE_URL_RE.test(url) || DATA_IMAGE_URL_RE.test(url)) {
    return url
  }
  return ''
}

/**
 * 渲染行内 Markdown 语法，覆盖正文展示需要的粗体、斜体和行内代码。
 */
function renderInlineMarkdown(value: string) {
  return escapeHtml(value)
    .replace(INLINE_CODE_RE, '<code>$1</code>')
    .replace(INLINE_BOLD_RE, '<strong>$1</strong>')
    .replace(INLINE_EM_RE, '<em>$1</em>')
}

/**
 * 解析标题行，避免使用带任意字符捕获的复杂正则。
 */
function parseHeading(line: string) {
  const trimmed = line.trimStart()
  const mark = HEADING_MARK_RE.exec(trimmed)
  if (!mark) {
    return null
  }
  const rest = trimmed.slice(mark[1].length)
  if (!rest.startsWith(' ')) {
    return null
  }
  return {
    level: mark[1].length,
    content: rest.trimStart(),
  }
}

/**
 * 解析单独成行的 Markdown 图片语法。
 */
function parseImage(line: string) {
  const trimmed = line.trim()
  if (!trimmed.startsWith('![') || !trimmed.endsWith(')')) {
    return null
  }

  const separatorIndex = trimmed.indexOf('](')
  if (separatorIndex <= 2) {
    return null
  }

  const alt = trimmed.slice(2, separatorIndex)
  const url = normalizeImageUrl(trimmed.slice(separatorIndex + 2, -1))
  if (!url) {
    return null
  }

  return { alt, url }
}

/**
 * 解析无序列表行。
 */
function parseUnorderedList(line: string) {
  const trimmed = line.trimStart()
  if (!trimmed.startsWith('- ') && !trimmed.startsWith('* ')) {
    return null
  }
  return trimmed.slice(2).trimStart()
}

/**
 * 解析有序列表行。
 */
function parseOrderedList(line: string) {
  const trimmed = line.trimStart()
  const mark = ORDERED_LIST_MARK_RE.exec(trimmed)
  if (!mark) {
    return null
  }
  const rest = trimmed.slice(mark[0].length)
  return rest.startsWith(' ') ? rest.trimStart() : null
}

/**
 * 解析引用行。
 */
function parseQuote(line: string) {
  const trimmed = line.trimStart()
  return trimmed.startsWith('> ') ? trimmed.slice(2).trimStart() : null
}

/**
 * 将受控 Markdown 转成 HTML；不支持原始 HTML，保证展示结果可读且安全。
 */
export function renderMarkdown(value: string) {
  const lines = value.replace(WINDOWS_LINE_END_RE, '\n').split('\n')
  const html: string[] = []
  let paragraph: string[] = []
  let listType: 'ul' | 'ol' | null = null
  let codeBlock: string[] | null = null

  /**
   * 输出当前段落。
   */
  function flushParagraph() {
    if (paragraph.length === 0) {
      return
    }
    html.push(`<p>${paragraph.map(renderInlineMarkdown).join('<br>')}</p>`)
    paragraph = []
  }

  /**
   * 关闭当前列表。
   */
  function flushList() {
    if (!listType) {
      return
    }
    html.push(`</${listType}>`)
    listType = null
  }

  for (const rawLine of lines) {
    const line = rawLine.trimEnd()

    if (line.trim().startsWith('```')) {
      flushParagraph()
      flushList()
      if (codeBlock) {
        html.push(`<pre><code>${escapeHtml(codeBlock.join('\n'))}</code></pre>`)
        codeBlock = null
      }
      else {
        codeBlock = []
      }
      continue
    }

    if (codeBlock) {
      codeBlock.push(rawLine)
      continue
    }

    if (!line.trim()) {
      flushParagraph()
      flushList()
      continue
    }

    const image = parseImage(line)
    if (image) {
      flushParagraph()
      flushList()
      html.push(`<figure><img src="${escapeHtml(image.url)}" alt="${escapeHtml(image.alt)}" loading="lazy"><figcaption>${renderInlineMarkdown(image.alt)}</figcaption></figure>`)
      continue
    }

    const heading = parseHeading(line)
    if (heading) {
      flushParagraph()
      flushList()
      html.push(`<h${heading.level}>${renderInlineMarkdown(heading.content)}</h${heading.level}>`)
      continue
    }

    const unordered = parseUnorderedList(line)
    if (unordered) {
      flushParagraph()
      if (listType !== 'ul') {
        flushList()
        listType = 'ul'
        html.push('<ul>')
      }
      html.push(`<li>${renderInlineMarkdown(unordered)}</li>`)
      continue
    }

    const ordered = parseOrderedList(line)
    if (ordered) {
      flushParagraph()
      if (listType !== 'ol') {
        flushList()
        listType = 'ol'
        html.push('<ol>')
      }
      html.push(`<li>${renderInlineMarkdown(ordered)}</li>`)
      continue
    }

    const quote = parseQuote(line)
    if (quote) {
      flushParagraph()
      flushList()
      html.push(`<blockquote>${renderInlineMarkdown(quote)}</blockquote>`)
      continue
    }

    flushList()
    paragraph.push(line.trim())
  }

  flushParagraph()
  flushList()
  if (codeBlock) {
    html.push(`<pre><code>${escapeHtml(codeBlock.join('\n'))}</code></pre>`)
  }

  return html.join('\n')
}
