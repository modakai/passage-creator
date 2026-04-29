<script lang="ts" setup>
import sidebarLogo from '@/assets/logo.svg'
</script>

<template>
  <UiSidebarMenu>
    <UiSidebarMenuItem>
      <UiSidebarMenuButton
        size="lg"
        as-child
        class="data-[state=open]:bg-sidebar-accent data-[state=open]:text-sidebar-accent-foreground"
      >
        <router-link to="/dashboard">
          <div class="flex items-center justify-center rounded-lg aspect-square size-8 bg-sidebar-primary text-sidebar-primary-foreground">
            <!-- 侧边栏品牌标识统一使用项目 assets 中的 Sakura logo。 -->
            <img
              :src="sidebarLogo"
              alt="Logo"
              class="size-5 object-contain"
            >
          </div>
          <div class="grid flex-1 text-sm leading-tight text-left">
            <!-- 侧边栏收起时文本会由 shadcn sidebar 样式自动隐藏。 -->
            <span class="font-semibold truncate">Sakura Admin</span>
            <span class="text-xs truncate text-muted-foreground">Admin Console</span>
          </div>
        </router-link>
      </UiSidebarMenuButton>
    </UiSidebarMenuItem>
  </UiSidebarMenu>
</template>
