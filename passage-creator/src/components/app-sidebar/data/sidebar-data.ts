import type { AuthUserInfo } from '@/utils/auth-routing'

import { useAuth } from '@/composables/use-auth'
import { useSidebar } from '@/composables/use-sidebar'

import type { SidebarData, User } from '../types'

/**
 * 将登录 session 中的用户信息压缩为侧边栏展示字段。
 */
export function resolveSidebarUser(loginUser: AuthUserInfo | null): User {
  return {
    name: loginUser?.name || '未登录用户',
    email: loginUser?.email || '未绑定账号',
    avatar: loginUser?.avatar || '',
  }
}

export function useSidebarData() {
  const { navData } = useSidebar()
  const { session } = useAuth()

  const sidebarData = computed<SidebarData>(() => ({
    // 管理侧边栏始终展示当前登录用户，避免继续显示模板默认账号。
    user: resolveSidebarUser(session.value.user),
    navMain: navData.value,
  }))

  return {
    sidebarData,
  }
}
