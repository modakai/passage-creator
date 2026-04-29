import type { AuthEntry, UserRole } from '@/utils/auth-routing'

/**
 * 登录表单请求。
 */
export interface LoginPayload {
  userAccount: string
  userPassword: string
  entry: AuthEntry
}

/**
 * 注册请求。
 */
export interface RegisterPayload {
  userAccount: string
  userPassword: string
  checkPassword: string
}

/**
 * 后端登录用户响应。
 */
export interface LoginUser {
  id: string | number
  userAccount?: string
  userName?: string
  userAvatar?: string
  userProfile?: string
  userRole?: UserRole | string
  token?: string
  createTime?: string
  updateTime?: string
}
