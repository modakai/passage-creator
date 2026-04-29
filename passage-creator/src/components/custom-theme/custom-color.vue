<script lang="ts" setup>
import { storeToRefs } from 'pinia'
import { useI18n } from 'vue-i18n'

import { THEME_PRIMARY_COLORS } from '@/constants/themes'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()
const { setTheme } = themeStore
const { theme: t } = storeToRefs(themeStore)
const { t: translate } = useI18n()
</script>

<template>
  <div class="space-y-1.5 pt-6">
    <UiLabel for="radius" class="text-xs">
      {{ translate('pages.settings.appearancePreferences.palette') }}
    </UiLabel>
    <div class="grid grid-cols-2 gap-2 py-1.5">
      <UiButton
        v-for="theme in THEME_PRIMARY_COLORS" :key="theme.theme"
        variant="outline"
        class="justify-center h-8 px-3"
        :class="t === theme.theme ? 'border-foreground border-2' : ''"
        @click="setTheme(theme.theme)"
      >
        <span
          :style="{
            '--theme-primary': theme.primaryColor,
          }"
          class="size-2 rounded-full bg-(--theme-primary)"
        />
        <span class="text-xs">{{ translate(`pages.settings.appearancePreferences.themes.${theme.theme}`) }}</span>
      </UiButton>
    </div>
  </div>
</template>
