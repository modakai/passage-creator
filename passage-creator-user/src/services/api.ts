import type { ApiResponse, AppArticleItem, AppRednoteItem, CreditSummary, LoginPayload, PageResponse, RegisterPayload } from '@/types'

import { clearSession, sessionState, setSession, type LoginUser } from './session'

const DEFAULT_API_HOST = 'http://localhost:8101'
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL
  || `${import.meta.env.VITE_SERVER_API_URL || DEFAULT_API_HOST}${import.meta.env.VITE_SERVER_API_PREFIX || '/api'}`
const TOKEN_HEADER_NAME = import.meta.env.VITE_AUTH_TOKEN_HEADER_NAME || 'Authorization'
const TOKEN_HEADER_PREFIX = import.meta.env.VITE_AUTH_TOKEN_HEADER_PREFIX ?? 'Bearer '
const COMPATIBILITY_TOKEN_HEADER_NAME = import.meta.env.VITE_AUTH_COMPATIBILITY_TOKEN_HEADER_NAME || 'token'
const COMPATIBILITY_TOKEN_HEADER_ENABLED = import.meta.env.VITE_AUTH_COMPATIBILITY_TOKEN_HEADER_ENABLED !== 'false'

type RequestOptions = RequestInit & {
  auth?: boolean
}

/**
 * 创建带鉴权和语言信息的请求头，保持和后端 TokenManager 配置一致。
 */
function buildHeaders(options: RequestOptions) {
  const headers = new Headers(options.headers)
  headers.set('Accept-Language', 'zh-CN')
  if (!(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }

  if (options.auth !== false && sessionState.token) {
    headers.set(TOKEN_HEADER_NAME, `${TOKEN_HEADER_PREFIX}${sessionState.token}`)
    if (COMPATIBILITY_TOKEN_HEADER_ENABLED) {
      headers.set(COMPATIBILITY_TOKEN_HEADER_NAME, sessionState.token)
    }
  }

  return headers
}

/**
 * 统一处理后端 BaseResponse，避免页面里重复判断 code/message。
 */
async function request<T>(path: string, options: RequestOptions = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: buildHeaders(options),
  })

  if (!response.ok) {
    if (response.status === 401) {
      clearSession()
      redirectToLogin()
    }
    throw new Error(`请求失败：${response.status}`)
  }

  const result = await response.json() as ApiResponse<T>
  if (result.code && result.code !== 0 && result.code !== 200) {
    if (result.code === 40100) {
      clearSession()
      redirectToLogin()
    }
    throw new Error(result.message || '业务请求失败')
  }
  return result.data as T
}

/**
 * 登录失效时保留当前地址，用户重新登录后能回到原工作流。
 */
function redirectToLogin() {
  const current = `${window.location.pathname}${window.location.search}`
  if (!current.startsWith('/auth')) {
    window.location.replace(`/auth?mode=login&return=${encodeURIComponent(current)}`)
  }
}

/**
 * 调用后端登录接口并写入独立用户端会话。
 */
export async function login(payload: LoginPayload) {
  const user = await request<LoginUser>('/user/login', {
    method: 'POST',
    auth: false,
    body: JSON.stringify(payload),
  })
  setSession(user)
  return user
}

/**
 * 注册后立即登录，降低新用户从注册到创作的断点。
 */
export async function register(payload: RegisterPayload) {
  await request<number>('/user/register', {
    method: 'POST',
    auth: false,
    body: JSON.stringify(payload),
  })
  return login({
    userAccount: payload.userAccount,
    userPassword: payload.userPassword,
  })
}

/**
 * 拉取当前登录用户，用于刷新后校验 token 是否仍有效。
 */
export async function getCurrentUser() {
  const user = await request<LoginUser>('/user/get/login', { method: 'GET' })
  setSession({ ...user, token: sessionState.token })
  return user
}

/**
 * 通知后端注销并清理本地会话。
 */
export async function logout() {
  try {
    await request<boolean>('/user/logout', { method: 'POST' })
  }
  catch {
    // 退出操作以清理前端登录态为准，后端不可用时也不能阻塞重新登录测试。
  }
  finally {
    clearSession()
  }
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

/**
 * 获取当前用户积分概览，额度页和导航余额可以复用该接口。
 */
export function getCreditSummary() {
  return request<CreditSummary>('/app/credit/summary', { method: 'GET' })
}

export { API_BASE_URL, buildHeaders }
