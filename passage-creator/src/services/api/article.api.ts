import type { Ref } from 'vue'

import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'

import type {
  ArticleForm,
  ArticleItem,
  ArticlePageResponse,
  ArticleQuery,
} from '@/services/types/article.type'
import type { IResponse } from '@/services/types/response.type'

import { useApiFetch } from '@/composables/use-fetch'

/**
 * 获取文章分页列表。
 */
export function useGetArticlePageQuery(query: ArticleQuery) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<ArticlePageResponse<ArticleItem>>, Error>({
    queryKey: ['article-page', query.page, query.pageSize, query.topic, query.title, query.status, query.userId],
    queryFn: async () => await apiFetch<IResponse<ArticlePageResponse<ArticleItem>>>('/article/list/page', {
      method: 'post',
      body: {
        ...query,
        status: query.status === '' ? undefined : query.status,
      },
    }),
  })
}

/**
 * 获取文章详情。
 */
export function useGetArticleDetailQuery(id: number | null | undefined, enabled?: Ref<boolean>) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<ArticleItem>, Error>({
    queryKey: computed(() => ['article-detail', id]),
    enabled: computed(() => !!id && (enabled ? enabled.value : true)),
    queryFn: async () => await apiFetch<IResponse<ArticleItem>>('/article/get', {
      method: 'get',
      query: { id },
    }),
  })
}

/**
 * 新增文章。
 */
export function useCreateArticleMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<number>, Error, ArticleForm>({
    mutationKey: ['article-create'],
    mutationFn: async data => await apiFetch<IResponse<number>>('/article/add', {
      method: 'post',
      body: data,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['article-page'] })
    },
  })
}

/**
 * 更新文章。
 */
export function useUpdateArticleMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<boolean>, Error, ArticleForm>({
    mutationKey: ['article-update'],
    mutationFn: async data => await apiFetch<IResponse<boolean>>('/article/update', {
      method: 'post',
      body: data,
    }),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['article-page'] })
      queryClient.invalidateQueries({ queryKey: ['article-detail', variables.id] })
    },
  })
}

/**
 * 删除文章。
 */
export function useDeleteArticleMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<boolean>, Error, number>({
    mutationKey: ['article-delete'],
    mutationFn: async id => await apiFetch<IResponse<boolean>>('/article/delete', {
      method: 'post',
      body: { id },
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['article-page'] })
    },
  })
}
