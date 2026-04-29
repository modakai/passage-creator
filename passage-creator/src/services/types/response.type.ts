export interface IResponse<T, E = Record<string, any>> {
  data: T
  extra: E
  code: number
  message: string
  success: boolean
}

/**
 * 通用分页响应结构。
 */
export interface IPageResponse<T> {
  records: T[]
  totalRow: number
  pageSize: number
  pageNumber: number
}

export interface IPaginationRequestQuery {
  page?: number
  pageSize?: number
  sortField?: string
  sortOrder?: string
}

export type IRequestQuery<T extends Record<string, any>> = {
  page?: number
  pageSize?: number
  sortField?: string
  sortOrder?: string
} & {
  [K in keyof T]?: T[K]
}
