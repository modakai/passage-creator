export type AuthEntry = 'user' | 'admin'

export type UserRole = 'user' | 'admin'

export interface AuthUserInfo {
  id: string | number
  name: string
  email: string
  avatar?: string
  profile?: string
  role?: UserRole | string
  roles: UserRole[]
}

export interface AuthSession {
  isLogin: boolean
  loginEntry: AuthEntry
  token: string | null
  user: AuthUserInfo | null
}

export interface GuardMeta {
  auth?: boolean
  section?: 'user' | 'admin'
  requiresAdmin?: boolean
  guestOnly?: boolean
  authEntry?: AuthEntry
}

// 提供统一的游客态结构，供 store 初始化和守卫逻辑复用。
export function createGuestSession(): AuthSession {
  return {
    isLogin: false,
    loginEntry: 'user',
    token: null,
    user: null,
  }
}

// 管理后台访问资格完全由角色判断，不依赖入口来源。
export function canAccessAdmin(session: AuthSession): boolean {
  return session.isLogin && Boolean(session.user?.roles.includes('admin'))
}

// 登录成功后的默认落点只由实际角色决定，不再依赖用户从哪个登录入口进入。
export function getDefaultRedirectPath(session: AuthSession): string {
  if (canAccessAdmin(session)) {
    return '/dashboard'
  }

  return '/'
}

export function getLoginRoute(_section: 'user' | 'admin' = 'user'): string {
  // 前台与后台统一使用同一个登录路由，登录后再按角色分流。
  return '/auth/sign-in'
}

// 用纯函数统一表达守卫跳转规则，便于测试和路由复用。
export function resolveProtectedRedirect(meta: GuardMeta, session: AuthSession): string | null {
  if (meta.guestOnly && session.isLogin) {
    return getDefaultRedirectPath(session)
  }

  if (meta.auth && !session.isLogin) {
    return getLoginRoute(meta.section ?? 'user')
  }

  if (meta.requiresAdmin && !canAccessAdmin(session)) {
    return '/'
  }

  return null
}
