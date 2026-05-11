import { useQuery } from '@tanstack/vue-query'

import type { AiUsageQuery, AiUsageRecordItem, AiUsageSummary, AiUsageUserSummary } from '@/services/types/ai-usage.type'
import type { IPageResponse, IResponse } from '@/services/types/response.type'

import { useApiFetch } from '@/composables/use-fetch'

import { normalizeCreditQuery } from './credit.api'

/**
 * 获取 AI 用量总览。
 */
export function useGetAiUsageSummaryQuery(query: AiUsageQuery) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<AiUsageSummary>, Error>({
    queryKey: computed(() => ['ai-usage-summary', { ...query }]),
    queryFn: async () => await apiFetch<IResponse<AiUsageSummary>>('/ai/usage/summary', {
      method: 'post',
      body: normalizeCreditQuery(query),
    }),
  })
}

/**
 * 获取用户维度 AI 用量排行。
 */
export function useGetAiUsageUserPageQuery(query: AiUsageQuery) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<IPageResponse<AiUsageUserSummary>>, Error>({
    queryKey: computed(() => ['ai-usage-user-page', { ...query }]),
    queryFn: async () => await apiFetch<IResponse<IPageResponse<AiUsageUserSummary>>>('/ai/usage/user/list/page', {
      method: 'post',
      body: normalizeCreditQuery(query),
    }),
  })
}

/**
 * 获取 AI 调用明细。
 */
export function useGetAiUsageRecordPageQuery(query: AiUsageQuery) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<IPageResponse<AiUsageRecordItem>>, Error>({
    queryKey: computed(() => ['ai-usage-record-page', { ...query }]),
    queryFn: async () => await apiFetch<IResponse<IPageResponse<AiUsageRecordItem>>>('/ai/usage/list/page', {
      method: 'post',
      body: normalizeCreditQuery(query),
    }),
  })
}
