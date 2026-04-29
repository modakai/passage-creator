import type { IPageResponse, IPaginationRequestQuery } from '@/services/types/response.type'

/**
 * 文章状态值。
 */
export type ArticleStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'

/**
 * 文章分页响应。
 */
export type ArticlePageResponse<T> = IPageResponse<T>

/**
 * 文章列表和详情项。
 */
export interface ArticleItem {
  id: number
  taskId: string
  userId: number
  topic: string
  mainTitle?: string
  subTitle?: string
  outline?: string
  content?: string
  fullContent?: string
  coverImage?: string
  images?: string
  status: ArticleStatus
  errorMessage?: string
  createTime?: string
  completedTime?: string
  updateTime?: string
}

/**
 * 文章查询参数。
 */
export interface ArticleQuery extends IPaginationRequestQuery {
  page: number
  pageSize: number
  topic?: string
  title?: string
  status?: ArticleStatus | ''
  userId?: number
}

/**
 * 文章新增和编辑表单。
 */
export interface ArticleForm {
  id?: number
  topic: string
  mainTitle?: string
  subTitle?: string
  outline?: string
  content?: string
  fullContent?: string
  coverImage?: string
  images?: string
  status?: ArticleStatus
  errorMessage?: string
}
