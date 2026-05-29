import type { ApiResponse, AppArticleItem, AppRednoteItem, PageResponse } from '@/types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'
const TOKEN_STORAGE_KEY = 'sakura_user_token'

/**
 * 读取独立用户端保存的登录令牌；未登录时允许请求继续由后端返回 401。
 */
function getAuthToken() {
  return window.localStorage.getItem(TOKEN_STORAGE_KEY) || ''
}

/**
 * 统一处理后端 BaseResponse，避免页面里重复判断 code/message。
 */
async function request<T>(path: string, options: RequestInit = {}) {
  const headers = new Headers(options.headers)
  headers.set('Content-Type', 'application/json')
  headers.set('Accept-Language', 'zh-CN')

  const token = getAuthToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  })

  if (!response.ok) {
    throw new Error(`请求失败：${response.status}`)
  }

  const result = await response.json() as ApiResponse<T>
  if (result.code && result.code !== 0 && result.code !== 200) {
    throw new Error(result.message || '业务请求失败')
  }
  return result.data as T
}

/**
 * 创建文章任务，后端会返回可用于 SSE 订阅的 taskId。
 */
export function createArticleTask(topic: string, enabledImageMethods: string[]) {
  return request<string>('/app/article/create', {
    method: 'POST',
    body: JSON.stringify({ topic, enabledImageMethods }),
  })
}

/**
 * 确认标题后，后端继续生成可编辑大纲。
 */
export function confirmArticleTitle(taskId: string, selectedMainTitle: string, selectedSubTitle: string, userDescription?: string) {
  return request<boolean>('/app/article/confirm-title', {
    method: 'POST',
    body: JSON.stringify({ taskId, selectedMainTitle, selectedSubTitle, userDescription }),
  })
}

/**
 * 确认大纲后，后端继续生成正文和配图。
 */
export function confirmArticleOutline(taskId: string, outline: unknown) {
  return request<boolean>('/app/article/confirm-outline', {
    method: 'POST',
    body: JSON.stringify({ taskId, outline }),
  })
}

/**
 * 获取用户文章记录，用于作品页和任务页的卡片式列表。
 */
export function listArticles(status = '') {
  return request<PageResponse<AppArticleItem>>('/app/article/list/page', {
    method: 'POST',
    body: JSON.stringify({ page: 1, pageSize: 12, status: status || undefined }),
  })
}

/**
 * 创建小红书创作任务，完整流程由后端自动推进。
 */
export function createRednoteTask(content: string) {
  return request<string>('/app/rednote/create', {
    method: 'POST',
    body: JSON.stringify({ content }),
  })
}

/**
 * 获取小红书详情，用于刷新恢复和作品列表展示。
 */
export function getRednoteDetail(taskId: string) {
  return request<AppRednoteItem>(`/app/rednote/detail/${encodeURIComponent(taskId)}`)
}

/**
 * 获取用户小红书记录，避免在用户端使用后台表格。
 */
export function listRednotes(status = '') {
  return request<PageResponse<AppRednoteItem>>('/app/rednote/list/page', {
    method: 'POST',
    body: JSON.stringify({ page: 1, pageSize: 12, status: status || undefined }),
  })
}

export { API_BASE_URL, TOKEN_STORAGE_KEY }
