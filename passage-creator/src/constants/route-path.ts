import type { RouteLocationRaw } from 'vue-router'

export const RouterPath: Record<string, RouteLocationRaw> = {
  HOME: '/',
  USER_LOGIN: '/auth/sign-in',
  ADMIN_HOME: '/dashboard',
  ADMIN_LOGIN: '/auth/sign-in',
} as const
