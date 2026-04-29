<script lang="ts" setup>
import { storeToRefs } from 'pinia'
import { useI18n } from 'vue-i18n'

import type { AdminComponentStyle } from '@/constants/admin-appearance'

import { ADMIN_COMPONENT_STYLE_OPTIONS } from '@/constants/admin-appearance'
import { useAdminAppearancePreferencesStore } from '@/stores/admin-appearance-preferences'

const adminAppearanceStore = useAdminAppearancePreferencesStore()
const { preferences } = storeToRefs(adminAppearanceStore)
const { t } = useI18n()

function setComponentStyle(componentStyle: AdminComponentStyle) {
  // 快捷弹层和完整外观设置共用同一份持久化偏好。
  adminAppearanceStore.updatePreference('componentStyle', componentStyle)
}
</script>

<template>
  <div class="space-y-1.5 pt-6">
    <UiLabel class="text-xs">
      {{ t('pages.settings.appearancePreferences.componentStyle') }}
    </UiLabel>
    <div class="grid grid-cols-2 gap-2 py-1.5">
      <UiButton
        v-for="style in ADMIN_COMPONENT_STYLE_OPTIONS"
        :key="style.value"
        variant="outline"
        class="h-8 justify-center px-3"
        :class="preferences.componentStyle === style.value ? 'border-foreground border-2' : ''"
        :title="t(`pages.settings.appearancePreferences.componentStyleDescriptions.${style.value}`)"
        @click="setComponentStyle(style.value)"
      >
        <span class="truncate text-xs">
          {{ t(`pages.settings.appearancePreferences.componentStyles.${style.value}`) }}
        </span>
      </UiButton>
    </div>
  </div>
</template>
