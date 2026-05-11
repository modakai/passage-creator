import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'

import type { CreditRechargeForm, CreditSummary, CreditTransactionItem, CreditTransactionQuery } from '@/services/types/credit.type'
import type { IPageResponse, IResponse } from '@/services/types/response.type'

import { useApiFetch } from '@/composables/use-fetch'

/**
 * 清理积分查询对象中的空筛选值，避免后端把“全部”当作真实条件。
 */
export function normalizeCreditQuery<T extends Record<string, any>>(query: T) {
  return Object.fromEntries(
    Object.entries(query).filter(([, value]) => value !== '' && value !== undefined && value !== null),
  )
}

/**
 * 生成手动充值请求体。用户 ID 必须保持字符串，避免超过 JS 安全整数后被改写。
 */
export function normalizeCreditRechargePayload(data: CreditRechargeForm) {
  return {
    ...data,
    userId: data.userId?.trim(),
  }
}

/**
 * 获取当前用户积分概览。
 */
export function useGetCreditSummaryQuery() {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<CreditSummary>, Error>({
    queryKey: ['credit-summary'],
    queryFn: async () => await apiFetch<IResponse<CreditSummary>>('/app/credit/summary'),
  })
}

/**
 * 获取当前用户积分流水。
 */
export function useGetMyCreditTransactionsQuery(query: CreditTransactionQuery) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<IPageResponse<CreditTransactionItem>>, Error>({
    queryKey: computed(() => ['my-credit-transactions', { ...query }]),
    queryFn: async () => await apiFetch<IResponse<IPageResponse<CreditTransactionItem>>>('/app/credit/transactions/page', {
      method: 'post',
      body: normalizeCreditQuery(query),
    }),
  })
}

/**
 * 管理端获取全站积分流水。
 */
export function useGetAdminCreditTransactionsQuery(query: CreditTransactionQuery) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<IPageResponse<CreditTransactionItem>>, Error>({
    queryKey: computed(() => ['admin-credit-transactions', { ...query }]),
    queryFn: async () => await apiFetch<IResponse<IPageResponse<CreditTransactionItem>>>('/credit/admin/transactions/page', {
      method: 'post',
      body: normalizeCreditQuery(query),
    }),
  })
}

/**
 * 管理员手动充值积分。
 */
export function useRechargeCreditMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<boolean>, Error, CreditRechargeForm>({
    mutationKey: ['credit-admin-recharge'],
    mutationFn: async data => await apiFetch<IResponse<boolean>>('/credit/admin/recharge', {
      method: 'post',
      body: normalizeCreditRechargePayload(data),
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-credit-transactions'] })
      queryClient.invalidateQueries({ queryKey: ['credit-summary'] })
    },
  })
}
