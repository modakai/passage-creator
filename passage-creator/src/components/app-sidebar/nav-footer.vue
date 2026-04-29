<script setup lang="ts">
import { BadgeCheckIcon, BellIcon, ChevronsUpDownIcon, LogOutIcon, UserRoundCogIcon } from '@lucide/vue'
import { useI18n } from 'vue-i18n'

import { useSidebar } from '@/components/ui/sidebar'

import type { User } from './types'

const { user } = defineProps<
  { user: User }
>()

const { logout } = useAuth()
const { isMobile, open } = useSidebar()
// 用户下拉菜单文案统一走 i18n，避免直接显示缺失的翻译 key。
const { t } = useI18n()
</script>

<template>
  <UiSidebarMenu>
    <UiSidebarMenuItem>
      <UiDropdownMenu>
        <UiDropdownMenuTrigger as-child>
          <UiSidebarMenuButton
            size="lg"
            class="data-[state=open]:bg-sidebar-accent data-[state=open]:text-sidebar-accent-foreground"
          >
            <UiAvatar class="size-8 rounded-lg">
              <UiAvatarImage :src="user.avatar" :alt="user.name" />
              <UiAvatarFallback class="rounded-lg">
                CN
              </UiAvatarFallback>
            </UiAvatar>
            <div class="grid flex-1 text-sm leading-tight text-left">
              <span class="font-semibold truncate">{{ user.name }}</span>
              <span class="text-xs truncate">{{ user.email }}</span>
            </div>
            <ChevronsUpDownIcon class="ml-auto size-4" />
          </UiSidebarMenuButton>
        </UiDropdownMenuTrigger>
        <UiDropdownMenuContent
          class="w-(--radix-dropdown-menu-trigger-width) min-w-56 rounded-lg"
          :side="(isMobile || open) ? 'bottom' : 'right'"
          align="start"
          :side-offset="4"
        >
          <UiDropdownMenuLabel class="p-0 font-normal">
            <div class="flex items-center gap-2 px-1 py-1.5 text-left text-sm">
              <UiAvatar class="size-8 rounded-lg">
                <UiAvatarImage :src="user.avatar" :alt="user.name" />
                <UiAvatarFallback class="rounded-lg">
                  CN
                </UiAvatarFallback>
              </UiAvatar>
              <div class="grid flex-1 text-sm leading-tight text-left">
                <span class="font-semibold truncate">{{ user.name }}</span>
                <span class="text-xs truncate">{{ user.email }}</span>
              </div>
            </div>
          </UiDropdownMenuLabel>

          <UiDropdownMenuSeparator />
          <UiDropdownMenuGroup>
            <UiDropdownMenuItem @click="$router.push('/settings/')">
              <UserRoundCogIcon />
              {{ t('menu.settings.profile') }}
            </UiDropdownMenuItem>
            <UiDropdownMenuItem @click="$router.push('/settings/account')">
              <BadgeCheckIcon />
              {{ t('menu.settings.account') }}
            </UiDropdownMenuItem>
            <UiDropdownMenuItem @click="$router.push('/settings/notifications')">
              <BellIcon />
              {{ t('menu.settings.notifications') }}
            </UiDropdownMenuItem>
          </UiDropdownMenuGroup>

          <UiDropdownMenuSeparator />
          <UiDropdownMenuItem @click="logout">
            <LogOutIcon />
            {{ t('menu.logout') }}
          </UiDropdownMenuItem>
        </UiDropdownMenuContent>
      </UiDropdownMenu>
    </UiSidebarMenuItem>
  </UiSidebarMenu>
</template>
