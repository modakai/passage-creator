<script setup lang="ts">
import { ArrowRightIcon, CheckCircle2Icon, SparklesIcon } from '@lucide/vue'

interface Props {
  title: string
  subtitle: string
  mode: 'login' | 'register'
}

const props = defineProps<Props>()

const features = [
  '一句话进入创作流程',
  '标题与大纲由你确认',
  '文章和小红书任务实时追踪',
]
</script>

<template>
  <section class="relative overflow-hidden rounded-[2rem] border border-white/80 bg-slate-950 px-7 py-8 text-white shadow-2xl shadow-slate-950/15 sm:px-9 lg:px-10 lg:py-11">
    <div class="absolute inset-0 bg-[radial-gradient(circle_at_18%_12%,rgba(6,182,212,.28),transparent_30%),radial-gradient(circle_at_78%_18%,rgba(124,58,237,.22),transparent_28%),linear-gradient(150deg,#0f172a_0%,#172554_56%,#083344_100%)]" />
    <div class="absolute bottom-8 right-8 hidden h-44 w-28 rotate-12 rounded-full border border-white/10 bg-white/5 blur-[1px] lg:block" />

    <div class="relative flex h-full min-h-[480px] flex-col justify-between gap-10">
      <div>
        <div class="mb-7 inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/10 px-3 py-1.5 text-xs tracking-[0.22em] text-cyan-50 uppercase">
          <SparklesIcon class="size-3.5" />
          {{ props.mode === 'register' ? 'Start Creating' : 'Welcome Back' }}
        </div>
        <h1 class="max-w-xl text-4xl font-semibold leading-tight tracking-[-0.06em] sm:text-5xl">
          {{ title }}
        </h1>
        <p class="mt-5 max-w-lg text-sm leading-7 text-slate-200/82 sm:text-base">
          {{ subtitle }}
        </p>
      </div>

      <div class="grid gap-3">
        <article v-for="(feature, index) in features" :key="feature" class="flex items-start gap-3 rounded-3xl border border-white/12 bg-white/8 px-4 py-4 backdrop-blur-sm">
          <div class="mt-0.5 rounded-2xl bg-cyan-300/15 p-2 text-cyan-100">
            <CheckCircle2Icon v-if="index === 1" class="size-4" />
            <ArrowRightIcon v-else class="size-4" />
          </div>
          <p class="text-sm leading-6 text-slate-100/90">
            {{ feature }}
          </p>
        </article>
      </div>
    </div>
  </section>
</template>
