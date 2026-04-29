import type { IPaginationRequestQuery } from '@/services/types/response.type'

/**
 * 分页响应结构。
 */
export interface NotificationPageResponse<T> {
  records: T[]
  totalRow: number
  pageSize: number
  pageNumber: number
}

/**
 * 通知公告类型。
 */
export type NotificationType = 'message' | 'announcement'

/**
 * 接收端范围。
 */
export type NotificationReceiverType = 'admin' | 'app' | 'all'

/**
 * 目标范围。
 */
export type NotificationTargetType = 'all' | 'role' | 'user'

/**
 * 发布状态。
 */
export type NotificationStatus = 'draft' | 'published' | 'revoked' | 'archived'

/**
 * 通知公告条目。
 */
export interface NotificationItem {
  id: number
  type: NotificationType
  title: string
  summary?: string
  content: string
  level?: string
  status: NotificationStatus
  receiverType: NotificationReceiverType
  targetType: NotificationTargetType
  targetRoles?: string[]
  targetUserIds?: number[]
  pinned?: number
  popup?: number
  read?: boolean
  linkUrl?: string
  effectiveTime?: string
  expireTime?: string
  publishTime?: string
  createTime?: string
  updateTime?: string
}

/**
 * 通知公告查询参数。
 */
export interface NotificationQuery extends IPaginationRequestQuery {
  page: number
  pageSize: number
  type?: NotificationType | ''
  title?: string
  status?: NotificationStatus | ''
  receiverType?: NotificationReceiverType | ''
  targetType?: NotificationTargetType | ''
}

/**
 * 通知公告表单。
 */
export interface NotificationForm {
  id?: number
  type: NotificationType
  title: string
  summary?: string
  content: string
  level?: string
  receiverType: NotificationReceiverType
  targetType: NotificationTargetType
  targetRoles?: string[]
  targetUserIds?: number[]
  pinned?: number
  popup?: number
  linkUrl?: string
  effectiveTime?: string
  expireTime?: string
}

/**
 * 消息模板条目。
 */
export interface NotificationTemplateItem {
  id: number
  templateCode: string
  eventType: string
  titleTemplate: string
  contentTemplate: string
  variableSchema?: string
  receiverType: NotificationReceiverType
  enabled: number
  remark?: string
  createTime?: string
  updateTime?: string
}

/**
 * 消息模板查询参数。
 */
export interface NotificationTemplateQuery extends IPaginationRequestQuery {
  page: number
  pageSize: number
  templateCode?: string
  eventType?: string
  enabled?: number | ''
}

/**
 * 消息模板表单。
 */
export interface NotificationTemplateForm {
  id?: number
  templateCode: string
  eventType: string
  titleTemplate: string
  contentTemplate: string
  variableSchema?: string
  receiverType: NotificationReceiverType
  enabled: number
  remark?: string
}
