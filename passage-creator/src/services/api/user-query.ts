import type { UserEntityId } from '@/services/types/user.type'

/**
 * 判断用户详情查询是否应该启用，避免列表渲染时为每一行预取详情。
 */
export function shouldEnableUserDetailQuery(id: UserEntityId | null | undefined, enabled: boolean) {
  return Boolean(id) && enabled
}
