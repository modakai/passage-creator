<script lang="ts" setup>
import { ChevronRightIcon, LayoutListIcon } from '@lucide/vue'
import { storeToRefs } from 'pinia'
import { useI18n } from 'vue-i18n'

import type { NavigationMode } from '@/stores/sidebar-config'

import { useSidebarConfigStore } from '@/stores/sidebar-config'

const sidebarConfigStore = useSidebarConfigStore()
const { navigationMode } = storeToRefs(sidebarConfigStore)
const { t } = useI18n()

const menuStyles: Array<{
  value: NavigationMode
  icon: any
}> = [
  {
    value: 'collapsible',
    icon: LayoutListIcon,
  },
  {
    value: 'vercel',
    icon: ChevronRightIcon,
  },
]

function handleMenuStyleChange(style: NavigationMode) {
  sidebarConfigStore.setNavigationMode(style)
}
</script>

<template>
  <div class="space-y-1.5 pt-6">
    <UiLabel for="menu-style" class="text-xs">
      {{ t('pages.settings.appearancePreferences.menuStyle') }}
    </UiLabel>
    <div class="grid grid-cols-2 gap-2 py-1.5">
      <UiButton
        v-for="style in menuStyles"
        :key="style.value"
        variant="outline"
        class="justify-center h-8 px-3"
        :class="navigationMode === style.value ? 'border-foreground border-2' : ''"
        :title="t(`pages.settings.appearancePreferences.navigationDescriptions.${style.value}`)"
        @click="handleMenuStyleChange(style.value)"
      >
        <component :is="style.icon" class="w-4 h-4" />
        {{ t(`pages.settings.appearancePreferences.navigationModes.${style.value}`) }}
      </UiButton>
    </div>
  </div>
</template>
