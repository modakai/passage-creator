<script lang="ts" setup>
import { ChevronRightIcon, ExternalLinkIcon } from '@lucide/vue'

import { useSidebar } from '@/components/ui/sidebar'
import { isExternalUrl } from '@/utils/is-external-url'

import type { NavGroup, NavItem } from './types'

import MenuButton from './menu-button.vue'

const { navMain } = defineProps<{
  navMain: NavGroup[]
}>()

const route = useRoute()

const { state, isMobile } = useSidebar()

function isActive(menu: NavItem): boolean {
  const pathname = route.path
  if (menu.url) {
    return pathname === menu.url
  }
  // 多级菜单按后代路由递归判断激活态。
  return !!menu.items?.some(item => isActive(item))
}
</script>

<template>
  <UiSidebarGroup v-for="group in navMain" :key="group.id">
    <UiSidebarGroupLabel>{{ group.title }}</UiSidebarGroupLabel>
    <UiSidebarMenu>
      <template v-for="menu in group.items" :key="menu.id">
        <UiSidebarMenuItem v-if="!menu.items">
          <MenuButton
            :is-active="isActive(menu)"
            :tooltip="menu.title"
            :is-external-url="isExternalUrl(menu.url)"
            :menu="menu as NavItem"
          />
        </UiSidebarMenuItem>

        <UiSidebarMenuItem v-else>
          <!-- sidebar expanded -->
          <UiCollapsible
            v-if="state !== 'collapsed' || isMobile"
            :default-open="isActive(menu)"
            class="group/collapsible"
          >
            <UiCollapsibleTrigger as-child>
              <UiSidebarMenuButton :tooltip="menu.title">
                <component :is="menu.icon" v-if="menu.icon" />
                <span>{{ menu.title }}</span>
                <ChevronRightIcon
                  class="ml-auto transition-transform duration-200 group-data-[state=open]/collapsible:rotate-90"
                />
              </UiSidebarMenuButton>
            </UiCollapsibleTrigger>
            <UiCollapsibleContent>
              <UiSidebarMenuSub>
                <UiSidebarMenuSubItem v-for="subItem in menu.items" :key="subItem.id">
                  <UiCollapsible
                    v-if="subItem.items"
                    :default-open="isActive(subItem)"
                    class="group/sub-collapsible"
                  >
                    <UiCollapsibleTrigger as-child>
                      <UiSidebarMenuSubButton :is-active="isActive(subItem)" class="cursor-pointer">
                        <component :is="subItem.icon" v-if="subItem.icon" />
                        <span>{{ subItem.title }}</span>
                        <ChevronRightIcon
                          class="ml-auto transition-transform duration-200 group-data-[state=open]/sub-collapsible:rotate-90"
                        />
                      </UiSidebarMenuSubButton>
                    </UiCollapsibleTrigger>
                    <UiCollapsibleContent>
                      <UiSidebarMenuSub class="ml-3">
                        <UiSidebarMenuSubItem v-for="childItem in subItem.items" :key="childItem.id">
                          <UiSidebarMenuSubButton as-child :is-active="isActive(childItem)">
                            <a v-if="isExternalUrl(childItem?.url)" :href="childItem?.url" target="_blank" rel="noopener noreferrer" class="flex items-center gap-2">
                              <component :is="childItem.icon" v-if="childItem.icon" />
                              <span>{{ childItem.title }}</span>
                              <ExternalLinkIcon class="w-4 h-4 ml-auto" />
                            </a>
                            <router-link v-else :to="childItem?.url || '/'">
                              <component :is="childItem.icon" v-if="childItem.icon" />
                              <span>{{ childItem.title }}</span>
                            </router-link>
                          </UiSidebarMenuSubButton>
                        </UiSidebarMenuSubItem>
                      </UiSidebarMenuSub>
                    </UiCollapsibleContent>
                  </UiCollapsible>

                  <UiSidebarMenuSubButton v-else as-child :is-active="isActive(subItem)">
                    <a v-if="isExternalUrl(subItem?.url)" :href="subItem?.url" target="_blank" rel="noopener noreferrer" class="flex items-center gap-2">
                      <component :is="subItem.icon" v-if="subItem.icon" />
                      <span>{{ subItem.title }}</span>
                      <ExternalLinkIcon class="w-4 h-4 ml-auto" />
                    </a>
                    <router-link v-else :to="subItem?.url || '/'">
                      <component :is="subItem.icon" v-if="subItem.icon" />
                      <span>{{ subItem.title }}</span>
                    </router-link>
                  </UiSidebarMenuSubButton>
                </UiSidebarMenuSubItem>
              </UiSidebarMenuSub>
            </UiCollapsibleContent>
          </UiCollapsible>

          <!-- sidebar collapsed -->
          <UiDropdownMenu v-else>
            <UiDropdownMenuTrigger as-child>
              <UiSidebarMenuButton :tooltip="menu.title">
                <component :is="menu.icon" v-if="menu.icon" />
                <span>{{ menu.title }}</span>
              </UiSidebarMenuButton>
            </UiDropdownMenuTrigger>
            <UiDropdownMenuContent align="start" side="right">
              <UiDropdownMenuLabel>{{ menu.title }}</UiDropdownMenuLabel>
              <UiDropdownMenuSeparator />
              <template v-for="subItem in menu.items" :key="subItem.id">
                <UiDropdownMenuSub v-if="subItem.items">
                  <UiDropdownMenuSubTrigger>
                    <component :is="subItem.icon" v-if="subItem.icon" />
                    <span>{{ subItem.title }}</span>
                  </UiDropdownMenuSubTrigger>
                  <UiDropdownMenuSubContent>
                    <UiDropdownMenuItem v-for="childItem in subItem.items" :key="childItem.id" as-child>
                      <a v-if="isExternalUrl(childItem?.url)" :href="childItem?.url" target="_blank" rel="noopener noreferrer">
                        <component :is="childItem.icon" v-if="childItem.icon" />
                        <span>{{ childItem.title }}</span>
                      </a>
                      <router-link v-else :to="childItem?.url || '/'">
                        <component :is="childItem.icon" v-if="childItem.icon" />
                        <span>{{ childItem.title }}</span>
                      </router-link>
                    </UiDropdownMenuItem>
                  </UiDropdownMenuSubContent>
                </UiDropdownMenuSub>

                <UiDropdownMenuItem v-else as-child>
                  <a v-if="isExternalUrl(subItem?.url)" :href="subItem?.url" target="_blank" rel="noopener noreferrer">
                    <component :is="subItem.icon" v-if="subItem.icon" />
                    <span>{{ subItem.title }}</span>
                  </a>
                  <router-link v-else :to="subItem?.url || '/'">
                    <component :is="subItem.icon" v-if="subItem.icon" />
                    <span>{{ subItem.title }}</span>
                  </router-link>
                </UiDropdownMenuItem>
              </template>
            </UiDropdownMenuContent>
          </UiDropdownMenu>
        </UiSidebarMenuItem>
      </template>
    </UiSidebarMenu>
  </UiSidebarGroup>
</template>
