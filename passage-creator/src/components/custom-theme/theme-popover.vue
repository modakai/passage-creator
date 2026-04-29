<script lang="ts" setup>
import { PaintbrushIcon } from '@lucide/vue'
import { useI18n } from 'vue-i18n'

import { Button } from '@/components/ui/button'
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover'

import ComponentStyle from './component-style.vue'
import ContentLayout from './content-layout.vue'
import CustomColor from './custom-color.vue'
import CustomRadius from './custom-radius.vue'
import CustomThemeTitle from './custom-theme-title.vue'
import MenuStyle from './menu-style.vue'
import ToggleColorMode from './toggle-color-mode.vue'

const { t } = useI18n()
</script>

<template>
  <Popover>
    <PopoverTrigger>
      <Button variant="outline" size="icon" :title="t('pages.settings.appearancePreferences.shortcut')">
        <PaintbrushIcon />
        <span class="sr-only">{{ t('pages.settings.appearancePreferences.shortcut') }}</span>
      </Button>
    </PopoverTrigger>
    <PopoverContent align="end" class="w-80">
      <CustomThemeTitle />
      <CustomColor />
      <CustomRadius />
      <ComponentStyle />
      <ToggleColorMode />
      <ContentLayout />
      <MenuStyle />
      <UiSeparator class="my-4" />
      <Button variant="outline" size="sm" class="w-full" as-child>
        <RouterLink to="/settings/appearance">
          {{ t('pages.settings.appearancePreferences.openFullSettings') }}
        </RouterLink>
      </Button>
    </PopoverContent>
  </Popover>
</template>
