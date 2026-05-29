import type { SseMessage } from '@/types'

import { API_BASE_URL } from './api'

type SseHandler = {
  onMessage: (message: SseMessage) => void
  onError?: (error: Event) => void
}

/**
 * 建立标准 EventSource 连接，后端事件不稳定时统一按 JSON 消息解析。
 */
function connect(path: string, handler: SseHandler) {
  const eventSource = new EventSource(`${API_BASE_URL}${path}`)

  eventSource.onmessage = (event) => {
    try {
      handler.onMessage(JSON.parse(event.data) as SseMessage)
    }
    catch {
      handler.onMessage({ type: 'MESSAGE', data: event.data })
    }
  }

  eventSource.onerror = (event) => {
    handler.onError?.(event)
  }

  return () => eventSource.close()
}

/**
 * 订阅文章工作流进度。
 */
export function connectArticleSse(taskId: string, handler: SseHandler) {
  return connect(`/app/article/sse/${encodeURIComponent(taskId)}`, handler)
}

/**
 * 订阅小红书工作流进度。
 */
export function connectRednoteSse(taskId: string, handler: SseHandler) {
  return connect(`/app/rednote/sse/${encodeURIComponent(taskId)}`, handler)
}
