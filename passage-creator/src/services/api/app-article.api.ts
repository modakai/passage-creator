import type {
  AppArticleConfirmOutlineRequest,
  AppArticleConfirmTitleRequest,
  AppArticleCreateRequest,
} from '@/services/types/app-article.type'
import type { IResponse } from '@/services/types/response.type'

import { useApiFetch } from '@/composables/use-fetch'

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
