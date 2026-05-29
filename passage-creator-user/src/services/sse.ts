import type { SseMessage } from '@/types'

import { API_BASE_URL, buildHeaders } from './api'

type SseHandler = {
  onMessage: (message: SseMessage) => void
  onError?: (error: unknown) => void
}

const SSE_LINE_BREAK_RE = /\r?\n/
const SSE_CHUNK_BREAK_RE = /\r?\n\r?\n/

/**
 * 解析 SSE 文本块，只消费 data 行，兼容后端默认事件名称。
 */
function parseSseChunk(chunk: string) {
  return chunk
    .split(SSE_LINE_BREAK_RE)
    .filter(line => line.startsWith('data:'))
    .map(line => line.slice('data:'.length).trim())
    .filter(Boolean)
}

/**
 * 使用 fetch 读取 SSE 流，原因是原生 EventSource 无法携带后端要求的 token header。
 */
function connect(path: string, handler: SseHandler) {
  const controller = new AbortController()

  async function openStream() {
    try {
      const response = await fetch(`${API_BASE_URL}${path}`, {
        method: 'GET',
        headers: buildHeaders({}),
        signal: controller.signal,
      })

      if (!response.ok || !response.body) {
        throw new Error(`SSE 连接失败：${response.status}`)
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
            handler.onMessage(JSON.parse(data) as SseMessage)
          }
        }
      }
    }
    catch (error) {
      if (!controller.signal.aborted) {
        handler.onError?.(error)
      }
    }
  }

  void openStream()
  return () => controller.abort()
}

/**
 * 订阅文章工作流进度。
 */
export function connectArticleSse(taskId: string, handler: SseHandler) {
  return connect(`/app/article/progress/${encodeURIComponent(taskId)}`, handler)
}

/**
 * 订阅小红书工作流进度。
 */
export function connectRednoteSse(taskId: string, handler: SseHandler) {
  return connect(`/app/rednote/progress/${encodeURIComponent(taskId)}`, handler)
}
