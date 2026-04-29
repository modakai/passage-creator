<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import Error from '@/components/custom-error.vue'
import { RouterPath } from '@/constants/route-path'

const { t } = useI18n()
const route = useRoute()

// 401 页面优先展示守卫或接口层传入的上下文提示。
const errorMessage = computed(() => {
  const queryMessage = route.query.message
  return typeof queryMessage === 'string' && queryMessage
    ? queryMessage
    : t('errors.unauthorized.description')
})

// 登录按钮固定指向用户端登录页，并透传原始跳转目标。
const loginTarget = computed(() => {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : undefined
  return redirect
    ? { path: String(RouterPath.USER_LOGIN), query: { redirect } }
    : { path: String(RouterPath.USER_LOGIN) }
})
</script>

<template>
  <div class="flex items-center justify-center h-screen">
    <Error
      :code="401"
      :subtitle="t('errors.unauthorized.subtitle')"
      :error="errorMessage"
    >
      <div class="flex justify-center gap-2">
        <UiButton variant="outline" @click="$router.push('/')">
          {{ t('errors.actions.backHome') }}
        </UiButton>
        <UiButton @click="$router.push(loginTarget)">
          {{ t('errors.actions.goUserLogin') }}
        </UiButton>
      </div>
    </Error>
  </div>
</template>

<route lang="yaml">
meta:
  layout: blank
</route>
