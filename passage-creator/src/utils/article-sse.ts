import type { ArticleSseMessage } from '@/services/types/app-article.type'

import { API_BASE_URL } from '@/constants/app-config'
import { appLocale } from '@/plugins/i18n'
import pinia from '@/plugins/pinia/setup'
import { useAuthStore } from '@/stores/auth'
import { buildApiRequestHeaders } from '@/utils/request-locale'

const SSE_LINE_BREAK_RE = /\r?\n/
const SSE_CHUNK_BREAK_RE = /\r?\n\r?\n/

export interface ConnectArticleSseOptions {
  onMessage: (message: ArticleSseMessage) => void
  onError?: (error: unknown) => void
}

/**
 * 解析 SSE 文本块，只处理后端发送的 data 行。
 */
function parseSseChunk(chunk: string) {
  return chunk
    .split(SSE_LINE_BREAK_RE)
    .filter(line => line.startsWith('data:'))
    .map(line => line.slice('data:'.length).trim())
    .filter(Boolean)
}

/**
 * 兼容后端在任务恢复场景下返回的一次性 JSON 消息。
 */
function parseJsonMessage(value: unknown): ArticleSseMessage | null {
  if (!value || typeof value !== 'object') {
    return null
  }
  const message = value as ArticleSseMessage
  return typeof message.type === 'string' ? message : null
}

/**
 * 连接文章生成进度 SSE。
 *
 * 原生 EventSource 无法设置 token 请求头，因此这里使用 fetch 读取流。
 */
export function connectArticleSse(taskId: string, options: ConnectArticleSseOptions) {
  const controller = new AbortController()
  const authStore = useAuthStore(pinia)
  const headers = buildApiRequestHeaders(appLocale.value, authStore.session.token ?? undefined)

  async function connect() {
    try {
      const response = await fetch(`${API_BASE_URL}/app/article/progress/${encodeURIComponent(taskId)}`, {
        method: 'GET',
        headers,
        signal: controller.signal,
      })

      if (!response.ok || !response.body) {
        throw new Error(`SSE 连接失败：${response.status}`)
      }

      const contentType = response.headers.get('content-type') ?? ''
      if (contentType.includes('application/json')) {
        const message = parseJsonMessage(await response.json())
        if (message) {
          options.onMessage(message)
          return
        }
        throw new Error('任务进度响应格式不正确')
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      while (!controller.signal.aborted) {
        const { value, done } = await reader.read()
        if (done) {
          break
        }

        buffer += decoder.decode(value, { stream: true })
        const chunks = buffer.split(SSE_CHUNK_BREAK_RE)
        buffer = chunks.pop() ?? ''

        for (const chunk of chunks) {
          for (const data of parseSseChunk(chunk)) {
            const message = JSON.parse(data) as ArticleSseMessage
            options.onMessage(message)

            if (message.type === 'ALL_COMPLETE' || message.type === 'ERROR') {
              controller.abort()
            }
          }
        }
      }
    }
    catch (error) {
      if (!controller.signal.aborted) {
        options.onError?.(error)
      }
    }
  }

  void connect()

  // 返回关闭函数，供页面卸载或任务结束时释放连接。
  return () => controller.abort()
}
