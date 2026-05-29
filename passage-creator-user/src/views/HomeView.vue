<script setup lang="ts">
import { ArrowRightIcon, BotIcon, CheckCircle2Icon, PenLineIcon, SparklesIcon, WandSparklesIcon } from '@lucide/vue'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { inspirationCards, workflowSteps } from '@/data/ui'

const router = useRouter()
const prompt = ref('')
const mode = ref<'article' | 'rednote'>('article')

/**
 * 首页只收一句自然语言，并把上下文交给对应创作流程页面。
 */
function startCreation() {
  const value = prompt.value.trim()
  if (!value) {
    return
  }
  router.push({
    path: mode.value === 'article' ? '/article-creator' : '/rednote-creator',
    query: { prompt: value },
  })
}

/**
 * 灵感卡片直接填充输入框，帮助用户从空白状态开始。
 */
function useInspiration(value: string) {
  prompt.value = value
  mode.value = value.includes('小红书') ? 'rednote' : 'article'
}
</script>

<template>
  <section class="glass-panel relative overflow-hidden rounded-[2.75rem] px-5 py-14 text-center sm:px-10 lg:px-14 lg:py-20">
    <div class="pointer-events-none absolute left-1/2 top-0 size-[32rem] -translate-x-1/2 -translate-y-2/3 rounded-full bg-[conic-gradient(from_120deg,rgba(124,58,237,.22),rgba(59,130,246,.18),rgba(6,182,212,.14),rgba(124,58,237,.22))] blur-3xl" />

    <div class="relative mx-auto max-w-4xl">
      <div class="mb-6 inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white/75 px-4 py-2 text-sm text-slate-500">
        <span class="size-2 rounded-full bg-cyan-400 shadow-[0_0_18px_rgba(6,182,212,.75)]" />
        AI 创作工作流 · 人机协作确认
      </div>
      <h1 class="ai-text text-5xl font-semibold leading-[1.06] tracking-[-0.07em] sm:text-6xl lg:text-7xl">
        让灵感，从一句话开始生长。
      </h1>
      <p class="mx-auto mt-6 max-w-2xl text-base leading-8 text-slate-500 sm:text-lg">
        输入主题，AI 会把选题拆解成可确认的标题、大纲、正文和配图流程。你只在关键节点做判断，其余交给创作智能体推进。
      </p>

      <div class="mx-auto mt-10 max-w-4xl rounded-[2rem] border border-slate-200 bg-white/90 p-4 text-left shadow-2xl shadow-slate-900/10">
        <textarea
          v-model="prompt"
          class="min-h-32 w-full resize-none rounded-[1.5rem] border-0 bg-transparent p-3 text-base leading-7 text-slate-900 outline-none placeholder:text-slate-400"
          placeholder="例如：写一篇关于 AI 如何改变内容创作流程的深度文章..."
          @keydown.meta.enter="startCreation"
          @keydown.ctrl.enter="startCreation"
        />
        <div class="flex flex-col gap-4 border-t border-slate-100 pt-4 sm:flex-row sm:items-center sm:justify-between">
          <div class="flex flex-wrap gap-2">
            <button
              class="rounded-full border px-4 py-2 text-sm transition"
              :class="mode === 'article' ? 'border-slate-950 bg-slate-950 text-white' : 'border-slate-200 bg-slate-50 text-slate-600 hover:bg-white'"
              type="button"
              @click="mode = 'article'"
            >
              <PenLineIcon class="mr-1 inline size-4" />
              深度文章
            </button>
            <button
              class="rounded-full border px-4 py-2 text-sm transition"
              :class="mode === 'rednote' ? 'border-slate-950 bg-slate-950 text-white' : 'border-slate-200 bg-slate-50 text-slate-600 hover:bg-white'"
              type="button"
              @click="mode = 'rednote'"
            >
              <WandSparklesIcon class="mr-1 inline size-4" />
              小红书笔记
            </button>
          </div>
          <button type="button" class="ai-gradient inline-flex min-h-12 items-center justify-center gap-2 rounded-2xl px-6 font-semibold text-white shadow-xl shadow-blue-500/20 transition hover:-translate-y-0.5" :disabled="!prompt.trim()" @click="startCreation">
            开始创作
            <ArrowRightIcon class="size-4" />
          </button>
        </div>
      </div>
    </div>

    <div class="relative mt-8 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
      <button
        v-for="card in inspirationCards"
        :key="card.title"
        type="button"
        class="subtle-card rounded-3xl p-5 text-left transition hover:-translate-y-1 hover:bg-white"
        @click="useInspiration(card.prompt)"
      >
        <strong class="block text-base tracking-[-0.03em]">{{ card.title }}</strong>
        <span class="mt-2 block text-sm leading-6 text-slate-500">{{ card.desc }}</span>
      </button>
    </div>
  </section>

  <section class="mt-8 grid gap-6 lg:grid-cols-[1.2fr_.8fr]">
    <div class="glass-panel rounded-[2rem] p-6 sm:p-8">
      <div class="mb-6 flex items-center justify-between gap-4">
        <div>
          <h2 class="text-2xl font-semibold tracking-[-0.05em]">创作流程可视化</h2>
          <p class="mt-2 text-sm leading-6 text-slate-500">每一步都可见，等待确认的节点会明确提醒。</p>
        </div>
        <BotIcon class="size-8 text-slate-400" />
      </div>
      <div class="grid gap-3 md:grid-cols-5">
        <div v-for="(step, index) in workflowSteps" :key="step.title" class="relative rounded-3xl border border-slate-200 bg-white/70 p-4">
          <div class="mb-4 flex items-center justify-between">
            <span class="grid size-8 place-items-center rounded-full bg-slate-950 text-xs font-semibold text-white">{{ index + 1 }}</span>
            <CheckCircle2Icon v-if="index < 3" class="size-4 text-emerald-500" />
          </div>
          <strong class="block text-sm">{{ step.title }}</strong>
          <p class="mt-2 text-xs leading-5 text-slate-500">{{ step.desc }}</p>
        </div>
      </div>
    </div>

    <div class="glass-panel rounded-[2rem] p-6 sm:p-8">
      <div class="mb-5 flex items-center gap-3">
        <span class="ai-gradient grid size-11 place-items-center rounded-2xl text-white">
          <SparklesIcon class="size-5" />
        </span>
        <div>
          <h2 class="text-xl font-semibold tracking-[-0.04em]">本月创作效率</h2>
          <p class="text-sm text-slate-500">以创作者视角展示，不做后台仪表盘。</p>
        </div>
      </div>
      <div class="grid grid-cols-2 gap-3">
        <div class="rounded-3xl bg-white/70 p-5">
          <b class="text-3xl tracking-[-0.06em]">42h</b>
          <span class="mt-2 block text-sm text-slate-500">预计节省时间</span>
        </div>
        <div class="rounded-3xl bg-white/70 p-5">
          <b class="text-3xl tracking-[-0.06em]">18</b>
          <span class="mt-2 block text-sm text-slate-500">完成作品</span>
        </div>
        <div class="rounded-3xl bg-white/70 p-5">
          <b class="text-3xl tracking-[-0.06em]">7</b>
          <span class="mt-2 block text-sm text-slate-500">等待确认</span>
        </div>
        <div class="rounded-3xl bg-white/70 p-5">
          <b class="text-3xl tracking-[-0.06em]">86</b>
          <span class="mt-2 block text-sm text-slate-500">生成配图</span>
        </div>
      </div>
    </div>
  </section>
</template>
