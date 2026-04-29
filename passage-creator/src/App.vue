<script setup lang="ts">
import GlobalRequestLoading from '@/components/global-request-loading.vue'
import Loading from '@/components/loading.vue'
import { Toaster } from '@/components/ui/sonner'
</script>

<template>
  <Toaster />
  <GlobalRequestLoading />

  <Suspense>
    <router-view v-slot="{ Component, route }">
      <!-- 路由切换期间 Component 可能短暂为空，先保护再按稳定路径 key 替换页面。 -->
      <component :is="Component" v-if="Component" :key="route.fullPath" />
    </router-view>

    <template #fallback>
      <Loading />
    </template>
  </Suspense>
</template>
