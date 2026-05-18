import { useQuery } from '@tanstack/vue-query'

import type {
  AppRednoteCreateRequest,
  AppRednoteItem,
  AppRednotePageResponse,
  AppRednoteQuery,
} from '@/services/types/app-rednote.type'
import type { IResponse } from '@/services/types/response.type'

import { useApiFetch } from '@/composables/use-fetch'
import { API_BASE_URL } from '@/constants/app-config'
import { appLocale } from '@/plugins/i18n'
import pinia from '@/plugins/pinia/setup'
import { useAuthStore } from '@/stores/auth'
import { buildApiRequestHeaders } from '@/utils/request-locale'

/**
 * 创建用户端小红书爆款创作任务。
 */
export async function createAppRednoteTask(data: AppRednoteCreateRequest) {
  const { apiFetch } = useApiFetch()
  return await apiFetch<IResponse<string>>('/app/rednote/create', {
    method: 'post',
    body: data,
  })
}

/**
 * 用户端分页获取本人小红书创作记录。
 */
export function useGetAppRednotePageQuery(query: AppRednoteQuery) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<AppRednotePageResponse<AppRednoteItem>>, Error>({
    queryKey: ['app-rednote-page', query.page, query.pageSize, query.content, query.subject, query.status, query.phase],
    queryFn: async () => await apiFetch<IResponse<AppRednotePageResponse<AppRednoteItem>>>('/app/rednote/list/page', {
      method: 'post',
      body: {
        ...query,
        status: query.status === '' ? undefined : query.status,
        phase: query.phase === '' ? undefined : query.phase,
      },
    }),
  })
}

/**
 * 一次性获取用户端小红书分页记录，适合创作页右侧历史列表手动刷新。
 */
export async function listAppRednotePage(query: AppRednoteQuery) {
  const { apiFetch } = useApiFetch()
  return await apiFetch<IResponse<AppRednotePageResponse<AppRednoteItem>>>('/app/rednote/list/page', {
    method: 'post',
    body: {
      ...query,
      status: query.status === '' ? undefined : query.status,
      phase: query.phase === '' ? undefined : query.phase,
    },
  })
}

/**
 * 用户端根据任务 id 获取本人小红书详情。
 */
export async function getAppRednoteDetail(taskId: string) {
  const { apiFetch } = useApiFetch()
  return await apiFetch<IResponse<AppRednoteItem>>(`/app/rednote/detail/${encodeURIComponent(taskId)}`, {
    method: 'get',
  })
}

/**
 * 重新生成失败的小红书任务。
 */
export async function retryAppRednoteTask(taskId: string) {
  const { apiFetch } = useApiFetch()
  return await apiFetch<IResponse<boolean>>(`/app/rednote/retry/${encodeURIComponent(taskId)}`, {
    method: 'post',
  })
}

/**
 * 通过后端代理下载小红书图片，避免浏览器直接访问 OSS 时被 CORS 拦截。
 */
export async function downloadAppRednoteImage(taskId: string, imageUrl: string) {
  const authStore = useAuthStore(pinia)
  const headers = buildApiRequestHeaders(appLocale.value, authStore.session.token ?? undefined)
  const url = new URL(`${API_BASE_URL}/app/rednote/image/download`, window.location.origin)
  url.searchParams.set('taskId', taskId)
  url.searchParams.set('imageUrl', imageUrl)

  const response = await fetch(url, { headers })
  if (!response.ok) {
    throw new Error(`图片下载失败：${response.status}`)
  }
  return await response.blob()
}
