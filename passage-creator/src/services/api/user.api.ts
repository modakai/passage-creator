import type { MaybeRefOrGetter } from 'vue'

import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { computed, toValue } from 'vue'

import type { IPageResponse, IResponse } from '@/services/types/response.type'
import type {
  UserAddForm,
  UserEntityId,
  UserItem,
  UserQuery,
  UserUpdateForm,
  UserUpdateMyForm,
  UserUpdatePasswordForm,
} from '@/services/types/user.type'

import { useApiFetch } from '@/composables/use-fetch'
import { normalizeUserQuery } from '@/services/api/admin-query'
import { shouldEnableUserDetailQuery } from '@/services/api/user-query'

/**
 * 获取后台用户分页列表。
 */
export function useGetUserPageQuery(query: UserQuery) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<IPageResponse<UserItem>>, Error>({
    queryKey: ['user-page', query.page, query.pageSize, query.userName, query.userRole, query.status],
    queryFn: async () => await apiFetch<IResponse<IPageResponse<UserItem>>>('/user/list/page', {
      method: 'post',
      body: normalizeUserQuery({
        page: query.page ?? 1,
        pageSize: query.pageSize ?? 10,
        userName: query.userName,
        userRole: query.userRole,
        status: query.status ?? '',
      }),
    }),
  })
}

/**
 * 获取后台用户详情。
 */
export function useGetUserDetailQuery(id: UserEntityId | null | undefined, enabled: MaybeRefOrGetter<boolean> = true) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<UserItem>, Error>({
    queryKey: ['user-detail', id],
    enabled: computed(() => shouldEnableUserDetailQuery(id, toValue(enabled))),
    queryFn: async () => await apiFetch<IResponse<UserItem>>('/user/get', {
      method: 'get',
      query: { id },
    }),
  })
}

/**
 * 新增后台用户。
 */
export function useCreateUserMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<number>, Error, UserAddForm>({
    mutationKey: ['user-create'],
    mutationFn: async data => await apiFetch<IResponse<number>>('/user/add', {
      method: 'post',
      body: data,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['user-page'] })
    },
  })
}

/**
 * 更新后台用户。
 */
export function useUpdateUserMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<boolean>, Error, UserUpdateForm>({
    mutationKey: ['user-update'],
    mutationFn: async data => await apiFetch<IResponse<boolean>>('/user/update', {
      method: 'post',
      body: data,
    }),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['user-page'] })
      queryClient.invalidateQueries({ queryKey: ['user-detail', variables.id] })
    },
  })
}

/**
 * 删除后台用户。
 */
export function useDeleteUserMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<boolean>, Error, UserEntityId>({
    mutationKey: ['user-delete'],
    mutationFn: async id => await apiFetch<IResponse<boolean>>('/user/delete', {
      method: 'post',
      body: { id },
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['user-page'] })
    },
  })
}

/**
 * 管理员重置用户密码。
 */
export function useResetUserPasswordMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<boolean>, Error, UserEntityId>({
    mutationKey: ['user-reset-password'],
    mutationFn: async id => await apiFetch<IResponse<boolean>>('/user/reset/password', {
      method: 'post',
      body: { id },
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['user-page'] })
    },
  })
}

/**
 * 获取公开用户详情。
 */
export function useGetUserVOQuery(id: UserEntityId | null | undefined) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<UserItem>, Error>({
    queryKey: ['user-vo', id],
    enabled: computed(() => Boolean(id)),
    queryFn: async () => await apiFetch<IResponse<UserItem>>('/user/get/vo', {
      method: 'get',
      query: { id },
    }),
  })
}

/**
 * 获取公开用户分页列表。
 */
export function useGetUserVOPageQuery(query: UserQuery) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<IPageResponse<UserItem>>, Error>({
    queryKey: ['user-vo-page', query.page, query.pageSize, query.userName, query.status],
    queryFn: async () => await apiFetch<IResponse<IPageResponse<UserItem>>>('/user/list/page/vo', {
      method: 'post',
      body: normalizeUserQuery({
        page: query.page ?? 1,
        pageSize: query.pageSize ?? 10,
        userName: query.userName,
        userRole: query.userRole,
        status: query.status ?? '',
      }),
    }),
  })
}

/**
 * 更新当前登录用户个人信息。
 */
export function useUpdateMyUserMutation() {
  const { apiFetch } = useApiFetch()

  return useMutation<IResponse<boolean>, Error, UserUpdateMyForm>({
    mutationKey: ['user-update-my'],
    mutationFn: async data => await apiFetch<IResponse<boolean>>('/user/update/my', {
      method: 'post',
      body: data,
    }),
  })
}

/**
 * 更新当前登录用户密码。
 */
export function useUpdateMyPasswordMutation() {
  const { apiFetch } = useApiFetch()

  return useMutation<IResponse<boolean>, Error, UserUpdatePasswordForm>({
    mutationKey: ['user-update-my-password'],
    mutationFn: async data => await apiFetch<IResponse<boolean>>('/user/password/update', {
      method: 'post',
      body: data,
    }),
  })
}
