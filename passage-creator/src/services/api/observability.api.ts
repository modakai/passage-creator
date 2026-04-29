import { useQuery } from '@tanstack/vue-query'

import type {
  ApiSummary,
  ErrorTrendBucket,
  ObservabilityEventItem,
  ObservabilityEventQuery,
  SystemStatus,
} from '@/services/types/observability.type'
import type { IPageResponse, IResponse } from '@/services/types/response.type'

import { useApiFetch } from '@/composables/use-fetch'

/**
 * 归一化运维事件查询参数，避免空字符串污染后端筛选。
 */
function normalizeObservabilityQuery(query: ObservabilityEventQuery) {
  return Object.fromEntries(
    Object.entries(query).filter(([, value]) => value !== '' && value !== undefined && value !== null),
  )
}

/**
 * 获取系统状态聚合数据。
 */
export function useGetSystemStatusQuery() {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<SystemStatus>, Error>({
    queryKey: ['observability-system-status'],
    queryFn: async () => await apiFetch<IResponse<SystemStatus>>('/admin/observability/status', {
      method: 'get',
    }),
    refetchInterval: 30000,
  })
}

/**
 * 获取接口质量摘要。
 */
export function useGetApiSummaryQuery(query: ObservabilityEventQuery) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<ApiSummary>, Error>({
    queryKey: computed(() => ['observability-api-summary', { ...query }]),
    queryFn: async () => await apiFetch<IResponse<ApiSummary>>('/admin/observability/api/summary', {
      method: 'post',
      body: normalizeObservabilityQuery(query),
    }),
  })
}

/**
 * 获取慢接口分页列表。
 */
export function useGetSlowApiPageQuery(query: ObservabilityEventQuery) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<IPageResponse<ObservabilityEventItem>>, Error>({
    queryKey: computed(() => ['observability-slow-api-page', { ...query }]),
    queryFn: async () => await apiFetch<IResponse<IPageResponse<ObservabilityEventItem>>>('/admin/observability/api/slow/page', {
      method: 'post',
      body: normalizeObservabilityQuery(query),
    }),
  })
}

/**
 * 获取错误趋势。
 */
export function useGetErrorTrendQuery(query: ObservabilityEventQuery) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<ErrorTrendBucket[]>, Error>({
    queryKey: computed(() => ['observability-error-trend', { ...query }]),
    queryFn: async () => await apiFetch<IResponse<ErrorTrendBucket[]>>('/admin/observability/api/errors/trend', {
      method: 'post',
      body: normalizeObservabilityQuery(query),
    }),
  })
}

/**
 * 获取安全事件分页列表。
 */
export function useGetSecurityEventPageQuery(query: ObservabilityEventQuery) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<IPageResponse<ObservabilityEventItem>>, Error>({
    queryKey: computed(() => ['observability-security-event-page', { ...query }]),
    queryFn: async () => await apiFetch<IResponse<IPageResponse<ObservabilityEventItem>>>('/admin/observability/security/events/page', {
      method: 'post',
      body: normalizeObservabilityQuery(query),
    }),
  })
}
