import type { AuthEntry } from '@/utils/auth-routing'

/**
 * 统一维护不同登录入口的展示差异，避免前后台页面各自散落文案判断。
 */
export interface AuthEntryConfig {
  entry: AuthEntry
  defaultAccount: string
  redirectPath: string
  titleKey: string
  descriptionKey: string
  submitKey: string
  badgeKey: string
  heroTitleKey: string
  heroDescriptionKey: string
  featureKeys: string[]
}

const AUTH_ENTRY_CONFIG_MAP: Record<AuthEntry, AuthEntryConfig> = {
  user: {
    entry: 'user',
    defaultAccount: 'student@example.com',
    redirectPath: '/',
    titleKey: 'pages.authPortal.user.title',
    descriptionKey: 'pages.authPortal.user.description',
    submitKey: 'pages.authPortal.user.submit',
    badgeKey: 'pages.authPortal.user.badge',
    heroTitleKey: 'pages.authPortal.user.heroTitle',
    heroDescriptionKey: 'pages.authPortal.user.heroDescription',
    featureKeys: [
      'pages.authPortal.user.features.creation',
      'pages.authPortal.user.features.resources',
      'pages.authPortal.user.features.switchAdmin',
    ],
  },
  admin: {
    entry: 'admin',
    defaultAccount: 'admin@example.com',
    redirectPath: '/dashboard',
    titleKey: 'pages.authPortal.admin.title',
    descriptionKey: 'pages.authPortal.admin.description',
    submitKey: 'pages.authPortal.admin.submit',
    badgeKey: 'pages.authPortal.admin.badge',
    heroTitleKey: 'pages.authPortal.admin.heroTitle',
    heroDescriptionKey: 'pages.authPortal.admin.heroDescription',
    featureKeys: [
      'pages.authPortal.admin.features.dashboard',
      'pages.authPortal.admin.features.permission',
      'pages.authPortal.admin.features.switchUser',
    ],
  },
}

/**
 * 用纯函数返回入口配置，既方便页面复用，也方便单元测试。
 */
export function getAuthEntryConfig(entry: AuthEntry): AuthEntryConfig {
  return AUTH_ENTRY_CONFIG_MAP[entry]
}
