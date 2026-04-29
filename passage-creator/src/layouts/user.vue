<script setup lang="ts">
import { AnnouncementHub } from '@/components/notification-center'
import UserPortalHeader from '@/components/user-portal/user-portal-header.vue'
import { useAuth } from '@/composables/use-auth'

const route = useRoute()
const { isLogin } = useAuth()

// 创作页需要使用参考图的三栏全宽画布，其他用户端页面仍保留原来的内容容器。
const isFullWidthPage = computed(() => route.meta.fullWidth === true)
</script>

<template>
  <div class="min-h-screen bg-[#f8faf9] text-slate-950">
    <UserPortalHeader />

    <main :class="isFullWidthPage ? '' : 'container mx-auto px-4 py-8'">
      <AnnouncementHub v-if="isLogin && !isFullWidthPage" receiver-type="app" />
      <router-view />
    </main>

    <footer v-if="!isFullWidthPage" class="border-t bg-background/70">
      <div class="container mx-auto flex flex-col gap-2 px-4 py-6 text-sm text-muted-foreground md:flex-row md:items-center md:justify-between">
        <span>Sakura Passage AI 创作平台</span>
        <span>与后台共用认证接口，按角色决定后台访问权限。</span>
      </div>
    </footer>
  </div>
</template>
