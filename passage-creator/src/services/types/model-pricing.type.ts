import type { IPaginationRequestQuery } from './response.type'

/**
 * AI 模型费率配置。
 */
export interface AiModelPricingItem {
  id: string
  provider: string
  model: string
  requestType: 'TEXT' | 'IMAGE' | string
  promptTokenPricePer1k: number
  completionTokenPricePer1k: number
  fixedCredits: number
  reserveCredits: number
  enabled: number
  createTime?: string
  updateTime?: string
}

/**
 * AI 模型费率查询参数。
 */
export interface AiModelPricingQuery extends IPaginationRequestQuery {
  page: number
  pageSize: number
  provider?: string
  model?: string
  requestType?: string
  enabled?: number | ''
}

/**
 * AI 模型费率保存表单。
 */
export interface AiModelPricingForm {
  id?: string
  provider: string
  model: string
  requestType: string
  promptTokenPricePer1k: number
  completionTokenPricePer1k: number
  fixedCredits: number
  reserveCredits: number
  enabled: number
}
