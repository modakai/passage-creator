<script lang="ts" setup>
import type { Component } from 'vue'

import { MoonIcon, SunIcon, SunMoonIcon } from '@lucide/vue'
import { storeToRefs } from 'pinia'
import { useI18n } from 'vue-i18n'

import type { AdminColorMode } from '@/constants/admin-appearance'

import { useAdminAppearancePreferencesStore } from '@/stores/admin-appearance-preferences'

const adminAppearanceStore = useAdminAppearancePreferencesStore()
const { preferences } = storeToRefs(adminAppearanceStore)
const { t } = useI18n()
const colorModes: {
  colorMode: AdminColorMode
  icon: Component
}[] = [
  { colorMode: 'light', icon: SunIcon },
  { colorMode: 'dark', icon: MoonIcon },
  { colorMode: 'system', icon: SunMoonIcon },
]

function setColorMode(colorMode: AdminColorMode) {
  adminAppearanceStore.updatePreference('colorMode', colorMode)
}
</script>

<template>
  <div class="space-y-1.5 pt-6">
    <UiLabel for="radius" class="text-xs">
      {{ t('pages.settings.appearancePreferences.colorMode') }}
    </UiLabel>
    <div class="grid grid-cols-3 gap-2 py-1.5">
      <UiButton
        v-for="item in colorModes" :key="item.colorMode"
        variant="outline"
        class="justify-center items-center h-8 px-3"
        :class="item.colorMode === preferences.colorMode ? 'border-foreground border-2' : ''"
        @click="setColorMode(item.colorMode)"
      >
        <component :is="item.icon" />
        <span class="text-xs">{{ t(`pages.settings.appearancePreferences.colorModes.${item.colorMode}`) }}</span>
      </UiButton>
    </div>
  </div>
</template>
