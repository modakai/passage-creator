import { defineStore } from 'pinia'

import type { AdminAppearancePreferences } from '@/constants/admin-appearance'

import {
  DEFAULT_ADMIN_APPEARANCE_PREFERENCES,
} from '@/constants/admin-appearance'

function cloneDefaultPreferences(): AdminAppearancePreferences {
  // 默认配置只包含原始值，浅拷贝即可避免重置时共享引用。
  return { ...DEFAULT_ADMIN_APPEARANCE_PREFERENCES }
}

export const useAdminAppearancePreferencesStore = defineStore('admin-appearance-preferences', () => {
  const preferences = ref<AdminAppearancePreferences>(cloneDefaultPreferences())

  function updatePreferences(nextPreferences: Partial<AdminAppearancePreferences>) {
    preferences.value = {
      ...preferences.value,
      ...nextPreferences,
    }
  }

  function updatePreference<Key extends keyof AdminAppearancePreferences>(
    key: Key,
    value: AdminAppearancePreferences[Key],
  ) {
    preferences.value = {
      ...preferences.value,
      [key]: value,
    }
  }

  function resetPreferences() {
    preferences.value = cloneDefaultPreferences()
  }

  return {
    preferences,
    updatePreference,
    updatePreferences,
    resetPreferences,
  }
}, {
  // 外观偏好属于当前浏览器的长期设置，关闭浏览器后仍需要从 localStorage 恢复。
  persist: {
    storage: localStorage,
  },
})
