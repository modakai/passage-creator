<script setup lang="ts">
import type { AuthEntry } from '@/utils/auth-routing'

import AuthHero from './auth-hero.vue'

interface Props {
  entry: AuthEntry
}

defineProps<Props>()
</script>

<template>
  <div class="relative min-h-screen overflow-hidden bg-[linear-gradient(180deg,_#f8fbff_0%,_#f8fafc_38%,_#eef6ff_100%)] dark:bg-[linear-gradient(180deg,_#020617_0%,_#0f172a_42%,_#082f49_100%)]">
    <div class="pointer-events-none absolute inset-x-0 top-0 h-64 bg-[radial-gradient(circle_at_top,_rgba(56,189,248,0.24),_transparent_60%)]" />
    <div class="pointer-events-none absolute inset-x-0 bottom-0 h-56 bg-[radial-gradient(circle_at_bottom,_rgba(14,165,233,0.12),_transparent_60%)]" />

    <div class="relative mx-auto flex min-h-screen max-w-7xl items-center px-4 py-10 sm:px-6 lg:px-8">
      <main class="grid w-full items-stretch gap-6 lg:min-h-[520px] lg:grid-cols-[1.1fr_minmax(420px,520px)] lg:gap-8">
        <AuthHero :entry="entry" />

        <!-- 右侧固定为表单承载区，前后台只切换插槽内部内容。 -->
        <section class="flex h-full items-stretch">
          <div class="flex w-full items-center rounded-[2rem] border border-slate-200/70 bg-white/92 p-4 shadow-[0_24px_80px_rgba(15,23,42,0.12)] backdrop-blur dark:border-slate-800/80 dark:bg-slate-950/82 sm:p-6">
            <slot />
          </div>
        </section>
      </main>
    </div>
  </div>
</template>
