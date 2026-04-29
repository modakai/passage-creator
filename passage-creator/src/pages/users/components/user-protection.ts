import type { UserItem } from '@/services/types/user.type'

export const PROTECTED_SUPER_ADMIN_ACCOUNT = 'sakura'

/**
 * 内置超级管理员账号不允许在用户管理中删除。
 */
export function canDeleteUser(user: Pick<UserItem, 'userAccount'>) {
  return user.userAccount !== PROTECTED_SUPER_ADMIN_ACCOUNT
}

/**
 * 内置超级管理员账号不允许在用户管理中禁用。
 */
export function canToggleUserStatus(user: Pick<UserItem, 'userAccount'>) {
  return user.userAccount !== PROTECTED_SUPER_ADMIN_ACCOUNT
}
