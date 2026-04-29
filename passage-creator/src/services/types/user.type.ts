import type { IPaginationRequestQuery } from '@/services/types/response.type'

export type UserEntityId = string | number

/**
 * 后台用户列表项。
 */
export interface UserItem {
  id: UserEntityId
  userAccount?: string
  userName?: string
  userAvatar?: string
  userProfile?: string
  userRole?: string
  status?: number
  createTime?: string
  updateTime?: string
}

/**
 * 用户分页查询参数。
 */
export interface UserQuery extends IPaginationRequestQuery {
  id?: UserEntityId
  unionId?: string
  mpOpenId?: string
  userName?: string
  userProfile?: string
  userRole?: string
  status?: number | ''
}

/**
 * 新增用户请求。
 */
export interface UserAddForm {
  userAccount: string
  userName?: string
  userAvatar?: string
  userProfile?: string
  userRole?: string
  status: number
}

/**
 * 更新用户请求。
 */
export interface UserUpdateForm {
  id: UserEntityId
  userName?: string
  userAvatar?: string
  userProfile?: string
  userRole?: string
  status: number
}

/**
 * 更新个人信息请求。
 */
export interface UserUpdateMyForm {
  userName?: string
  userAvatar?: string
  userProfile?: string
}

/**
 * 更新个人密码请求。
 */
export interface UserUpdatePasswordForm {
  oldPassword: string
  newPassword: string
  checkPassword: string
}

/**
 * 用户状态选项。
 */
export interface UserStatusOption {
  label: string
  value: number
  variant: 'default' | 'secondary' | 'destructive' | 'outline'
}
