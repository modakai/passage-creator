<script setup lang="ts">
import { useI18n } from 'vue-i18n'

defineProps<{
  code: number
  subtitle: string
  error: string
}>()

// 错误页通用按钮统一走国际化，避免各错误页默认按钮文案不一致。
const { t } = useI18n()
</script>

<template>
  <div class="max-w-2xl mx-auto text-center">
    <h1 class="font-bold text-8xl">
      {{ code }}
    </h1>
    <h2 class="mt-4 text-2xl font-bold">
      {{ subtitle }}
    </h2>
    <p class="text-stone-400">
      {{ error }}
    </p>

    <footer class="mt-8">
      <slot>
        <div class="flex justify-center gap-2">
          <UiButton variant="outline" @click="$router.back()">
            {{ t('errors.actions.goBack') }}
          </UiButton>
          <UiButton @click="$router.push('/')">
            {{ t('errors.actions.backHome') }}
          </UiButton>
        </div>
      </slot>
    </footer>
  </div>
</template>
