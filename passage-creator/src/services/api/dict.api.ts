import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'

import type {
  DictEntityId,
  DictItemForm,
  DictItemItem,
  DictItemQuery,
  DictOption,
  DictTypeForm,
  DictTypeItem,
  DictTypeQuery,
} from '@/services/types/dict.type'
import type { IPageResponse, IResponse } from '@/services/types/response.type'

import { useApiFetch } from '@/composables/use-fetch'
import {
  isDetailQueryEnabled,
  normalizeDictItemQuery,
  normalizeDictTypeQuery,
  normalizeEntityId,
} from '@/services/api/admin-query'

/**
 * 根据字典编码获取选项列表。
 */
export function useGetDictOptionsQuery(dictCode: string) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<DictOption[]>, Error>({
    queryKey: ['dict-options', dictCode],
    enabled: computed(() => Boolean(dictCode)),
    queryFn: async () => await apiFetch<IResponse<DictOption[]>>('/dict/map', {
      method: 'get',
      query: { dictCode },
    }),
  })
}

/**
 * 批量获取字典映射。
 */
export function useGetDictMapBatchMutation() {
  const { apiFetch } = useApiFetch()

  return useMutation<IResponse<Record<string, DictOption[]>>, Error, string[]>({
    mutationKey: ['dict-map-batch'],
    mutationFn: async dictCodes => await apiFetch<IResponse<Record<string, DictOption[]>>>('/dict/map/batch', {
      method: 'post',
      body: { dictCodes },
    }),
  })
}

/**
 * 根据编码和值获取字典标签。
 */
export function useGetDictLabelQuery(dictCode: string, value: string) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<string>, Error>({
    queryKey: ['dict-label', dictCode, value],
    enabled: computed(() => Boolean(dictCode && value)),
    queryFn: async () => await apiFetch<IResponse<string>>('/dict/label', {
      method: 'get',
      query: { dictCode, value },
    }),
  })
}

/**
 * 获取字典类型分页列表。
 */
export function useGetDictTypePageQuery(query: DictTypeQuery) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<IPageResponse<DictTypeItem>>, Error>({
    queryKey: ['dict-type-page', query.page, query.pageSize, query.dictCode, query.dictName, query.status],
    queryFn: async () => await apiFetch<IResponse<IPageResponse<DictTypeItem>>>('/dict/type/list/page', {
      method: 'post',
      body: normalizeDictTypeQuery(query),
    }),
  })
}

/**
 * 获取字典类型详情。
 */
export function useGetDictTypeDetailQuery(id: DictEntityId | null | undefined, enabled = true) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<DictTypeItem>, Error>({
    queryKey: ['dict-type-detail', id],
    enabled: computed(() => isDetailQueryEnabled(id, enabled)),
    queryFn: async () => await apiFetch<IResponse<DictTypeItem>>('/dict/type/get', {
      method: 'get',
      query: { id: normalizeEntityId(id!) },
    }),
  })
}

/**
 * 新增字典类型。
 */
export function useCreateDictTypeMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<number>, Error, DictTypeForm>({
    mutationKey: ['dict-type-create'],
    mutationFn: async data => await apiFetch<IResponse<number>>('/dict/type/add', {
      method: 'post',
      body: data,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dict-type-page'] })
    },
  })
}

/**
 * 更新字典类型。
 */
export function useUpdateDictTypeMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<boolean>, Error, DictTypeForm>({
    mutationKey: ['dict-type-update'],
    mutationFn: async data => await apiFetch<IResponse<boolean>>('/dict/type/update', {
      method: 'post',
      body: data,
    }),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['dict-type-page'] })
      queryClient.invalidateQueries({ queryKey: ['dict-type-detail', variables.id] })
    },
  })
}

/**
 * 删除字典类型。
 */
export function useDeleteDictTypeMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<boolean>, Error, DictEntityId>({
    mutationKey: ['dict-type-delete'],
    mutationFn: async id => await apiFetch<IResponse<boolean>>('/dict/type/delete', {
      method: 'post',
      body: { id: normalizeEntityId(id) },
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dict-type-page'] })
      queryClient.invalidateQueries({ queryKey: ['dict-item-page'] })
    },
  })
}

/**
 * 获取字典明细分页列表。
 */
export function useGetDictItemPageQuery(query: DictItemQuery) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<IPageResponse<DictItemItem>>, Error>({
    queryKey: ['dict-item-page', query.page, query.pageSize, query.dictTypeId, query.dictLabel, query.dictValue, query.status],
    enabled: computed(() => Boolean(query.dictTypeId)),
    queryFn: async () => await apiFetch<IResponse<IPageResponse<DictItemItem>>>('/dict/item/list/page', {
      method: 'post',
      body: normalizeDictItemQuery(query),
    }),
  })
}

/**
 * 获取字典明细详情。
 */
export function useGetDictItemDetailQuery(id: DictEntityId | null | undefined, enabled = true) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<DictItemItem>, Error>({
    queryKey: ['dict-item-detail', id],
    enabled: computed(() => isDetailQueryEnabled(id, enabled)),
    queryFn: async () => await apiFetch<IResponse<DictItemItem>>('/dict/item/get', {
      method: 'get',
      query: { id: normalizeEntityId(id!) },
    }),
  })
}

/**
 * 新增字典明细。
 */
export function useCreateDictItemMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<number>, Error, DictItemForm>({
    mutationKey: ['dict-item-create'],
    mutationFn: async data => await apiFetch<IResponse<number>>('/dict/item/add', {
      method: 'post',
      body: data,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dict-item-page'] })
    },
  })
}

/**
 * 更新字典明细。
 */
export function useUpdateDictItemMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<boolean>, Error, DictItemForm>({
    mutationKey: ['dict-item-update'],
    mutationFn: async data => await apiFetch<IResponse<boolean>>('/dict/item/update', {
      method: 'post',
      body: data,
    }),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['dict-item-page'] })
      queryClient.invalidateQueries({ queryKey: ['dict-item-detail', variables.id] })
    },
  })
}

/**
 * 删除字典明细。
 */
export function useDeleteDictItemMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<boolean>, Error, DictEntityId>({
    mutationKey: ['dict-item-delete'],
    mutationFn: async id => await apiFetch<IResponse<boolean>>('/dict/item/delete', {
      method: 'post',
      body: { id: normalizeEntityId(id) },
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dict-item-page'] })
    },
  })
}
