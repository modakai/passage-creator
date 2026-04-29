import { useQuery } from '@tanstack/vue-query'

import type { DashboardStatistics } from '@/services/types/dashboard.type'
import type { IResponse } from '@/services/types/response.type'

import { useApiFetch } from '@/composables/use-fetch'

/**
 * 获取管理端 Dashboard 统计聚合数据。
 */
export function useGetDashboardStatisticsQuery() {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<DashboardStatistics>, Error>({
    queryKey: ['dashboard-statistics'],
    queryFn: async () => await apiFetch<IResponse<DashboardStatistics>>('/dashboard/statistics', {
      method: 'get',
    }),
  })
}
