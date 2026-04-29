<script setup lang="ts">
import { RotateCcwIcon, SaveIcon } from '@lucide/vue'
import { storeToRefs } from 'pinia'
import { useI18n } from 'vue-i18n'

import type {
  AdminColorMode,
  AdminComponentStyle,
  AdminDensity,
  AdminFont,
  AdminSidebarState,
} from '@/constants/admin-appearance'
import type { ContentLayout, Radius, Theme } from '@/constants/themes'
import type { NavigationMode } from '@/stores/sidebar-config'

import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import {
  ADMIN_COMPONENT_STYLE_OPTIONS,
  ADMIN_CONTENT_LAYOUTS,
  ADMIN_DENSITY_OPTIONS,
  ADMIN_FONT_OPTIONS,
  ADMIN_RADIUS,
  ADMIN_SIDEBAR_STATE_OPTIONS,
  ADMIN_THEME_PRIMARY_COLORS,
  COLOR_MODES,
} from '@/constants/admin-appearance'
import { useAdminAppearancePreferencesStore } from '@/stores/admin-appearance-preferences'

const adminAppearanceStore = useAdminAppearancePreferencesStore()
const { preferences } = storeToRefs(adminAppearanceStore)
const { t } = useI18n()

// 表单直接写入本地偏好 store，Pinia 持久化负责保存到当前浏览器。
const navigationModeOptions: Array<{ value: NavigationMode }> = [
  { value: 'collapsible' },
  { value: 'vercel' },
]

function setColorMode(colorMode: AdminColorMode) {
  adminAppearanceStore.updatePreference('colorMode', colorMode)
}

function setTheme(theme: Theme) {
  adminAppearanceStore.updatePreference('theme', theme)
}

function setRadius(radius: Radius) {
  adminAppearanceStore.updatePreference('radius', radius)
}

function setContentLayout(contentLayout: ContentLayout) {
  adminAppearanceStore.updatePreference('contentLayout', contentLayout)
}

function setNavigationMode(navigationMode: NavigationMode) {
  adminAppearanceStore.updatePreference('navigationMode', navigationMode)
}

function setFont(font: AdminFont) {
  adminAppearanceStore.updatePreference('font', font)
}

function setDensity(density: AdminDensity) {
  adminAppearanceStore.updatePreference('density', density)
}

function setComponentStyle(componentStyle: AdminComponentStyle) {
  adminAppearanceStore.updatePreference('componentStyle', componentStyle)
}

function setSidebarDefaultState(sidebarDefaultState: AdminSidebarState) {
  adminAppearanceStore.updatePreference('sidebarDefaultState', sidebarDefaultState)
}

function setReducedMotion(reducedMotion: boolean) {
  adminAppearanceStore.updatePreference('reducedMotion', reducedMotion)
}

function setStripedTables(stripedTables: boolean) {
  adminAppearanceStore.updatePreference('stripedTables', stripedTables)
}

function setShowBreadcrumbs(showBreadcrumbs: boolean) {
  adminAppearanceStore.updatePreference('showBreadcrumbs', showBreadcrumbs)
}

function setShowPageTitle(showPageTitle: boolean) {
  adminAppearanceStore.updatePreference('showPageTitle', showPageTitle)
}
</script>

