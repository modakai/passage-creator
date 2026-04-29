import type { Ref } from 'vue'

import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'

import type {
  AgreementForm,
  AgreementItem,
  AgreementPageResponse,
  AgreementQuery,
} from '@/services/types/agreement.type'
import type { DictOption } from '@/services/types/dict.type'

import { useApiFetch } from '@/composables/use-fetch'

import type { IResponse } from '../types/response.type'

/**
 * 协议类型对应字典类型中的 agreement 协议分类。
 */
export const AGREEMENT_TYPE_DICT_CODE = 'agreement'

/**
 * 获取协议分页列表。
 */
export function useGetAgreementPageQuery(query: AgreementQuery) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<AgreementPageResponse<AgreementItem>>, Error>({
    queryKey: ['agreement-page', query.page, query.pageSize, query.agreementType, query.title, query.status],
    queryFn: async () => await apiFetch<IResponse<AgreementPageResponse<AgreementItem>>>('/agreement/list/page', {
      method: 'post',
      body: {
        ...query,
        status: query.status === '' ? undefined : query.status,
      },
    }),
  })
}

/**
 * 获取协议详情。
 */
export function useGetAgreementDetailQuery(id: number | null | undefined) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<AgreementItem>, Error>({
    queryKey: computed(() => ['agreement-detail', id]),
    enabled: computed(() => !!id),
    queryFn: async () => await apiFetch<IResponse<AgreementItem>>('/agreement/get', {
      method: 'get',
      query: { id },
    }),
  })
}

/**
 * 获取公开协议详情。
 */
export function useGetPublicAgreementQuery(agreementType: string | Readonly<Ref<string>>) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<AgreementItem>, Error>({
    queryKey: computed(() => ['agreement-public-detail', toValue(agreementType)]),
    enabled: computed(() => !!toValue(agreementType)),
    queryFn: async () => await apiFetch<IResponse<AgreementItem>>('/agreement/public/get', {
      method: 'get',
      query: { agreementType: toValue(agreementType) },
    }),
  })
}

/**
 * 获取协议类型选项。
 */
export function useGetAgreementTypeOptionsQuery() {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<DictOption[]>, Error>({
    queryKey: ['agreement-type-options'],
    queryFn: async () => await apiFetch<IResponse<DictOption[]>>('/dict/map', {
      method: 'get',
      query: { dictCode: AGREEMENT_TYPE_DICT_CODE },
    }),
  })
}

/**
 * 新增协议。
 */
export function useCreateAgreementMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<number>, Error, AgreementForm>({
    mutationKey: ['agreement-create'],
    mutationFn: async data => await apiFetch<IResponse<number>>('/agreement/add', {
      method: 'post',
      body: data,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['agreement-page'] })
    },
  })
}

/**
 * 更新协议。
 */
export function useUpdateAgreementMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<boolean>, Error, AgreementForm>({
    mutationKey: ['agreement-update'],
    mutationFn: async data => await apiFetch<IResponse<boolean>>('/agreement/update', {
      method: 'post',
      body: data,
    }),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['agreement-page'] })
      queryClient.invalidateQueries({ queryKey: ['agreement-detail', variables.id] })
      queryClient.invalidateQueries({ queryKey: ['agreement-public-detail', variables.agreementType] })
    },
  })
}

/**
 * 删除协议。
 */
export function useDeleteAgreementMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<boolean>, Error, number>({
    mutationKey: ['agreement-delete'],
    mutationFn: async id => await apiFetch<IResponse<boolean>>('/agreement/delete', {
      method: 'post',
      body: { id },
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['agreement-page'] })
    },
  })
}
