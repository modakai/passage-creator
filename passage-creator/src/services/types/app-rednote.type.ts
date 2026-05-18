import type { IPageResponse, IPaginationRequestQuery } from '@/services/types/response.type'

/**
 * 用户端创建小红书任务请求。
 */
export interface AppRednoteCreateRequest {
  content: string
}

/**
 * 小红书任务状态。
 */
export type RednoteStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'

/**
 * 小红书 workflow 阶段。
 */
export type RednotePhase
  = | 'PENDING'
    | 'SEARCH_AGENT'
    | 'COPY_GENERATING'
    | 'IMAGE_PROMPT_GENERATING'
    | 'IMAGE_GENERATING'
    | 'COMPLETED'
    | 'FAILED'

/**
 * SearchAgent 保存的搜索摘要。
 */
export interface RednoteSearchResult {
  title?: string
  summary?: string
  sourceName?: string
  sourceUrl?: string
}

/**
 * 普通配图提示词计划。
 */
export interface RednoteImagePromptItem {
  position?: number
  purpose?: string
  prompt?: string
  variants?: string[]
}

/**
 * 小红书图片生成结果。
 */
export interface RednoteImageResult {
  position?: number
  type?: 'NORMAL' | 'COVER' | string
  url?: string
  prompt?: string
  model?: string
  status?: 'SUCCESS' | 'FAILED' | string
  errorMessage?: string
}

/**
 * 小红书任务详情。
 */
export interface AppRednoteItem {
  id?: number
  taskId: string
  userId?: number
  content?: string
  subject?: string
  context?: string
  contentLength?: string
  targetWordCount?: number
  keywords?: string
  tagCount?: number
  imageCount?: number
  searchResults?: string
  bodyContent?: string
  tags?: string
  coverTitle?: string
  coverPrompt?: string
  imagePrompts?: string
  images?: string
  coverImage?: string
  status?: RednoteStatus
  statusLabel?: string
  phase?: RednotePhase
  phaseLabel?: string
  errorMessage?: string
  createTime?: string
  completedTime?: string
  updateTime?: string
}

/**
 * 用户端小红书分页响应。
 */
export type AppRednotePageResponse<T> = IPageResponse<T>

/**
 * 用户端小红书记录查询条件。
 */
export interface AppRednoteQuery extends IPaginationRequestQuery {
  page: number
  pageSize: number
  content?: string
  subject?: string
  status?: '' | RednoteStatus
  phase?: '' | RednotePhase
}

/**
 * 小红书 SSE 消息。
 */
export interface RednoteSseMessage<T = unknown> {
  type: string
  message?: string
  data?: T
  taskId?: string
  nodeType?: string
  payload?: Record<string, unknown>
}
