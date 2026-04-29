/**
 * ofetch: https://github.com/unjs/ofetch
 */
import { ofetch } from 'ofetch'
import { toast } from 'vue-sonner'

import { API_BASE_URL, API_TIMEOUT } from '@/constants/app-config'
import { appLocale } from '@/plugins/i18n'
import pinia from '@/plugins/pinia/setup'
import { useAuthStore } from '@/stores/auth'
import {
  createApiBusinessError,
  isApiSuccess,
  isAuthExpiredCode,
  resolveHttpErrorAction,
} from '@/utils/api-response'
import {
  getRequestDebounceKey,
  registerRequestDebounce,
  startGlobalLoading,
} from '@/utils/request-control'
import { buildApiRequestHeaders } from '@/utils/request-locale'

const finishLoadingMap = new WeakMap<object, () => void>()
const REQUEST_DEBOUNCE_DELAY = 500

/**
 * 根据异常动作统一执行前端跳转或提示。
 */
function handleHttpError(status: number, message?: string) {
  const currentPath = `${window.location.pathname}${window.location.search}`
  const action = resolveHttpErrorAction(status, message, currentPath)

  if (action.type === 'redirect') {
    const targetUrl = new URL(action.path, window.location.origin)
    if (action.query) {
      Object.entries(action.query).forEach(([key, value]) => {
        targetUrl.searchParams.set(key, value)
      })
    }
    window.location.replace(targetUrl.toString())
    return
  }

  toast.error(action.message)
}

/**
 * 判断当前请求是否应纳入防抖，FormData 上传允许连续提交不同文件。
 */
function shouldDebounceRequest(options: Record<string, any>) {
  if (options.debounce === false) {
    return false
  }
  return !(typeof FormData !== 'undefined' && options.body instanceof FormData)
}

/**
 * 结束当前请求的全局 Loading，保证成功、失败和请求错误路径都能释放计数。
 */
function finishTrackedLoading(options: object) {
  const finish = finishLoadingMap.get(options)
  if (!finish) {
    return
  }

  finish()
  finishLoadingMap.delete(options)
}

const apiFetch = ofetch.create({
  baseURL: API_BASE_URL,
  timeout: API_TIMEOUT ?? false,
  onRequest: ({ request, options }) => {
    const authStore = useAuthStore(pinia)
    const token = authStore.session.token
    const headers = buildApiRequestHeaders(appLocale.value, token ?? undefined)
    const requestOptions = options as Record<string, any>

    if (shouldDebounceRequest(requestOptions)) {
      const debounceKey = getRequestDebounceKey(String(request), requestOptions)
      if (!registerRequestDebounce(debounceKey, REQUEST_DEBOUNCE_DELAY)) {
        throw new Error('重复请求已拦截，请稍后再试。')
      }
    }

    // 保留调用方显式传入的请求头，再补充统一鉴权和语言信息。
    if (options.headers) {
      const customHeaders = new Headers(options.headers)
      customHeaders.forEach((value, key) => headers.set(key, value))
    }

    finishLoadingMap.set(options, startGlobalLoading())
    options.headers = headers
  },
  onRequestError: ({ options }) => {
    finishTrackedLoading(options)
  },
  onResponse: ({ response, options }) => {
    finishTrackedLoading(options)

    const body = response._data
    if (!body || typeof body.code !== 'number') {
      return
    }

    if (isApiSuccess(body)) {
      return
    }

    if (isAuthExpiredCode(body.code) && typeof window !== 'undefined') {
      // 登录失效时先清理本地会话，再统一进入 401 错误页。
      const authStore = useAuthStore(pinia)
      authStore.clearSession()
      handleHttpError(401, body.message || '登录已失效，请重新登录。')
      return
    }

    throw createApiBusinessError(body)
  },
  onResponseError: ({ response, options }) => {
    finishTrackedLoading(options)

    if (typeof window === 'undefined') {
      return
    }

    if (!response) {
      toast.error('网络异常，请稍后重试。')
      return
    }

    if (response.status === 401) {
      const authStore = useAuthStore(pinia)
      authStore.clearSession()
    }

    handleHttpError(response.status, response._data?.message || response.statusText)
  },
})

export function useApiFetch() {
  return {
    apiFetch,
  }
}
