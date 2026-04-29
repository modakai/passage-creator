import { defineStore } from 'pinia'

import { useAdminAppearancePreferencesStore } from './admin-appearance-preferences'

export type NavigationMode = 'collapsible' | 'vercel'

/**
 * Sidebar configuration store
 * Manages user preferences for sidebar navigation mode
 */
export const useSidebarConfigStore = defineStore(
  'sidebar-config',
  () => {
    const adminAppearanceStore = useAdminAppearancePreferencesStore()

    // 保留旧 store API，实际菜单风格由管理端外观偏好统一维护。
    const navigationMode = computed({
      get: () => adminAppearanceStore.preferences.navigationMode,
      set: (mode: NavigationMode) => adminAppearanceStore.updatePreference('navigationMode', mode),
    })

    function setNavigationMode(mode: NavigationMode) {
      adminAppearanceStore.updatePreference('navigationMode', mode)
    }

    return {
      navigationMode,
      setNavigationMode,
    }
  },
)
