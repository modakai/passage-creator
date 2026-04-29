import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'

import type {
  NotificationForm,
  NotificationItem,
  NotificationPageResponse,
  NotificationQuery,
  NotificationReceiverType,
  NotificationTemplateForm,
  NotificationTemplateItem,
  NotificationTemplateQuery,
} from '@/services/types/notification.type'

import { useApiFetch } from '@/composables/use-fetch'

import type { IResponse } from '../types/response.type'

import { normalizeNotificationQuery } from './notification-query'

/**
 * 获取通知公告分页列表。
 */
export function useGetNotificationPageQuery(query: NotificationQuery) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<NotificationPageResponse<NotificationItem>>, Error>({
    queryKey: computed(() => ['notification-page', { ...query }]),
    queryFn: async () => await apiFetch<IResponse<NotificationPageResponse<NotificationItem>>>('/notification/list/page', {
      method: 'post',
      body: normalizeNotificationQuery(query),
    }),
  })
}

/**
 * 获取模板分页列表。
 */
export function useGetNotificationTemplatePageQuery(query: NotificationTemplateQuery) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<NotificationPageResponse<NotificationTemplateItem>>, Error>({
    queryKey: computed(() => ['notification-template-page', { ...query }]),
    queryFn: async () => await apiFetch<IResponse<NotificationPageResponse<NotificationTemplateItem>>>('/notification/template/list/page', {
      method: 'post',
      body: normalizeNotificationQuery(query),
    }),
  })
}

/**
 * 获取当前接收端消息列表。
 */
export function useGetClientMessagesQuery(receiverType: NotificationReceiverType) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<NotificationItem[]>, Error>({
    queryKey: ['notification-client-messages', receiverType],
    queryFn: async () => await apiFetch<IResponse<NotificationItem[]>>('/notification/client/messages', {
      method: 'get',
      query: { receiverType },
    }),
  })
}

/**
 * 获取当前接收端公告列表。
 */
export function useGetClientAnnouncementsQuery(receiverType: NotificationReceiverType) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<NotificationItem[]>, Error>({
    queryKey: ['notification-client-announcements', receiverType],
    queryFn: async () => await apiFetch<IResponse<NotificationItem[]>>('/notification/client/announcements', {
      method: 'get',
      query: { receiverType },
    }),
  })
}

/**
 * 获取未读通知数量。
 */
export function useGetUnreadCountQuery(receiverType: NotificationReceiverType) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<number>, Error>({
    queryKey: ['notification-unread-count', receiverType],
    queryFn: async () => await apiFetch<IResponse<number>>('/notification/client/unread/count', {
      method: 'get',
      query: { receiverType },
    }),
  })
}

/**
 * 创建通知公告。
 */
export function useCreateNotificationMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<number>, Error, NotificationForm>({
    mutationKey: ['notification-create'],
    mutationFn: async data => await apiFetch<IResponse<number>>('/notification/add', {
      method: 'post',
      body: data,
    }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notification-page'] }),
  })
}

/**
 * 更新通知公告。
 */
export function useUpdateNotificationMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<boolean>, Error, NotificationForm>({
    mutationKey: ['notification-update'],
    mutationFn: async data => await apiFetch<IResponse<boolean>>('/notification/update', {
      method: 'post',
      body: data,
    }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notification-page'] }),
  })
}

/**
 * 执行通知状态动作。
 */
export function useNotificationActionMutation(action: 'publish' | 'revoke' | 'archive') {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<boolean>, Error, number>({
    mutationKey: ['notification-action', action],
    mutationFn: async id => await apiFetch<IResponse<boolean>>(`/notification/${action}`, {
      method: 'post',
      body: { id },
    }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notification-page'] }),
  })
}

/**
 * 创建消息模板。
 */
export function useCreateNotificationTemplateMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<number>, Error, NotificationTemplateForm>({
    mutationKey: ['notification-template-create'],
    mutationFn: async data => await apiFetch<IResponse<number>>('/notification/template/add', {
      method: 'post',
      body: data,
    }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notification-template-page'] }),
  })
}

/**
 * 更新消息模板。
 */
export function useUpdateNotificationTemplateMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<boolean>, Error, NotificationTemplateForm>({
    mutationKey: ['notification-template-update'],
    mutationFn: async data => await apiFetch<IResponse<boolean>>('/notification/template/update', {
      method: 'post',
      body: data,
    }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notification-template-page'] }),
  })
}

/**
 * 启用或停用消息模板。
 */
export function useNotificationTemplateEnabledMutation(action: 'enable' | 'disable') {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<boolean>, Error, number>({
    mutationKey: ['notification-template-enabled', action],
    mutationFn: async id => await apiFetch<IResponse<boolean>>(`/notification/template/${action}`, {
      method: 'post',
      body: { id },
    }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notification-template-page'] }),
  })
}

/**
 * 标记单条消息已读。
 */
export function useMarkNotificationReadMutation(receiverType: NotificationReceiverType) {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<boolean>, Error, number>({
    mutationKey: ['notification-read', receiverType],
    mutationFn: async id => await apiFetch<IResponse<boolean>>('/notification/client/read', {
      method: 'post',
      query: { receiverType },
      body: { id },
    }),
    onSuccess: () => invalidateClientQueries(queryClient, receiverType),
  })
}

/**
 * 标记全部消息已读。
 */
export function useMarkAllNotificationsReadMutation(receiverType: NotificationReceiverType) {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<boolean>, Error, void>({
    mutationKey: ['notification-read-all', receiverType],
    mutationFn: async () => await apiFetch<IResponse<boolean>>('/notification/client/read/all', {
      method: 'post',
      query: { receiverType },
    }),
    onSuccess: () => invalidateClientQueries(queryClient, receiverType),
  })
}

/**
 * 关闭公告弹窗。
 */
export function useCloseAnnouncementMutation(receiverType: NotificationReceiverType) {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<boolean>, Error, number>({
    mutationKey: ['notification-announcement-close', receiverType],
    mutationFn: async id => await apiFetch<IResponse<boolean>>('/notification/client/announcement/close', {
      method: 'post',
      query: { receiverType },
      body: { id },
    }),
    onSuccess: () => invalidateClientQueries(queryClient, receiverType),
  })
}

/**
 * 刷新当前端通知缓存。
 */
function invalidateClientQueries(queryClient: ReturnType<typeof useQueryClient>, receiverType: NotificationReceiverType) {
  queryClient.invalidateQueries({ queryKey: ['notification-client-messages', receiverType] })
  queryClient.invalidateQueries({ queryKey: ['notification-client-announcements', receiverType] })
  queryClient.invalidateQueries({ queryKey: ['notification-unread-count', receiverType] })
}
