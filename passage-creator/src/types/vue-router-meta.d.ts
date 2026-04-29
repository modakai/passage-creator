import 'vue-router'

export {}

declare module 'vue-router' {
  interface RouteMeta {
    // 是否需要登录。
    auth?: boolean
    // 标识该页面属于用户端还是后台，用于选择登录入口。
    section?: 'user' | 'admin'
    // 是否需要管理员角色。
    requiresAdmin?: boolean
    // 是否仅允许游客访问。
    guestOnly?: boolean
    // 记录登录页入口来源，决定登录后的默认跳转。
    authEntry?: 'user' | 'admin'
  }
}
