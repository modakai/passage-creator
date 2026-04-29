<script setup lang="ts">
import { MoonIcon, SunIcon, SunMoonIcon } from '@lucide/vue'
import { useColorMode } from '@vueuse/core'
import { useI18n } from 'vue-i18n'

const mode = useColorMode()
const { t } = useI18n()
</script>

<template>
  <UiDropdownMenu>
    <UiDropdownMenuTrigger as-child>
      <UiButton variant="outline" size="icon" :title="t('pages.settings.appearancePreferences.colorMode')">
        <MoonIcon class=" rotate-0 scale-100 transition-all dark:-rotate-90 dark:scale-0" />
        <SunIcon class="absolute  rotate-90 scale-0 transition-all dark:rotate-0 dark:scale-100" />
        <span class="sr-only">{{ t('pages.settings.appearancePreferences.colorMode') }}</span>
      </UiButton>
    </UiDropdownMenuTrigger>
    <UiDropdownMenuContent align="end">
      <UiDropdownMenuItem @click="mode = 'light'">
        <SunIcon />
        {{ t('pages.settings.appearancePreferences.colorModes.light') }}
      </UiDropdownMenuItem>
      <UiDropdownMenuItem @click="mode = 'dark'">
        <MoonIcon />
        {{ t('pages.settings.appearancePreferences.colorModes.dark') }}
      </UiDropdownMenuItem>
      <UiDropdownMenuItem @click="mode = 'auto'">
        <SunMoonIcon />
        {{ t('pages.settings.appearancePreferences.colorModes.system') }}
      </UiDropdownMenuItem>
    </UiDropdownMenuContent>
  </UiDropdownMenu>
</template>
