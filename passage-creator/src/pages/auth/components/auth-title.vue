<script setup lang="ts">
import { useColorMode } from '@vueuse/core'

const mode = useColorMode()
const logo = computed(() => {
  return mode.value === 'dark' ? '/logo.svg' : '/logo-black.svg'
})
</script>

<template>
  <h1 class="flex items-center gap-2 text-xl font-bold">
    <img :src="logo">
    <!-- 产品标题文案：按项目品牌定制 -->
    <span>Sakura Admin</span>
  </h1>
</template>
