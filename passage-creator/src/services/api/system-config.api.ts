import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'

import type { IResponse } from '@/services/types/response.type'
import type { SystemConfigItem } from '@/services/types/system-config.type'

import { useApiFetch } from '@/composables/use-fetch'

/**
 * 按键查询系统配置。
 */
export function useGetSystemConfigByKeyQuery(key: string) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<SystemConfigItem | null>, Error>({
    queryKey: ['system-config', key],
    queryFn: async () => await apiFetch<IResponse<SystemConfigItem | null>>('/system/config/get', {
      method: 'get',
      query: { key },
    }),
  })
}

/**
 * 新增系统配置。
 */
export function useCreateSystemConfigMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<boolean>, Error, SystemConfigItem>({
    mutationKey: ['system-config-create'],
    mutationFn: async data => await apiFetch<IResponse<boolean>>('/system/config/add', {
      method: 'post',
      body: data,
    }),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['system-config', variables.key] })
    },
  })
}

/**
 * 更新系统配置。
 */
export function useUpdateSystemConfigByKeyMutation(key: string) {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<boolean>, Error, SystemConfigItem>({
    mutationKey: ['system-config-update', key],
    mutationFn: async data => await apiFetch<IResponse<boolean>>('/system/config/update', {
      method: 'post',
      body: data,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['system-config', key] })
    },
  })
}
