<script setup lang="ts">
import { ArrowRightIcon, EyeIcon, EyeOffIcon, LoaderCircleIcon, LockKeyholeIcon, UserRoundIcon } from '@lucide/vue'
import { computed, ref } from 'vue'

interface Props {
  mode: 'login' | 'register'
  userAccount: string
  userPassword: string
  checkPassword: string
  errorMessage?: string
  loading?: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:mode': [value: 'login' | 'register']
  'update:userAccount': [value: string]
  'update:userPassword': [value: string]
  'update:checkPassword': [value: string]
  submit: []
}>()

const showPassword = ref(false)
const showCheckPassword = ref(false)

const isRegister = computed(() => props.mode === 'register')
const canSubmit = computed(() => {
  if (props.userAccount.trim().length < 4 || props.userPassword.length < 8) {
    return false
  }
  return !isRegister.value || props.checkPassword === props.userPassword
})

/**
 * 统一从原生输入事件里读取值，避免在模板中写类型断言。
 */
function readInputValue(event: Event) {
  return (event.target as HTMLInputElement).value
}

/**
 * 表单组件只负责本地基础校验，具体后端提交由页面容器处理。
 */
function submit() {
  if (!canSubmit.value || props.loading) {
    return
  }
  emit('submit')
}
</script>

<template>
  <form class="w-full" @submit.prevent="submit">
    <div class="mb-7">
      <div class="inline-flex rounded-full bg-slate-100 p-1">
        <button type="button" class="rounded-full px-5 py-2 text-sm transition" :class="!isRegister ? 'bg-slate-950 text-white shadow-sm' : 'text-slate-500 hover:text-slate-950'" @click="emit('update:mode', 'login')">
          登录
        </button>
        <button type="button" class="rounded-full px-5 py-2 text-sm transition" :class="isRegister ? 'bg-slate-950 text-white shadow-sm' : 'text-slate-500 hover:text-slate-950'" @click="emit('update:mode', 'register')">
          注册
        </button>
      </div>

      <h2 class="mt-7 text-2xl font-semibold tracking-[-0.04em] text-slate-950">
        {{ isRegister ? '创建账户' : '账号登录' }}
      </h2>
      <p class="mt-2 text-sm leading-6 text-slate-500">
        {{ isRegister ? '注册后自动登录，直接进入你的创作流程。' : '使用 Sakura Passage AI 账号继续创作。' }}
      </p>
    </div>

    <div class="grid gap-4">
      <label class="grid gap-2 text-sm font-semibold text-slate-700" for="auth-account">
        账号
        <div class="relative">
          <UserRoundIcon class="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
          <input id="auth-account" :value="userAccount" class="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50/80 pl-11 pr-4 text-sm outline-none transition focus:border-blue-300 focus:bg-white" autocomplete="username" placeholder="至少 4 位账号" @input="emit('update:userAccount', readInputValue($event))" />
        </div>
      </label>

      <label class="grid gap-2 text-sm font-semibold text-slate-700" for="auth-password">
        密码
        <div class="relative">
          <LockKeyholeIcon class="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
          <input id="auth-password" :type="showPassword ? 'text' : 'password'" :value="userPassword" class="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50/80 pl-11 pr-12 text-sm outline-none transition focus:border-blue-300 focus:bg-white" autocomplete="current-password" placeholder="至少 8 位密码" @input="emit('update:userPassword', readInputValue($event))" />
          <button type="button" class="absolute right-3 top-1/2 grid size-8 -translate-y-1/2 place-items-center rounded-xl text-slate-400 transition hover:bg-white hover:text-slate-700" :aria-label="showPassword ? '隐藏密码' : '查看密码'" @click="showPassword = !showPassword">
            <EyeOffIcon v-if="showPassword" class="size-4" />
            <EyeIcon v-else class="size-4" />
          </button>
        </div>
      </label>

      <label v-if="isRegister" class="grid gap-2 text-sm font-semibold text-slate-700" for="auth-check-password">
        确认密码
        <div class="relative">
          <LockKeyholeIcon class="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
          <input id="auth-check-password" :type="showCheckPassword ? 'text' : 'password'" :value="checkPassword" class="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50/80 pl-11 pr-12 text-sm outline-none transition focus:border-blue-300 focus:bg-white" autocomplete="new-password" placeholder="再次输入密码" @input="emit('update:checkPassword', readInputValue($event))" />
          <button type="button" class="absolute right-3 top-1/2 grid size-8 -translate-y-1/2 place-items-center rounded-xl text-slate-400 transition hover:bg-white hover:text-slate-700" :aria-label="showCheckPassword ? '隐藏确认密码' : '查看确认密码'" @click="showCheckPassword = !showCheckPassword">
            <EyeOffIcon v-if="showCheckPassword" class="size-4" />
            <EyeIcon v-else class="size-4" />
          </button>
        </div>
      </label>
    </div>

    <p v-if="errorMessage" class="mt-5 rounded-2xl border border-rose-100 bg-rose-50 p-3 text-sm leading-6 text-rose-700">
      {{ errorMessage }}
    </p>

    <button type="submit" class="ai-gradient mt-6 inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-2xl font-semibold text-white shadow-xl shadow-blue-500/20 transition hover:-translate-y-0.5 disabled:translate-y-0 disabled:opacity-50" :disabled="loading || !canSubmit">
      <LoaderCircleIcon v-if="loading" class="size-4 animate-spin" />
      <template v-else>
        {{ isRegister ? '注册并进入创作' : '登录并继续' }}
        <ArrowRightIcon class="size-4" />
      </template>
    </button>
  </form>
</template>
