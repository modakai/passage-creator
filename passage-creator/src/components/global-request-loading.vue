<script setup lang="ts">
import { LoaderCircle } from '@lucide/vue'

import { globalLoadingVisible } from '@/utils/request-control'
</script>

<template>
  <Transition
    enter-active-class="transition-opacity duration-150"
    enter-from-class="opacity-0"
    enter-to-class="opacity-100"
    leave-active-class="transition-opacity duration-150"
    leave-from-class="opacity-100"
    leave-to-class="opacity-0"
  >
    <!-- 全局请求中的遮罩只提示状态，不阻塞用户阅读当前页面。 -->
    <div
      v-if="globalLoadingVisible"
      class="pointer-events-none fixed inset-x-0 top-0 z-50 flex justify-center pt-4"
      role="status"
      aria-live="polite"
      aria-label="请求加载中"
    >
      <div class="flex items-center gap-2 rounded-md border bg-background/95 px-3 py-2 text-sm text-foreground shadow-md backdrop-blur">
        <LoaderCircle class="size-4 animate-spin" />
        <span>加载中...</span>
      </div>
    </div>
  </Transition>
</template>
