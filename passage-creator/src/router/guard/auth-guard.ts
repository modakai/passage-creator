import type { Router } from 'vue-router'

import { storeToRefs } from 'pinia'

import pinia from '@/plugins/pinia/setup'
import { useAuthStore } from '@/stores/auth'
import { resolveProtectedRedirect } from '@/utils/auth-routing'

export function setupAuthGuard(router: Router) {
  router.beforeEach((to) => {
    const authStore = useAuthStore(pinia)
    const { session } = storeToRefs(authStore)

    // 兼容旧后台路由：历史页面只有 auth 标记时，默认视为后台受保护页。
    const normalizedMeta = {
      ...to.meta,
      section: to.meta.section ?? (to.meta.auth ? 'admin' : undefined),
      requiresAdmin: to.meta.requiresAdmin ?? Boolean(to.meta.auth && !to.meta.section),
    }

    // 统一通过纯函数处理双入口登录和后台权限重定向。
    const redirectPath = resolveProtectedRedirect(normalizedMeta, session.value)
    if (redirectPath) {
      if (to.meta.guestOnly) {
        return { path: redirectPath }
      }

      // 未登录访问受保护页时先进入 401 页面，由页面提供统一的用户端登录按钮。
      if (!session.value.isLogin) {
        return {
          path: '/errors/401',
          query: {
            message: '请先登录后再访问该页面。',
            redirect: to.fullPath,
          },
        }
      }

      // 已登录但无权限进入后台时，直接进入 403 页面。
      if (normalizedMeta.requiresAdmin) {
        return { path: '/errors/403' }
      }

      return {
        path: redirectPath,
        query: { redirect: to.fullPath },
      }
    }
  })
}
