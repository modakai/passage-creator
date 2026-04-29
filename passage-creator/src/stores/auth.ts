import { defineStore } from 'pinia'

import type { AuthSession } from '@/utils/auth-routing'

import {

  canAccessAdmin,
  createGuestSession,
} from '@/utils/auth-routing'

export const useAuthStore = defineStore('user', () => {
  // 使用统一 session 结构承载登录入口、用户信息和角色。
  const session = ref<AuthSession>(createGuestSession())

  const isLogin = computed(() => session.value.isLogin)
  const hasAdminAccess = computed(() => canAccessAdmin(session.value))

  function setSession(value: AuthSession) {
    session.value = value
  }

  function clearSession() {
    session.value = createGuestSession()
  }

  return {
    session,
    isLogin,
    hasAdminAccess,
    setSession,
    clearSession,
  }
}, {
  // 登录态必须跨浏览器重启保留，避免 localStorage 中有 token 时仍被路由守卫当作游客。
  persist: {
    storage: localStorage,
  },
})
