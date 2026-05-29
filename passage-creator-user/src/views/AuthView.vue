<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AuthFormPanel from '@/components/auth/AuthFormPanel.vue'
import AuthShell from '@/components/auth/AuthShell.vue'
import { login, register } from '@/services/api'

const route = useRoute()
const router = useRouter()
const mode = ref<'login' | 'register'>(route.query.mode === 'register' ? 'register' : 'login')
const userAccount = ref('')
const userPassword = ref('')
const checkPassword = ref('')
const isSubmitting = ref(false)
const errorMessage = ref('')

const isRegister = computed(() => mode.value === 'register')
const title = computed(() => isRegister.value ? '创建你的创作账户' : '回到你的创作工作台')
const subtitle = computed(() => isRegister.value
  ? '注册后会直接登录，并返回刚才准备进入的创作流程。'
  : '登录后继续标题确认、大纲编辑、图文生成和作品管理。')
const canSubmit = computed(() => {
  if (userAccount.value.trim().length < 4 || userPassword.value.length < 8) {
    return false
  }
  return !isRegister.value || checkPassword.value === userPassword.value
})

/**
 * 登录或注册成功后回到 return 参数指向的创作页面，没有 return 时进入首页。
 */
async function submitAuth() {
  if (!canSubmit.value) {
    errorMessage.value = isRegister.value ? '账号至少 4 位，密码至少 8 位，两次密码必须一致。' : '账号至少 4 位，密码至少 8 位。'
    return
  }

  isSubmitting.value = true
  errorMessage.value = ''
  try {
    if (isRegister.value) {
      await register({
        userAccount: userAccount.value.trim(),
        userPassword: userPassword.value,
        checkPassword: checkPassword.value,
      })
    }
    else {
      await login({
        userAccount: userAccount.value.trim(),
        userPassword: userPassword.value,
      })
    }

    const redirect = typeof route.query.return === 'string' ? route.query.return : '/'
    await router.replace(redirect)
  }
  catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '认证失败，请稍后重试。'
  }
  finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <AuthShell :mode="mode" :subtitle="subtitle" :title="title">
    <AuthFormPanel
      v-model:check-password="checkPassword"
      v-model:mode="mode"
      v-model:user-account="userAccount"
      v-model:user-password="userPassword"
      :error-message="errorMessage"
      :loading="isSubmitting"
      @submit="submitAuth"
    />
  </AuthShell>
</template>
