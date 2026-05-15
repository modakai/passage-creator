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

/**
 * Prompt 反馈采集环节。
 */
export type PromptFeedbackStage = 'TITLE_SELECTION' | 'OUTLINE_EDITING' | 'CONTENT_MERGED'

/**
 * Prompt 反馈评价结果。
 */
export type PromptFeedbackRating = 'VERY_SATISFIED' | 'SATISFIED' | 'NEUTRAL' | 'UNSATISFIED'

/**
 * Prompt 反馈提交请求。
 */
export interface PromptFeedbackSubmitRequest {
  taskId: string
  feedbackStage: PromptFeedbackStage
  rating: PromptFeedbackRating
  remark?: string
}

/**
 * Prompt 反馈记录条目。
 */
export interface PromptFeedbackItem {
  id: number
  userId: number
  taskId: string
  feedbackStage: PromptFeedbackStage
  feedbackStageLabel?: string
  rating: PromptFeedbackRating
  ratingLabel?: string
  remark?: string
  promptUsageLogId?: number
  promptTemplateId?: number
  templateKey?: string
  version?: string
  environment?: string
  promptLinked?: boolean
  createTime?: string
  updateTime?: string
}

/**
 * Prompt 反馈查询参数。
 */
export interface PromptFeedbackQuery extends IPaginationRequestQuery {
  page: number
  pageSize: number
  userId?: number
  taskId?: string
  feedbackStage?: PromptFeedbackStage | ''
  rating?: PromptFeedbackRating | ''
  templateKey?: string
  version?: string
  startTime?: string
  endTime?: string
}

/**
 * Prompt 反馈统计条目。
 */
export interface PromptFeedbackStatsItem {
  feedbackStage: PromptFeedbackStage
  feedbackStageLabel?: string
  verySatisfiedCount: number
  satisfiedCount: number
  neutralCount: number
  unsatisfiedCount: number
  totalCount: number
  verySatisfiedRatio: number | string
  satisfiedRatio: number | string
  neutralRatio: number | string
  unsatisfiedRatio: number | string
}
