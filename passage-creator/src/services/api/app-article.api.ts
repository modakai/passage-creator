import type { AppArticleCreateRequest } from '@/services/types/app-article.type'
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
