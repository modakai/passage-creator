<script setup lang="ts">
import { ArrowUpRightIcon } from '@lucide/vue'
import { useCookies } from '@vueuse/integrations/useCookies'
import { storeToRefs } from 'pinia'

import AppSidebar from '@/components/app-sidebar/index.vue'
import CommandMenuPanel from '@/components/command-menu-panel/index.vue'
import ThemePopover from '@/components/custom-theme/theme-popover.vue'
import LanguageChange from '@/components/language-change.vue'
import { AnnouncementHub, NotificationBell } from '@/components/notification-center'
import { SIDEBAR_COOKIE_NAME } from '@/components/ui/sidebar/utils'
import { cn } from '@/lib/utils'
import { useAdminAppearancePreferencesStore } from '@/stores/admin-appearance-preferences'

const sidebarCookies = useCookies([SIDEBAR_COOKIE_NAME])
const adminAppearanceStore = useAdminAppearancePreferencesStore()
const { preferences } = storeToRefs(adminAppearanceStore)

// 后台布局是管理端外观偏好的唯一初始化位置，避免用户端读取这套配置。
useApplyAdminAppearancePreferences()

const defaultSidebarOpen = computed(() => {
  if (preferences.value.sidebarDefaultState === 'expanded')
    return true
  if (preferences.value.sidebarDefaultState === 'collapsed')
    return false
  return sidebarCookies.get(SIDEBAR_COOKIE_NAME) !== 'false'
})
</script>

<template>
  <UiSidebarProvider :default-open="defaultSidebarOpen">
    <AppSidebar />
    <UiSidebarInset class="w-full max-w-full peer-data-[state=collapsed]:w-[calc(100%-var(--sidebar-width-icon)-1rem)] peer-data-[state=expanded]:w-[calc(100%-var(--sidebar-width))]">
      <header
        class="flex items-center gap-3 sm:gap-4 h-16 p-4 shrink-0 transition-[width,height] ease-linear"
      >
        <UiSidebarTrigger class="-ml-1" />
        <UiSeparator orientation="vertical" class="h-6" />
        <CommandMenuPanel />
        <div class="flex-1" />
        <div class="ml-auto flex items-center space-x-4">
          <NotificationBell receiver-type="admin" />
          <!-- 在后台顶部提供回到前台的快捷入口，方便双端切换。 -->
          <UiButton variant="outline" size="sm" as-child>
            <RouterLink to="/">
              去前台
              <ArrowUpRightIcon class="ml-1 size-4" />
            </RouterLink>
          </UiButton>
          <LanguageChange />
          <ThemePopover />
        </div>
      </header>

      <main
        :class="cn(
          'p-[var(--admin-density-space,1rem)] grow',
          preferences.contentLayout === 'centered' ? 'container mx-auto ' : '',
        )"
      >
        <AnnouncementHub receiver-type="admin" />
        <router-view />
      </main>
    </UiSidebarInset>
  </UiSidebarProvider>
</template>
