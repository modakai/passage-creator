<script setup lang="ts">
import AuthTitle from './components/auth-title.vue'
import LoginForm from './components/login-form.vue'
</script>

<template>
  <div class="relative min-h-screen overflow-hidden bg-[linear-gradient(180deg,_#f8fbff_0%,_#f8fafc_38%,_#eef6ff_100%)] dark:bg-[linear-gradient(180deg,_#020617_0%,_#0f172a_42%,_#082f49_100%)]">
    <div class="pointer-events-none absolute inset-x-0 top-0 h-64 bg-[radial-gradient(circle_at_top,_rgba(56,189,248,0.24),_transparent_60%)]" />
    <div class="pointer-events-none absolute inset-x-0 bottom-0 h-56 bg-[radial-gradient(circle_at_bottom,_rgba(14,165,233,0.12),_transparent_60%)]" />

    <main class="relative mx-auto flex min-h-screen w-full max-w-[560px] flex-col justify-center px-4 py-10 sm:px-6">
      <div class="grid gap-4">
        <AuthTitle />

        <!-- 登录页使用截图同款居中卡片，路由统一收敛到 /auth/sign-in。 -->
        <UiCard class="w-full rounded-xl border border-slate-200/80 bg-white/92 shadow-sm backdrop-blur dark:border-slate-800/80 dark:bg-slate-950/88">
          <UiCardContent class="p-5 sm:p-6">
            <!-- 统一登录页固定使用同一路由，登录成功后按角色跳转。 -->
            <LoginForm entry="user" variant="compact" />
          </UiCardContent>
        </UiCard>
      </div>
    </main>
  </div>
</template>

<route lang="yaml">
meta:
  layout: false
  guestOnly: true
  authEntry: user
</route>
