import { useQuery } from '@tanstack/vue-query'

import type {
  AppArticleConfirmOutlineRequest,
  AppArticleConfirmTitleRequest,
  AppArticleCreateRequest,
  AppArticleItem,
  AppArticlePageResponse,
  AppArticleQuery,
} from '@/services/types/app-article.type'
import type { IResponse } from '@/services/types/response.type'

import { useApiFetch } from '@/composables/use-fetch'
import { API_BASE_URL } from '@/constants/app-config'
import { appLocale } from '@/plugins/i18n'
import pinia from '@/plugins/pinia/setup'
import { useAuthStore } from '@/stores/auth'
import { buildApiRequestHeaders } from '@/utils/request-locale'

/**
 * 创建用户端 AI 文章任务。
 */
export async function createAppArticleTask(data: AppArticleCreateRequest) {
  const { apiFetch } = useApiFetch()
  return await apiFetch<IResponse<string>>('/app/article/create', {
    method: 'post',
    body: data,
  })
}

/**
 * 用户端分页获取本人文章创建记录。
 */
export function useGetAppArticlePageQuery(query: AppArticleQuery) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<AppArticlePageResponse<AppArticleItem>>, Error>({
    queryKey: ['app-article-page', query.page, query.pageSize, query.topic, query.title, query.status],
    queryFn: async () => await apiFetch<IResponse<AppArticlePageResponse<AppArticleItem>>>('/app/article/list/page', {
      method: 'post',
      body: {
        ...query,
        status: query.status === '' ? undefined : query.status,
      },
    }),
  })
}

/**
 * 用户端根据文章 id 获取本人文章详情。
 */
export async function getAppArticleDetail(id: number) {
  const { apiFetch } = useApiFetch()
  return await apiFetch<IResponse<AppArticleItem>>('/app/article/get', {
    method: 'get',
    query: { id },
  })
}

/**
 * 确认用户选择的标题，并触发后端生成大纲。
 */
export async function confirmAppArticleTitle(data: AppArticleConfirmTitleRequest) {
  const { apiFetch } = useApiFetch()
  return await apiFetch<IResponse<boolean>>('/app/article/confirm-title', {
    method: 'post',
    body: data,
  })
}

/**
 * 确认用户编辑后的大纲，并触发后端生成正文。
 */
export async function confirmAppArticleOutline(data: AppArticleConfirmOutlineRequest) {
  const { apiFetch } = useApiFetch()
  return await apiFetch<IResponse<boolean>>('/app/article/confirm-outline', {
    method: 'post',
    body: data,
  })
}

/**
 * 通过后端代理下载文章图片，避免浏览器直接访问 OSS 时被 CORS 拦截。
 */
export async function downloadAppArticleImage(taskId: string, imageUrl: string) {
  const authStore = useAuthStore(pinia)
  const headers = buildApiRequestHeaders(appLocale.value, authStore.session.token ?? undefined)
  const url = new URL(`${API_BASE_URL}/app/article/image/download`, window.location.origin)
  url.searchParams.set('taskId', taskId)
  url.searchParams.set('imageUrl', imageUrl)

  const response = await fetch(url, { headers })
  if (!response.ok) {
    throw new Error(`图片下载失败：${response.status}`)
  }
  return await response.blob()
}
