import type { IPaginationRequestQuery } from './response.type'

/**
 * AI 用量查询参数。
 */
export interface AiUsageQuery extends IPaginationRequestQuery {
  page: number
  pageSize: number
  userId?: string
  taskId?: string
  agentName?: string
  provider?: string
  model?: string
  phase?: string
  requestType?: string
  startTime?: string
  endTime?: string
}

/**
 * AI 用量明细。
 */
export interface AiUsageRecordItem {
  id: number
  userId: string
  taskId?: string
  agentName: string
  phase?: string
  provider: string
  model: string
  requestType: string
  promptTokens: number
  completionTokens: number
  totalTokens: number
  creditCost: number
  latencyMs?: number
  responseOk?: boolean
  errorMessage?: string
  usedAt?: string
}

/**
 * AI 用量聚合条目。
 */
export interface AiUsageSummaryItem {
  label: string
  callCount: number
  totalTokens: number
  creditCost: number
}

/**
 * AI 用量总览。
 */
export interface AiUsageSummary {
  callCount: number
  promptTokens: number
  completionTokens: number
  totalTokens: number
  creditCost: number
  modelItems: AiUsageSummaryItem[]
  phaseItems: AiUsageSummaryItem[]
}

/**
 * 用户维度 AI 用量汇总。
 */
export interface AiUsageUserSummary {
  userId: string
  callCount: number
  totalTokens: number
  creditCost: number
}
