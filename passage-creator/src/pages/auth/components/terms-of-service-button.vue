<script lang="ts" setup>
/**
 * 点击后跳转到用户协议详情页。
 */
const router = useRouter()
</script>

<template>
  <UiButton variant="link" class="px-0 font-normal underline text-muted-foreground" @click="router.push('/agreement/user_agreement')">
    Terms of Service
  </UiButton>
</template>
