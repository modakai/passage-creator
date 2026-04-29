import { storeToRefs } from 'pinia'

import type { LoginPayload } from '@/services/types/auth.type'

import { getLoginUserRequest, loginByPassword, logoutRequest } from '@/services/api/auth.api'
import { useAuthStore } from '@/stores/auth'
import {
  canAccessAdmin,
  getDefaultRedirectPath,
  getLoginRoute,
} from '@/utils/auth-routing'
import { buildAuthSessionFromLoginUser } from '@/utils/auth-session'

export function useAuth() {
  const router = useRouter()

  const authStore = useAuthStore()
  const { hasAdminAccess, isLogin, session } = storeToRefs(authStore)
  const loading = ref(false)

  async function logout() {
    try {
      await logoutRequest()
    }
    finally {
      authStore.clearSession()
    }

    router.push({ path: getLoginRoute() })
  }

  /**
   * 刷新当前登录态，适合页面初始化后兜底同步。
   */
  async function refreshLoginUser() {
    const response = await getLoginUserRequest()
    authStore.setSession(buildAuthSessionFromLoginUser(
      response.data,
      authStore.session.loginEntry,
      authStore.session.token,
    ))
    return response.data
  }

  async function login(payload: LoginPayload) {
    loading.value = true
    try {
      const response = await loginByPassword(payload)
      const nextSession = buildAuthSessionFromLoginUser(response.data, payload.entry)

      authStore.setSession(nextSession)
      const redirect = router.currentRoute.value.query.redirect as string | undefined
      // 登录页统一后，避免普通用户因后台 redirect 被送进无权限页面。
      if (redirect && !redirect.startsWith('//') && (!redirect.startsWith('/dashboard') || canAccessAdmin(nextSession))) {
        await router.push(redirect)
        return
      }

      await router.push(getDefaultRedirectPath(nextSession))
    }
    finally {
      loading.value = false
    }
  }

  return {
    loading,
    logout,
    login,
    refreshLoginUser,
    isLogin,
    hasAdminAccess,
    session,
  }
}
