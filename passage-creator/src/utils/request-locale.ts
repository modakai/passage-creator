import type { Language } from '@/plugins/i18n'

import {
  COMPATIBILITY_TOKEN_HEADER_ENABLED,
  COMPATIBILITY_TOKEN_HEADER_NAME,
  TOKEN_HEADER_NAME,
  TOKEN_HEADER_PREFIX,
} from '@/constants/app-config'

/**
 * 请求头构建配置。
 */
export interface ApiRequestHeaderOptions {
  /**
   * token 主请求头名称。
   */
  tokenHeaderName?: string
  /**
   * token 主请求头前缀。
   */
  tokenHeaderPrefix?: string
  /**
   * 是否启用兼容 token 请求头。
   */
  compatibilityTokenHeaderEnabled?: boolean
  /**
   * 兼容 token 请求头名称。
   */
  compatibilityTokenHeaderName?: string
}

/**
 * 将前端语言值转换为后端统一识别的请求头语言。
 *
 * @param locale 前端语言值
 * @returns 后端请求头语言
 */
export function resolveRequestLanguage(locale?: string): string {
  return locale === 'zh' ? 'zh-CN' : 'en-US'
}

/**
 * 构建统一的接口请求头。
 *
 * @param locale 前端语言值
 * @param token 登录 token
 * @param options 请求头构建配置
 * @returns 标准请求头对象
 */
export function buildApiRequestHeaders(
  locale?: Language | string,
  token?: string,
  options: ApiRequestHeaderOptions = {},
): Headers {
  const headers = new Headers()
  const tokenHeaderName = options.tokenHeaderName ?? TOKEN_HEADER_NAME
  const tokenHeaderPrefix = options.tokenHeaderPrefix ?? TOKEN_HEADER_PREFIX
  const compatibilityTokenHeaderEnabled = options.compatibilityTokenHeaderEnabled ?? COMPATIBILITY_TOKEN_HEADER_ENABLED
  const compatibilityTokenHeaderName = options.compatibilityTokenHeaderName ?? COMPATIBILITY_TOKEN_HEADER_NAME

  headers.set('Accept-Language', resolveRequestLanguage(locale))

  if (token) {
    headers.set(tokenHeaderName, `${tokenHeaderPrefix}${token}`)
    if (compatibilityTokenHeaderEnabled) {
      headers.set(compatibilityTokenHeaderName, token)
    }
  }

  return headers
}
