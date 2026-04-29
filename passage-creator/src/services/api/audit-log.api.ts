import { useMutation, useQuery } from '@tanstack/vue-query'

import type {
  AuditLogExportQuery,
  AuditLogItem,
  AuditLogPageResponse,
  AuditLogQuery,
} from '@/services/types/audit-log.type'

import { useApiFetch } from '@/composables/use-fetch'

import type { IResponse } from '../types/response.type'

import { normalizeAuditLogQuery } from './audit-query'

/**
 * 获取审计日志分页列表。
 */
export function useGetAuditLogPageQuery(query: AuditLogQuery) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<AuditLogPageResponse<AuditLogItem>>, Error>({
    queryKey: computed(() => ['audit-log-page', { ...query }]),
    queryFn: async () => await apiFetch<IResponse<AuditLogPageResponse<AuditLogItem>>>('/audit/log/list/page', {
      method: 'post',
      body: normalizeAuditLogQuery(query),
    }),
  })
}

/**
 * 获取审计日志详情。
 */
export function useGetAuditLogDetailQuery(id: Ref<number | null>) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<AuditLogItem>, Error>({
    queryKey: computed(() => ['audit-log-detail', id.value]),
    enabled: computed(() => Boolean(id.value)),
    queryFn: async () => await apiFetch<IResponse<AuditLogItem>>('/audit/log/get', {
      method: 'get',
      query: { id: id.value },
    }),
  })
}

/**
 * 导出当前筛选条件下的审计日志。
 */
export function useExportAuditLogMutation() {
  const { apiFetch } = useApiFetch()

  return useMutation<Blob, Error, AuditLogExportQuery>({
    mutationKey: ['audit-log-export'],
    mutationFn: async query => await apiFetch<Blob>('/audit/log/export', {
      method: 'post',
      body: normalizeAuditLogQuery(query),
      responseType: 'blob' as any,
    }),
  })
}
