import { RouterPath } from '@/constants/route-path'

/**
 * 需要前端强制回登录页的业务码。
 */
const AUTH_EXPIRED_CODES = new Set([40100, 40101])
const DEFAULT_HTTP_ERROR_MESSAGE = '请求失败，请稍后重试。'

export interface HttpErrorRedirectAction {
  type: 'redirect'
  path: string
  query?: Record<string, string>
}

export interface HttpErrorToastAction {
  type: 'toast'
  message: string
}

export type HttpErrorAction = HttpErrorRedirectAction | HttpErrorToastAction

/**
 * 最小响应约束，只要求具备业务码和消息。
 */
interface ApiResponseLike {
  code: number
  message?: string
  data?: unknown
}

/**
 * 前端统一业务异常，保留后端业务码，便于页面层判断。
 */
export class ApiBusinessError extends Error {
  code: number

  constructor(code: number, message: string) {
    super(message)
    this.name = 'ApiBusinessError'
    this.code = code
  }
}

/**
 * 判断响应是否为业务成功。
 */
export function isApiSuccess(response: ApiResponseLike): boolean {
  return response.code === 0
}

/**
 * 判断业务码是否表示登录失效或权限不足，需要重新登录。
 */
export function isAuthExpiredCode(code: number): boolean {
  return AUTH_EXPIRED_CODES.has(code)
}

/**
 * 根据当前路径推断应该跳转到哪个登录页。
 */
export function resolveAuthRedirectPath(_currentPath: string | undefined): string {
  // 登录入口已统一，具体落点由登录后的角色权限决定。
  return String(RouterPath.USER_LOGIN)
}

/**
 * 将 HTTP 状态码映射为前端跳转或提示动作。
 */
export function resolveHttpErrorAction(status: number, message?: string, currentPath?: string): HttpErrorAction {
  // 401 错误页固定提供用户端登录入口，后续统一登录页时只需改这一层规则。
  if (status === 401) {
    const query: Record<string, string> = {}
    if (message) {
      query.message = message
    }
    if (currentPath) {
      query.redirect = currentPath
    }

    return {
      type: 'redirect',
      path: '/errors/401',
      query,
    }
  }

  if (status === 403) {
    return {
      type: 'redirect',
      path: '/errors/403',
    }
  }

  if (status === 404) {
    return {
      type: 'redirect',
      path: '/errors/404',
    }
  }

  return {
    type: 'toast',
    message: message || DEFAULT_HTTP_ERROR_MESSAGE,
  }
}

/**
 * 将后端业务失败包装成统一异常对象。
 */
export function createApiBusinessError(response: ApiResponseLike): ApiBusinessError {
  return new ApiBusinessError(response.code, response.message || '请求失败')
}
