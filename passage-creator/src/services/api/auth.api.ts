import type { LoginPayload, LoginUser, RegisterPayload } from '@/services/types/auth.type'
import type { IResponse } from '@/services/types/response.type'

import { useApiFetch } from '@/composables/use-fetch'

/**
 * 调用后端用户登录接口。
 */
export async function loginByPassword(payload: LoginPayload) {
  const { apiFetch } = useApiFetch()

  return await apiFetch<IResponse<LoginUser>>('/user/login', {
    method: 'post',
    body: {
      userAccount: payload.userAccount,
      userPassword: payload.userPassword,
    },
  })
}

/**
 * 调用后端用户注册接口。
 */
export async function registerUser(payload: RegisterPayload) {
  const { apiFetch } = useApiFetch()

  return await apiFetch<IResponse<number>>('/user/register', {
    method: 'post',
    body: payload,
  })
}

/**
 * 获取当前登录用户。
 */
export async function getLoginUserRequest() {
  const { apiFetch } = useApiFetch()

  return await apiFetch<IResponse<LoginUser>>('/user/get/login', {
    method: 'get',
  })
}

/**
 * 调用后端退出登录接口。
 */
export async function logoutRequest() {
  const { apiFetch } = useApiFetch()

  return await apiFetch<IResponse<boolean>>('/user/logout', {
    method: 'post',
  })
}
