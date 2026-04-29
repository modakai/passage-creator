import type { IPaginationRequestQuery } from '@/services/types/response.type'

/**
 * 审计日志分页响应结构。
 */
export interface AuditLogPageResponse<T> {
  records: T[]
  totalRow: number
  pageSize: number
  pageNumber: number
}

/**
 * 审计日志类型。
 */
export type AuditLogType = 'login' | 'admin_operation'

/**
 * 审计日志结果。
 */
export type AuditLogResult = 'success' | 'failure'

/**
 * 审计日志条目。
 */
export interface AuditLogItem {
  id: number
  logType: AuditLogType
  userId?: number
  accountIdentifier?: string
  ipAddress?: string
  clientInfo?: string
  requestPath?: string
  httpMethod?: string
  operationDescription?: string
  businessModule?: string
  operationType?: string
  costMillis?: number
  result: AuditLogResult
  statusCode?: number
  failureReason?: string
  exceptionSummary?: string
  requestSummary?: string
  responseSummary?: string
  traceId?: string
  auditTime?: string
}

/**
 * 审计日志查询参数。
 */
export interface AuditLogQuery extends IPaginationRequestQuery {
  page: number
  pageSize: number
  logType?: AuditLogType | ''
  userId?: number | string
  accountIdentifier?: string
  ipAddress?: string
  requestPath?: string
  httpMethod?: string
  result?: AuditLogResult | ''
  operationDescription?: string
  businessModule?: string
  operationType?: string
  auditStartTime?: string
  auditEndTime?: string
}

/**
 * 审计日志导出参数。
 */
export interface AuditLogExportQuery extends AuditLogQuery {
  exportLimit?: number
}
