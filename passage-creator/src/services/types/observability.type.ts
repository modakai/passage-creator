import type { IPaginationRequestQuery } from '@/services/types/response.type'

/**
 * 运维状态等级。
 */
export type ObservabilityStatus = 'up' | 'degraded' | 'down' | 'unknown'

/**
 * 运维指标快照。
 */
export interface MetricSnapshot {
  name: string
  value: number
  unit: string
  used?: number
  total?: number
  usagePercent?: number
  status: ObservabilityStatus
}

/**
 * JVM 状态。
 */
export interface JvmStatus {
  heapMemory: MetricSnapshot
  nonHeapMemory: MetricSnapshot
  threadCount: number
  daemonThreadCount: number
  gcCount: number
  gcTimeMillis: number
  status: ObservabilityStatus
}

/**
 * 操作系统状态。
 */
export interface OsStatus {
  systemCpu: MetricSnapshot
  processCpu: MetricSnapshot
  memory: MetricSnapshot
  disk: MetricSnapshot
  status: ObservabilityStatus
}

/**
 * 依赖状态。
 */
export interface DependencyStatus {
  name: string
  status: ObservabilityStatus
  message?: string
  latencyMillis?: number
  activeConnections?: number
  idleConnections?: number
  totalConnections?: number
}

/**
 * 系统状态聚合数据。
 */
export interface SystemStatus {
  sampleTime: string
  overallStatus: ObservabilityStatus
  jvm: JvmStatus
  os: OsStatus
  database: DependencyStatus
  redis: DependencyStatus
}

/**
 * 运维事件类型。
 */
export type ObservabilityEventType
  = | 'slow_api'
    | 'api_error'
    | 'login_failure'
    | 'abnormal_ip'
    | 'force_logout'
    | 'security_alert'

/**
 * 运维事件。
 */
export interface ObservabilityEventItem {
  id: number
  eventType: ObservabilityEventType
  eventLevel?: string
  title?: string
  subject?: string
  requestPath?: string
  httpMethod?: string
  statusCode?: number
  durationMillis?: number
  userId?: number
  accountIdentifier?: string
  ipAddress?: string
  exceptionSummary?: string
  detail?: string
  auditLogId?: number
  notificationId?: number
  eventTime?: string
}

/**
 * 运维事件查询参数。
 */
export interface ObservabilityEventQuery extends IPaginationRequestQuery {
  page: number
  pageSize: number
  eventType?: ObservabilityEventType | ''
  eventLevel?: string
  requestPath?: string
  ipAddress?: string
  accountIdentifier?: string
  startTime?: string
  endTime?: string
}

/**
 * 接口质量摘要。
 */
export interface ApiSummary {
  slowApiCount: number
  errorCount: number
  averageSlowDurationMillis: number
}

/**
 * 错误趋势桶。
 */
export interface ErrorTrendBucket {
  bucket: string
  clientErrorCount: number
  serverErrorCount: number
  exceptionCount: number
}
