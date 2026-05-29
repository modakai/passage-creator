<script setup lang="ts">
import { ArrowRightIcon, LoaderCircleIcon, LockKeyholeIcon, SparklesIcon, UserPlusIcon } from '@lucide/vue'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { login, register } from '@/services/api'

const route = useRoute()
const router = useRouter()
const mode = ref(route.query.mode === 'register' ? 'register' : 'login')
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
 * 切换登录和注册模式时只清理错误，不清理账号，减少用户重复输入。
 */
function switchMode(nextMode: 'login' | 'register') {
  mode.value = nextMode
  errorMessage.value = ''
}

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
  <div class="grid min-h-[calc(100vh-9rem)] place-items-center">
    <section class="grid w-full max-w-5xl overflow-hidden rounded-[2.5rem] border border-white/90 bg-white/70 shadow-2xl shadow-slate-900/10 backdrop-blur-2xl lg:grid-cols-[1fr_420px]">
      <div class="relative overflow-hidden p-8 sm:p-10">
        <div class="pointer-events-none absolute -right-24 -top-32 size-96 rounded-full bg-[conic-gradient(from_120deg,rgba(124,58,237,.22),rgba(59,130,246,.18),rgba(6,182,212,.14),rgba(124,58,237,.22))] blur-3xl" />
        <div class="relative">
          <div class="mb-6 inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white/70 px-4 py-2 text-sm text-slate-500">
            <SparklesIcon class="size-4 text-blue-500" />
            Sakura Passage AI
          </div>
          <h1 class="ai-text max-w-xl text-5xl font-semibold leading-[1.06] tracking-[-0.07em] sm:text-6xl">
            {{ title }}
          </h1>
          <p class="mt-6 max-w-xl text-base leading-8 text-slate-500">
            {{ subtitle }}
          </p>

          <div class="mt-10 grid gap-3 sm:grid-cols-3">
            <div v-for="item in ['灵感输入', '人机确认', '作品沉淀']" :key="item" class="rounded-3xl border border-slate-200 bg-white/65 p-4 text-sm font-medium text-slate-700">
              {{ item }}
            </div>
          </div>
        </div>
      </div>

      <form class="border-t border-white/90 bg-white/78 p-6 sm:p-8 lg:border-l lg:border-t-0" @submit.prevent="submitAuth">
        <div class="mb-6 flex rounded-full bg-slate-100 p-1">
          <button type="button" class="flex-1 rounded-full px-4 py-2 text-sm transition" :class="!isRegister ? 'bg-slate-950 text-white shadow-sm' : 'text-slate-500'" @click="switchMode('login')">
            登录
          </button>
          <button type="button" class="flex-1 rounded-full px-4 py-2 text-sm transition" :class="isRegister ? 'bg-slate-950 text-white shadow-sm' : 'text-slate-500'" @click="switchMode('register')">
            注册
          </button>
        </div>

        <label class="block text-sm font-semibold text-slate-700" for="user-account">账号</label>
        <input id="user-account" v-model="userAccount" class="mt-2 h-12 w-full rounded-2xl border border-slate-200 bg-white px-4 text-sm outline-none focus:border-blue-300" autocomplete="username" placeholder="至少 4 位账号" />

        <label class="mt-5 block text-sm font-semibold text-slate-700" for="user-password">密码</label>
        <input id="user-password" v-model="userPassword" class="mt-2 h-12 w-full rounded-2xl border border-slate-200 bg-white px-4 text-sm outline-none focus:border-blue-300" autocomplete="current-password" type="password" placeholder="至少 8 位密码" />

        <template v-if="isRegister">
          <label class="mt-5 block text-sm font-semibold text-slate-700" for="check-password">确认密码</label>
          <input id="check-password" v-model="checkPassword" class="mt-2 h-12 w-full rounded-2xl border border-slate-200 bg-white px-4 text-sm outline-none focus:border-blue-300" autocomplete="new-password" type="password" placeholder="再次输入密码" />
        </template>

        <p v-if="errorMessage" class="mt-5 rounded-2xl border border-rose-100 bg-rose-50 p-3 text-sm leading-6 text-rose-700">
          {{ errorMessage }}
        </p>

        <button type="submit" class="ai-gradient mt-6 inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-2xl font-semibold text-white shadow-xl shadow-blue-500/20" :disabled="isSubmitting || !canSubmit">
          <LoaderCircleIcon v-if="isSubmitting" class="size-4 animate-spin" />
          <UserPlusIcon v-else-if="isRegister" class="size-4" />
          <LockKeyholeIcon v-else class="size-4" />
          {{ isRegister ? '注册并进入创作' : '登录并继续' }}
          <ArrowRightIcon class="size-4" />
        </button>
      </form>
    </section>
  </div>
</template>
