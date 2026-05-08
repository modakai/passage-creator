import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'

import type {
  PromptTemplateForm,
  PromptTemplateItem,
  PromptTemplateQuery,
  PromptUsageLogItem,
  PromptUsageLogQuery,
} from '@/services/types/prompt-template.type'
import type { IPageResponse, IResponse } from '@/services/types/response.type'

import { useApiFetch } from '@/composables/use-fetch'

/**
 * 清理查询对象中的空字符串，避免后端把“全部”当成真实条件。
 */
function normalizeQuery<T extends Record<string, any>>(query: T) {
  return Object.fromEntries(
    Object.entries(query).filter(([, value]) => value !== '' && value !== undefined && value !== null),
  )
}

/**
 * 清理 Prompt 表单，空变量定义不发送空字符串给后端 JSON 字段。
 */
function normalizePromptTemplateForm(data: PromptTemplateForm) {
  const variablesSchema = data.variablesSchema?.trim()

  return {
    ...data,
    variablesSchema: variablesSchema || undefined,
  }
}

/**
 * 获取 Prompt 模板分页列表。
 */
export function useGetPromptTemplatePageQuery(query: PromptTemplateQuery) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<IPageResponse<PromptTemplateItem>>, Error>({
    queryKey: computed(() => ['prompt-template-page', { ...query }]),
    queryFn: async () => await apiFetch<IResponse<IPageResponse<PromptTemplateItem>>>('/prompt/template/list/page', {
      method: 'post',
      body: normalizeQuery(query),
    }),
  })
}

/**
 * 获取 Prompt 使用日志分页列表。
 */
export function useGetPromptUsageLogPageQuery(query: PromptUsageLogQuery) {
  const { apiFetch } = useApiFetch()

  return useQuery<IResponse<IPageResponse<PromptUsageLogItem>>, Error>({
    queryKey: computed(() => ['prompt-usage-log-page', { ...query }]),
    queryFn: async () => await apiFetch<IResponse<IPageResponse<PromptUsageLogItem>>>('/prompt/template/usage/list/page', {
      method: 'post',
      body: normalizeQuery(query),
    }),
  })
}

/**
 * 创建 Prompt 模板草稿。
 */
export function useCreatePromptTemplateMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<number>, Error, PromptTemplateForm>({
    mutationKey: ['prompt-template-create'],
    mutationFn: async data => await apiFetch<IResponse<number>>('/prompt/template/add', {
      method: 'post',
      body: normalizePromptTemplateForm(data),
    }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['prompt-template-page'] }),
  })
}

/**
 * 更新 Prompt 模板草稿。
 */
export function useUpdatePromptTemplateMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<boolean>, Error, PromptTemplateForm>({
    mutationKey: ['prompt-template-update'],
    mutationFn: async data => await apiFetch<IResponse<boolean>>('/prompt/template/update', {
      method: 'post',
      body: {
        id: data.id,
        content: data.content,
        variablesSchema: data.variablesSchema?.trim() || null,
        description: data.description,
      },
    }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['prompt-template-page'] }),
  })
}

/**
 * 执行 Prompt 模板状态动作。
 */
export function usePromptTemplateActionMutation(action: 'publish' | 'archive') {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<boolean>, Error, number>({
    mutationKey: ['prompt-template-action', action],
    mutationFn: async id => await apiFetch<IResponse<boolean>>(`/prompt/template/${action}`, {
      method: 'post',
      body: { id },
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['prompt-template-page'] })
      queryClient.invalidateQueries({ queryKey: ['prompt-usage-log-page'] })
    },
  })
}

/**
 * 删除非 ACTIVE Prompt 模板版本。
 */
export function useDeletePromptTemplateMutation() {
  const { apiFetch } = useApiFetch()
  const queryClient = useQueryClient()

  return useMutation<IResponse<boolean>, Error, number>({
    mutationKey: ['prompt-template-delete'],
    mutationFn: async id => await apiFetch<IResponse<boolean>>('/prompt/template/delete', {
      method: 'post',
      body: { id },
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['prompt-template-page'] })
      queryClient.invalidateQueries({ queryKey: ['prompt-usage-log-page'] })
    },
  })
}

/**
 * 按模板标识和环境查询已占用版本号。
 */
export function usePromptTemplateVersionsRequest() {
  const { apiFetch } = useApiFetch()

  return async (data: { templateKey: string, environment?: string }) => {
    return await apiFetch<IResponse<string[]>>('/prompt/template/versions', {
      method: 'post',
      body: data,
    })
  }
}

/**
 * 刷新 Prompt 模板运行时缓存。
 */
export function useRefreshPromptTemplateMutation() {
  const { apiFetch } = useApiFetch()

  return useMutation<IResponse<boolean>, Error, { templateKey: string, environment?: string }>({
    mutationKey: ['prompt-template-refresh'],
    mutationFn: async data => await apiFetch<IResponse<boolean>>('/prompt/template/refresh', {
      method: 'post',
      body: data,
    }),
  })
}
