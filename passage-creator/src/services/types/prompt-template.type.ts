import type { IPaginationRequestQuery } from '@/services/types/response.type'

/**
 * Prompt 模板状态。
 */
export type PromptTemplateStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED'

/**
 * Prompt 模板版本条目。
 */
export interface PromptTemplateItem {
  id: number
  templateKey: string
  version: string
  content: string
  variablesSchema?: string
  description?: string
  status: PromptTemplateStatus
  environment: string
  createdBy?: string
  publishedBy?: string
  publishedAt?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * Prompt 模板分页查询参数。
 */
export interface PromptTemplateQuery extends IPaginationRequestQuery {
  page: number
  pageSize: number
  templateKey?: string
  version?: string
  status?: PromptTemplateStatus | ''
  environment?: string
}

/**
 * Prompt 模板表单。
 */
export interface PromptTemplateForm {
  id?: number
  templateKey: string
  version: string
  content: string
  variablesSchema?: string
  description?: string
  environment: string
}

/**
 * Prompt 使用日志条目。
 */
export interface PromptUsageLogItem {
  id: number
  promptTemplateId?: number
  templateKey: string
  version: string
  environment: string
  agentName: string
  taskId?: string
  userId?: number
  usedAt?: string
  responseOk?: boolean
  errorMessage?: string
  latencyMs?: number
  feedback?: number
}

/**
 * Prompt 使用日志分页查询参数。
 */
export interface PromptUsageLogQuery extends IPaginationRequestQuery {
  page: number
  pageSize: number
  templateKey?: string
  agentName?: string
  taskId?: string
  environment?: string
  responseOk?: boolean | ''
}