<template>
  <div class="space-y-1">
    <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
      <div>
        <h3 class="text-lg font-medium">
          {{ t('pages.settings.appearancePreferences.title') }}
        </h3>
        <p class="text-sm text-muted-foreground">
          {{ t('pages.settings.appearancePreferences.description') }}
        </p>
      </div>
      <Button variant="outline" size="sm" @click="adminAppearanceStore.resetPreferences()">
        <RotateCcwIcon class="size-4" />
        {{ t('pages.settings.appearancePreferences.resetDefaults') }}
      </Button>
    </div>
  </div>
  <Separator class="my-4" />

  <div class="space-y-8">
    <section class="space-y-4">
      <div>
        <h4 class="text-sm font-medium">
          {{ t('pages.settings.appearancePreferences.themeSection') }}
        </h4>
        <p class="text-sm text-muted-foreground">
          {{ t('pages.settings.appearancePreferences.themeDescription') }}
        </p>
      </div>

      <div class="grid gap-4 lg:grid-cols-2">
        <div class="space-y-2">
          <UiLabel>{{ t('pages.settings.appearancePreferences.colorMode') }}</UiLabel>
          <div class="grid grid-cols-3 gap-2">
            <Button
              v-for="colorMode in COLOR_MODES"
              :key="colorMode"
              variant="outline"
              class="justify-center"
              :class="preferences.colorMode === colorMode ? 'border-foreground border-2' : ''"
              @click="setColorMode(colorMode)"
            >
              {{ t(`pages.settings.appearancePreferences.colorModes.${colorMode}`) }}
            </Button>
          </div>
        </div>

        <div class="space-y-2">
          <UiLabel>{{ t('pages.settings.appearancePreferences.font') }}</UiLabel>
          <UiSelect :model-value="preferences.font" @update:model-value="value => setFont(value as AdminFont)">
            <UiSelectTrigger>
              <UiSelectValue :placeholder="t('pages.settings.appearancePreferences.fontPlaceholder')" />
            </UiSelectTrigger>
            <UiSelectContent>
              <UiSelectItem
                v-for="font in ADMIN_FONT_OPTIONS"
                :key="font.value"
                :value="font.value"
              >
                {{ t(`pages.settings.appearancePreferences.fonts.${font.value}`) }}
              </UiSelectItem>
            </UiSelectContent>
          </UiSelect>
        </div>
      </div>

      <div class="space-y-2">
        <UiLabel>{{ t('pages.settings.appearancePreferences.palette') }}</UiLabel>
        <div class="grid gap-2 sm:grid-cols-2 xl:grid-cols-4">
          <Button
            v-for="theme in ADMIN_THEME_PRIMARY_COLORS"
            :key="theme.theme"
            variant="outline"
            class="justify-start"
            :class="preferences.theme === theme.theme ? 'border-foreground border-2' : ''"
            @click="setTheme(theme.theme)"
          >
            <span
              :style="{ '--theme-primary': theme.primaryColor }"
              class="size-3 rounded-full bg-(--theme-primary)"
            />
            <span>{{ t(`pages.settings.appearancePreferences.themes.${theme.theme}`) }}</span>
          </Button>
        </div>
      </div>

      <div class="space-y-2">
        <UiLabel>{{ t('pages.settings.appearancePreferences.radius') }}</UiLabel>
        <div class="grid grid-cols-5 gap-2">
          <Button
            v-for="radius in ADMIN_RADIUS"
            :key="radius"
            variant="outline"
            class="justify-center"
            :class="preferences.radius === radius ? 'border-foreground border-2' : ''"
            @click="setRadius(radius)"
          >
            {{ radius }}
          </Button>
        </div>
      </div>

      <div class="space-y-2">
        <UiLabel>{{ t('pages.settings.appearancePreferences.componentStyle') }}</UiLabel>
        <div class="grid gap-2 sm:grid-cols-2 xl:grid-cols-3">
          <Button
            v-for="style in ADMIN_COMPONENT_STYLE_OPTIONS"
            :key="style.value"
            variant="outline"
            class="h-auto justify-start px-3 py-2 text-left"
            :class="preferences.componentStyle === style.value ? 'border-foreground border-2' : ''"
            :title="t(`pages.settings.appearancePreferences.componentStyleDescriptions.${style.value}`)"
            @click="setComponentStyle(style.value)"
          >
            <span class="flex min-w-0 flex-col gap-1">
              <span class="truncate">{{ t(`pages.settings.appearancePreferences.componentStyles.${style.value}`) }}</span>
              <span class="text-xs font-normal text-muted-foreground">
                {{ t(`pages.settings.appearancePreferences.componentStyleDescriptions.${style.value}`) }}
              </span>
            </span>
          </Button>
        </div>
        <p class="text-xs text-muted-foreground">
          {{ t('pages.settings.appearancePreferences.componentStyleHint') }}
        </p>
      </div>
    </section>

    <Separator />

    <section class="space-y-4">
      <div>
        <h4 class="text-sm font-medium">
          {{ t('pages.settings.appearancePreferences.layoutSection') }}
        </h4>
        <p class="text-sm text-muted-foreground">
          {{ t('pages.settings.appearancePreferences.layoutDescription') }}
        </p>
      </div>

      <div class="grid gap-4 lg:grid-cols-3">
        <div class="space-y-2">
          <UiLabel>{{ t('pages.settings.appearancePreferences.contentLayout') }}</UiLabel>
          <div class="grid grid-cols-2 gap-2">
            <Button
              v-for="layout in ADMIN_CONTENT_LAYOUTS"
              :key="layout.value"
              variant="outline"
              class="justify-center"
              :class="preferences.contentLayout === layout.value ? 'border-foreground border-2' : ''"
              @click="setContentLayout(layout.value)"
            >
              <component :is="layout.icon" />
              {{ t(`pages.settings.appearancePreferences.contentLayouts.${layout.value}`) }}
            </Button>
          </div>
        </div>

        <div class="space-y-2">
          <UiLabel>{{ t('pages.settings.appearancePreferences.menuStyle') }}</UiLabel>
          <div class="grid grid-cols-2 gap-2">
            <Button
              v-for="mode in navigationModeOptions"
              :key="mode.value"
              variant="outline"
              class="justify-center"
              :title="t(`pages.settings.appearancePreferences.navigationDescriptions.${mode.value}`)"
              :class="preferences.navigationMode === mode.value ? 'border-foreground border-2' : ''"
              @click="setNavigationMode(mode.value)"
            >
              {{ t(`pages.settings.appearancePreferences.navigationModes.${mode.value}`) }}
            </Button>
          </div>
        </div>

        <div class="space-y-2">
          <UiLabel>{{ t('pages.settings.appearancePreferences.sidebarStartup') }}</UiLabel>
          <UiSelect :model-value="preferences.sidebarDefaultState" @update:model-value="value => setSidebarDefaultState(value as AdminSidebarState)">
            <UiSelectTrigger>
              <UiSelectValue :placeholder="t('pages.settings.appearancePreferences.sidebarStartupPlaceholder')" />
            </UiSelectTrigger>
            <UiSelectContent>
              <UiSelectItem
                v-for="state in ADMIN_SIDEBAR_STATE_OPTIONS"
                :key="state.value"
                :value="state.value"
              >
                {{ t(`pages.settings.appearancePreferences.sidebarStates.${state.value}`) }}
              </UiSelectItem>
            </UiSelectContent>
          </UiSelect>
        </div>
      </div>
    </section>

    <Separator />

    <section class="space-y-4">
      <div>
        <h4 class="text-sm font-medium">
          {{ t('pages.settings.appearancePreferences.displaySection') }}
        </h4>
        <p class="text-sm text-muted-foreground">
          {{ t('pages.settings.appearancePreferences.displayDescription') }}
        </p>
      </div>

      <div class="grid gap-4 lg:grid-cols-2">
        <div class="space-y-2">
          <UiLabel>{{ t('pages.settings.appearancePreferences.density') }}</UiLabel>
          <UiSelect :model-value="preferences.density" @update:model-value="value => setDensity(value as AdminDensity)">
            <UiSelectTrigger>
              <UiSelectValue :placeholder="t('pages.settings.appearancePreferences.densityPlaceholder')" />
            </UiSelectTrigger>
            <UiSelectContent>
              <UiSelectItem
                v-for="density in ADMIN_DENSITY_OPTIONS"
                :key="density.value"
                :value="density.value"
              >
                {{ t(`pages.settings.appearancePreferences.densities.${density.value}`) }}
              </UiSelectItem>
            </UiSelectContent>
          </UiSelect>
        </div>

        <div class="grid gap-3 rounded-md border p-4">
          <!-- UiSwitch 使用 Reka modelValue 受控接口，确保布尔偏好能写回持久化 store。 -->
          <label class="flex items-center justify-between gap-4">
            <span>
              <span class="block text-sm font-medium">{{ t('pages.settings.appearancePreferences.reduceMotion') }}</span>
              <span class="block text-sm text-muted-foreground">{{ t('pages.settings.appearancePreferences.reduceMotionDescription') }}</span>
            </span>
            <UiSwitch
              :model-value="preferences.reducedMotion"
              @update:model-value="setReducedMotion"
            />
          </label>
          <label class="flex items-center justify-between gap-4">
            <span>
              <span class="block text-sm font-medium">{{ t('pages.settings.appearancePreferences.stripedTables') }}</span>
              <span class="block text-sm text-muted-foreground">{{ t('pages.settings.appearancePreferences.stripedTablesDescription') }}</span>
            </span>
            <UiSwitch
              :model-value="preferences.stripedTables"
              @update:model-value="setStripedTables"
            />
          </label>
          <label class="flex items-center justify-between gap-4">
            <span>
              <span class="block text-sm font-medium">{{ t('pages.settings.appearancePreferences.showBreadcrumbs') }}</span>
              <span class="block text-sm text-muted-foreground">{{ t('pages.settings.appearancePreferences.showBreadcrumbsDescription') }}</span>
            </span>
            <UiSwitch
              :model-value="preferences.showBreadcrumbs"
              @update:model-value="setShowBreadcrumbs"
            />
          </label>
          <label class="flex items-center justify-between gap-4">
            <span>
              <span class="block text-sm font-medium">{{ t('pages.settings.appearancePreferences.showPageTitles') }}</span>
              <span class="block text-sm text-muted-foreground">{{ t('pages.settings.appearancePreferences.showPageTitlesDescription') }}</span>
            </span>
            <UiSwitch
              :model-value="preferences.showPageTitle"
              @update:model-value="setShowPageTitle"
            />
          </label>
        </div>
      </div>
    </section>

    <div class="flex items-center gap-2 text-sm text-muted-foreground">
      <SaveIcon class="size-4" />
      {{ t('pages.settings.appearancePreferences.savedHint') }}
    </div>
  </div>
</template>
