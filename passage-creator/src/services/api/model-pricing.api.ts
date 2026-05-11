import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'

import type { AiModelPricingForm, AiModelPricingItem, AiModelPricingQuery } from '@/services/types/model-pricing.type'
import type { IPageResponse, IResponse } from '@/services/types/response.type'

import { useApiFetch } from '@/composables/use-fetch'

import { normalizeCreditQuery } from './credit.api'

/**
 * 生成模型费率保存请求体，统一修剪文本字段并保留后端 Long id 的字符串形态。
 */
export function normalizeModelPricingPayload(data: AiModelPricingForm) {
  return {
    ...data,
    id: data.id || undefined,
    provider: data.provider.trim(),
    model: data.model.trim(),
    requestType: data.requestType.trim(),
  }
}

/**
 * 获取模型费率配置分页。
 */
export function useGetModelPricingPageQuery(query: AiModelPricingQuery) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<IPageResponse<AiModelPricingItem>>, Error>({
    queryKey: computed(() => ['model-pricing-page', { ...query }]),
    queryFn: async () => await apiFetch<IResponse<IPageResponse<AiModelPricingItem>>>('/ai/pricing/list/page', {
      method: 'post',
      body: normalizeCreditQuery(query),
    }),
  })
}

/**
 * 新增或更新模型费率配置。
 */
export function useSaveModelPricingMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<AiModelPricingItem>, Error, AiModelPricingForm>({
    mutationKey: ['model-pricing-save'],
    mutationFn: async data => await apiFetch<IResponse<AiModelPricingItem>>('/ai/pricing/save', {
      method: 'post',
      body: normalizeModelPricingPayload(data),
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['model-pricing-page'] })
    },
  })
}

/**
 * 删除模型费率配置。
 */
export function useDeleteModelPricingMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<boolean>, Error, string>({
    mutationKey: ['model-pricing-delete'],
    mutationFn: async id => await apiFetch<IResponse<boolean>>('/ai/pricing/delete', {
      method: 'post',
      body: { id },
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['model-pricing-page'] })
    },
  })
}
