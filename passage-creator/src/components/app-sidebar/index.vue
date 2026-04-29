<script lang="ts" setup>
import { useSidebarData } from './data/sidebar-data'
import NavFooter from './nav-footer.vue'
import NavTeam from './nav-team.vue'
import SidebarLogo from './sidebar-logo.vue'

const { sidebarData } = useSidebarData()
</script>

<template>
  <UiSidebar collapsible="icon" class="z-50">
    <UiSidebarHeader>
      <SidebarLogo />
    </UiSidebarHeader>

    <UiSidebarContent>
      <NavTeam :nav-main="sidebarData.navMain" />
    </UiSidebarContent>

    <UiSidebarFooter>
      <NavFooter :user="sidebarData.user" />
    </UiSidebarFooter>

    <UiSidebarRail />
  </UiSidebar>
</template>
