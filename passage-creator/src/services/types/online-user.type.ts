import type { IPaginationRequestQuery } from '@/services/types/response.type'

/**
 * 在线用户查询参数。
 */
export interface OnlineUserQuery extends IPaginationRequestQuery {
  page: number
  pageSize: number
  userId?: number | string
  userAccount?: string
  userName?: string
  userRole?: string
  loginIp?: string
  loginStartTime?: string
  loginEndTime?: string
}

/**
 * 在线用户列表项。
 */
export interface OnlineUserItem {
  sessionId: string
  userId?: number | string
  userAccount?: string
  userName?: string
  userRole?: string
  loginIp?: string
  clientInfo?: string
  loginTime?: string
  lastAccessTime?: string
  expireTime?: string
}

/**
 * 强制下线请求。
 */
export interface OnlineUserForceLogoutRequest {
  sessionId: string
}
