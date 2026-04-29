import type { LoginUser } from '@/services/types/auth.type'
import type { AuthEntry, AuthSession, UserRole } from '@/utils/auth-routing'

/**
 * 将后端单角色字段转换为前端角色数组，兼容用户端与后台共用导航。
 */
function resolveRoles(userRole: string | undefined): UserRole[] {
  if (userRole === 'admin') {
    return ['admin', 'user']
  }

  return ['user']
}

/**
 * 将后端登录返回值映射为前端统一 session。
 */
export function buildAuthSessionFromLoginUser(loginUser: LoginUser, entry: AuthEntry, currentToken?: string | null): AuthSession {
  return {
    isLogin: true,
    loginEntry: entry,
    // 刷新登录态接口不会重新下发 token，因此优先保留当前 token。
    token: loginUser.token ?? currentToken ?? null,
    user: {
      id: loginUser.id,
      name: loginUser.userName || loginUser.userAccount || '未命名用户',
      email: loginUser.userAccount || '',
      avatar: loginUser.userAvatar,
      profile: loginUser.userProfile,
      role: loginUser.userRole,
      roles: resolveRoles(loginUser.userRole),
    },
  }
}
