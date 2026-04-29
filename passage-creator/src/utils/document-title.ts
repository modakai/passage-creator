import type { Router } from 'vue-router'

import { watch } from 'vue'

import type { NavGroup, NavItem } from '@/components/app-sidebar/types'

import { buildAdminNavGroups, buildOtherNavGroups, buildSettingsNavItems } from '@/composables/use-sidebar'
import { appLocale } from '@/plugins/i18n'
import { i18n } from '@/plugins/i18n/setup'

export const DEFAULT_DOCUMENT_TITLE = import.meta.env.VITE_APP_TITLE || 'Sakura Passage AI'

type TranslateFn = (key: string) => string

/**
 * 规范化路由路径，避免 /settings 与 /settings/ 这类路径无法匹配同一个菜单。
 */
function normalizePath(path: string) {
  if (path.length > 1 && path.endsWith('/')) {
    return path.slice(0, -1)
  }

  return path
}

/**
 * 从嵌套菜单中查找与当前路由匹配的菜单标题。
 */
function findTitleByPath(items: NavItem[], path: string): string | undefined {
  for (const item of items) {
    if ('url' in item && item.url && normalizePath(item.url) === path) {
      return item.title
    }

    if ('items' in item && item.items) {
      const childTitle = findTitleByPath(item.items, path)
      if (childTitle) {
        return childTitle
      }
    }
  }
}

/**
 * 汇总当前应用的可导航菜单，作为浏览器标签页标题的数据源。
 */
function buildTitleNavGroups(t: TranslateFn): NavGroup[] {
  return [
    ...buildAdminNavGroups(t),
    ...buildOtherNavGroups(t),
    {
      id: 'settings-pages',
      title: t('menu.settings.title'),
      items: buildSettingsNavItems(t),
    },
  ]
}

/**
 * 根据路由路径生成浏览器标签页标题，找不到菜单时回退到应用默认标题。
 */
export function buildDocumentTitle(path: string, t: TranslateFn) {
  const normalizedPath = normalizePath(path)
  const title = buildTitleNavGroups(t)
    .map(group => findTitleByPath(group.items, normalizedPath))
    .find(Boolean)

  return title ?? DEFAULT_DOCUMENT_TITLE
}

/**
 * 注册动态标题更新，覆盖菜单点击、命令面板跳转、前进后退和语言切换。
 */
export function setupDocumentTitle(router: Router) {
  // 用显式函数签名包一层，避免 vue-i18n 的复杂泛型传递到标题构建函数。
  const i18nGlobal = i18n.global as unknown as { t: TranslateFn }
  const translate: TranslateFn = key => i18nGlobal.t(key)

  const updateTitle = (path = router.currentRoute.value.path) => {
    document.title = buildDocumentTitle(path, translate)
  }

  router.afterEach((to) => {
    updateTitle(to.path)
  })

  watch(appLocale, () => {
    updateTitle()
  }, { immediate: true })
}
