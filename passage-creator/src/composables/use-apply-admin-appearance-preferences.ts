import { storeToRefs } from 'pinia'

import type { AdminAppearancePreferences } from '@/constants/admin-appearance'

import {
  ADMIN_COMPONENT_STYLES,
  ADMIN_DENSITIES,
  ADMIN_FONTS,
  ADMIN_THEMES,
  DEFAULT_ADMIN_APPEARANCE_PREFERENCES,
} from '@/constants/admin-appearance'
import { useAdminAppearancePreferencesStore } from '@/stores/admin-appearance-preferences'

export interface AdminAppearanceEffects {
  addClasses: string[]
  removeClasses: string[]
  cssVariables: Record<string, string>
}

const OPTIONAL_EFFECT_CLASSES = [
  'dark',
  'motion-reduced',
  'tables-striped',
  'breadcrumbs-hidden',
  'page-title-hidden',
]

const ALL_ADMIN_APPEARANCE_CLASSES = [
  ...ADMIN_THEMES.map(theme => `theme-${theme}`),
  ...ADMIN_FONTS.map(font => `font-${font}`),
  ...ADMIN_DENSITIES.map(density => `density-${density}`),
  ...ADMIN_COMPONENT_STYLES.map(style => `style-${style}`),
  ...OPTIONAL_EFFECT_CLASSES,
]

export function resolveAdminAppearanceEffects(
  preferences: AdminAppearancePreferences,
  isSystemDark: boolean,
): AdminAppearanceEffects {
  const isDark = preferences.colorMode === 'dark' || (preferences.colorMode === 'system' && isSystemDark)
  const themeClasses = ADMIN_THEMES.map(theme => `theme-${theme}`)
  const fontClasses = ADMIN_FONTS.map(font => `font-${font}`)
  const densityClasses = ADMIN_DENSITIES.map(density => `density-${density}`)
  // 持久化数据可能来自旧版本，运行时兜底避免出现 style-undefined。
  const componentStyle = preferences.componentStyle ?? DEFAULT_ADMIN_APPEARANCE_PREFERENCES.componentStyle
  const componentStyleClasses = ADMIN_COMPONENT_STYLES.map(style => `style-${style}`)
  const addClasses = [
    `theme-${preferences.theme}`,
    `font-${preferences.font}`,
    `density-${preferences.density}`,
    `style-${componentStyle}`,
  ]

  if (isDark)
    addClasses.push('dark')
  if (preferences.reducedMotion)
    addClasses.push('motion-reduced')
  if (preferences.stripedTables)
    addClasses.push('tables-striped')
  if (!preferences.showBreadcrumbs)
    addClasses.push('breadcrumbs-hidden')
  if (!preferences.showPageTitle)
    addClasses.push('page-title-hidden')

  return {
    addClasses,
    removeClasses: [
      ...themeClasses.filter(className => className !== `theme-${preferences.theme}`),
      ...fontClasses.filter(className => className !== `font-${preferences.font}`),
      ...densityClasses.filter(className => className !== `density-${preferences.density}`),
      ...componentStyleClasses.filter(className => className !== `style-${componentStyle}`),
      ...OPTIONAL_EFFECT_CLASSES.filter(className => !addClasses.includes(className)),
    ],
    cssVariables: {
      '--radius': `${preferences.radius}rem`,
    },
  }
}

function applyEffectsToRoot(root: HTMLElement, effects: AdminAppearanceEffects) {
  // 先移除互斥类再添加目标类，避免主题和密度状态叠加。
  root.classList.remove(...effects.removeClasses)
  root.classList.add(...effects.addClasses)
  Object.entries(effects.cssVariables).forEach(([name, value]) => {
    root.style.setProperty(name, value)
  })
}

export function useApplyAdminAppearancePreferences() {
  const store = useAdminAppearancePreferencesStore()
  const { preferences } = storeToRefs(store)
  const isSystemDark = ref(false)

  function syncSystemDark() {
    isSystemDark.value = window.matchMedia('(prefers-color-scheme: dark)').matches
  }

  onMounted(() => {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    syncSystemDark()

    // system 模式需要响应系统主题变化，其他模式也保持监听以简化状态同步。
    mediaQuery.addEventListener('change', syncSystemDark)
    onScopeDispose(() => {
      mediaQuery.removeEventListener('change', syncSystemDark)
    })
  })

  watchEffect(() => {
    const root = document.documentElement
    const effects = resolveAdminAppearanceEffects(preferences.value ?? DEFAULT_ADMIN_APPEARANCE_PREFERENCES, isSystemDark.value)
    applyEffectsToRoot(root, effects)
  })

  onScopeDispose(() => {
    // 离开后台布局时移除管理端专属外观类，避免影响用户端页面。
    document.documentElement.classList.remove(...ALL_ADMIN_APPEARANCE_CLASSES)
  })

  return {
    preferences,
  }
}
