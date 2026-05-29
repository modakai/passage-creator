import { computed, reactive } from 'vue'

export const TOKEN_STORAGE_KEY = 'sakura_user_token'
export const USER_STORAGE_KEY = 'sakura_user_profile'

export interface LoginUser {
  id: string | number
  userAccount?: string
  userName?: string
  userAvatar?: string
  userProfile?: string
  userRole?: string
  token?: string
  status?: number
  createTime?: string
  updateTime?: string
}

interface SessionState {
  token: string
  user: LoginUser | null
}

export const sessionState = reactive<SessionState>({
  token: window.localStorage.getItem(TOKEN_STORAGE_KEY) || '',
  user: readStoredUser(),
})

export const isLoggedIn = computed(() => Boolean(sessionState.token))

/**
 * 从 localStorage 读取用户快照，坏数据直接丢弃，避免阻塞应用启动。
 */
function readStoredUser() {
  const value = window.localStorage.getItem(USER_STORAGE_KEY)
  if (!value) {
    return null
  }
  try {
    return JSON.parse(value) as LoginUser
  }
  catch {
    window.localStorage.removeItem(USER_STORAGE_KEY)
    return null
  }
}

/**
 * 保存登录态，token 用于后续后端接口鉴权，用户快照用于导航和个人中心展示。
 */
export function setSession(user: LoginUser) {
  sessionState.token = user.token || ''
  sessionState.user = user
  if (user.token) {
    window.localStorage.setItem(TOKEN_STORAGE_KEY, user.token)
  }
  window.localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user))
}

/**
 * 清理本地登录态，后端注销失败时也要保证前端不继续携带旧 token。
 */
export function clearSession() {
  sessionState.token = ''
  sessionState.user = null
  window.localStorage.removeItem(TOKEN_STORAGE_KEY)
  window.localStorage.removeItem(USER_STORAGE_KEY)
}
