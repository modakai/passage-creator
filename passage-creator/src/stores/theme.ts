import { defineStore } from 'pinia'

import type { ContentLayout, Radius, Theme } from '@/constants/themes'

import { useAdminAppearancePreferencesStore } from './admin-appearance-preferences'

export const useThemeStore = defineStore('system-config', () => {
  const adminAppearanceStore = useAdminAppearancePreferencesStore()

  // 兼容旧组件的字段名，实际状态统一存放在管理端外观偏好 store。
  const radius = computed({
    get: () => adminAppearanceStore.preferences.radius,
    set: (newRadius: Radius) => adminAppearanceStore.updatePreference('radius', newRadius),
  })
  function setRadius(newRadius: Radius) {
    adminAppearanceStore.updatePreference('radius', newRadius)
  }

  const theme = computed({
    get: () => adminAppearanceStore.preferences.theme,
    set: (newTheme: Theme) => adminAppearanceStore.updatePreference('theme', newTheme),
  })
  function setTheme(newTheme: Theme) {
    adminAppearanceStore.updatePreference('theme', newTheme)
  }

  const contentLayout = computed({
    get: () => adminAppearanceStore.preferences.contentLayout,
    set: (newContentLayout: ContentLayout) => adminAppearanceStore.updatePreference('contentLayout', newContentLayout),
  })
  function setContentLayout(newContentLayout: ContentLayout) {
    adminAppearanceStore.updatePreference('contentLayout', newContentLayout)
  }

  return {
    radius,
    setRadius,

    theme,
    setTheme,

    contentLayout,
    setContentLayout,
  }
})
