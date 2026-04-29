import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'

import type { OnlineUserForceLogoutRequest, OnlineUserItem, OnlineUserQuery } from '@/services/types/online-user.type'
import type { IPageResponse, IResponse } from '@/services/types/response.type'

import { useApiFetch } from '@/composables/use-fetch'

import { normalizeOnlineUserQuery } from './online-user-query'

/**
 * 获取在线用户分页列表。
 */
export function useGetOnlineUserPageQuery(query: OnlineUserQuery) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<IPageResponse<OnlineUserItem>>, Error>({
    queryKey: computed(() => ['online-user-page', { ...query }]),
    queryFn: async () => await apiFetch<IResponse<IPageResponse<OnlineUserItem>>>('/online/user/list/page', {
      method: 'post',
      body: normalizeOnlineUserQuery(query),
    }),
  })
}

/**
 * 强制在线用户下线。
 */
export function useForceLogoutOnlineUserMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<boolean>, Error, OnlineUserForceLogoutRequest>({
    mutationKey: ['online-user-force-logout'],
    mutationFn: async data => await apiFetch<IResponse<boolean>>('/online/user/force-logout', {
      method: 'post',
      body: data,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['online-user-page'] })
    },
  })
}
