<script setup lang="ts">
import { BotIcon, CreditCardIcon, FileTextIcon, HomeIcon, SparklesIcon, UserIcon } from '@lucide/vue'

const navItems = [
  { label: '创作', path: '/', icon: HomeIcon },
  { label: '作品', path: '/works', icon: FileTextIcon },
  { label: '任务', path: '/tasks', icon: BotIcon },
  { label: '额度', path: '/credits', icon: CreditCardIcon },
  { label: '我的', path: '/profile', icon: UserIcon },
]
</script>

<template>
  <div class="min-h-screen pb-24">
    <header class="sticky top-4 z-50 px-4">
      <nav class="glass-panel mx-auto flex h-16 max-w-7xl items-center justify-between rounded-full px-4 sm:px-5">
        <RouterLink to="/" class="flex items-center gap-2 font-semibold tracking-[-0.03em]">
          <span class="ai-gradient grid size-9 place-items-center rounded-2xl text-white shadow-lg shadow-blue-500/20">
            <SparklesIcon class="size-5" />
          </span>
          <span class="hidden sm:inline">Sakura Passage AI</span>
        </RouterLink>

        <div class="hidden items-center gap-1 rounded-full bg-slate-100/80 p-1 md:flex">
          <RouterLink
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="flex items-center gap-2 rounded-full px-4 py-2 text-sm text-slate-500 transition hover:bg-white hover:text-slate-950"
            active-class="bg-slate-950 text-white shadow-sm hover:bg-slate-950 hover:text-white"
          >
            <component :is="item.icon" class="size-4" />
            {{ item.label }}
          </RouterLink>
        </div>

        <div class="flex items-center gap-2">
          <span class="hidden rounded-full border border-slate-200 bg-white/70 px-3 py-2 text-xs text-slate-600 sm:inline">
            8,420 credits
          </span>
          <span class="grid size-9 place-items-center rounded-full bg-slate-950 text-sm font-semibold text-white shadow-lg shadow-slate-900/10">
            S
          </span>
        </div>
      </nav>
    </header>

    <main class="mx-auto max-w-7xl px-4 pt-10 sm:px-6 lg:px-8">
      <RouterView />
    </main>

    <nav class="fixed inset-x-4 bottom-4 z-50 rounded-full border border-white/90 bg-white/85 p-2 shadow-2xl shadow-slate-900/10 backdrop-blur-xl md:hidden">
      <div class="grid grid-cols-5 gap-1">
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="flex flex-col items-center gap-1 rounded-full px-2 py-2 text-[11px] text-slate-500"
          active-class="bg-slate-950 text-white"
        >
          <component :is="item.icon" class="size-4" />
          {{ item.label }}
        </RouterLink>
      </div>
    </nav>
  </div>
</template>
