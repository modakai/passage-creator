import type { IPaginationRequestQuery } from '@/services/types/response.type'

/**
 * 协议分页响应。
 */
export interface AgreementPageResponse<T> {
  records: T[]
  totalRow: number
  pageSize: number
  pageNumber: number
}

/**
 * 协议列表项。
 */
export interface AgreementItem {
  id: number
  agreementType: string
  title: string
  content: string
  status: number
  sortOrder: number
  remark?: string
  createTime?: string
  updateTime?: string
}

/**
 * 协议查询参数。
 */
export interface AgreementQuery extends IPaginationRequestQuery {
  page: number
  pageSize: number
  agreementType?: string
  title?: string
  status?: number | ''
}

/**
 * 协议表单参数。
 */
export interface AgreementForm {
  id?: number
  agreementType: string
  title: string
  content: string
  status: number
  sortOrder: number
  remark: string
}
