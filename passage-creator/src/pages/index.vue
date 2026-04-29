<script setup lang="ts">
import {
  FileTextIcon,
  LayoutListIcon,
  PenLineIcon,
  RocketIcon,
  SparklesIcon,
} from '@lucide/vue'

import UserPortalHeader from '@/components/user-portal/user-portal-header.vue'

const router = useRouter()
const topic = ref('')

const capabilities = [
  {
    title: '智能生成标题',
    desc: 'AI 自动分析选题，生成吸引眼球的爆款标题',
    icon: FileTextIcon,
    color: 'emerald',
  },
  {
    title: '自动生成大纲',
    desc: '智能规划文章结构，确保逻辑清晰完整',
    icon: LayoutListIcon,
    color: 'blue',
  },
  {
    title: '流式生成正文',
    desc: '实时展示创作过程，体验打字机般的流畅输出',
    icon: PenLineIcon,
    color: 'violet',
  },
]

/**
 * 从首页进入创作页，保留用户已输入的选题。
 */
function startCreation() {
  const normalizedTopic = topic.value.trim()
  router.push({
    path: '/article-creator',
    query: normalizedTopic ? { topic: normalizedTopic } : undefined,
  })
}
</script>

<template>
  <div class="min-h-screen bg-white text-slate-950">
    <UserPortalHeader />

    <main>
      <section class="relative overflow-hidden bg-gradient-to-b from-emerald-50 via-emerald-50/70 to-white">
        <div class="mx-auto flex min-h-[620px] max-w-[1180px] flex-col items-center px-5 pb-24 pt-24 text-center lg:pt-28">
          <div class="inline-flex items-center gap-2 rounded-full border border-emerald-200 bg-emerald-100/70 px-5 py-2 text-sm font-semibold text-emerald-600">
            <SparklesIcon class="size-4" />
            AI 驱动的内容创作平台
          </div>

          <h1 class="mt-9 text-5xl font-black tracking-tight text-emerald-600 sm:text-6xl lg:text-7xl">
            AI 爆款文章创作器
          </h1>
          <p class="mt-7 text-xl text-slate-600 sm:text-2xl">
            让每个人都能写出 10万+ 文章
          </p>

          <div class="mt-14 w-full max-w-[860px] rounded-2xl border border-slate-200 bg-white p-2 shadow-[0_18px_45px_rgba(15,23,42,0.16)]">
            <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
              <div class="flex min-h-16 flex-1 items-center gap-3 px-4">
                <PenLineIcon class="size-6 shrink-0 text-slate-400" />
                <input
                  v-model="topic"
                  class="h-12 min-w-0 flex-1 bg-transparent text-lg text-slate-700 outline-none placeholder:text-slate-300"
                  placeholder="输入您想创作的文章选题，例如：2026年AI如何改变职场"
                  @keydown.enter="startCreation"
                >
              </div>
              <UiButton
                class="h-16 rounded-xl bg-emerald-500 px-9 text-lg font-semibold text-white shadow-[0_12px_28px_rgba(34,197,94,0.28)] hover:bg-emerald-600"
                @click="startCreation"
              >
                <RocketIcon class="mr-2 size-5" />
                开始创作
              </UiButton>
            </div>
          </div>

          <p class="mt-8 text-base text-slate-400">
            工作总结、心得体会、演讲稿、分析报告... 一键生成
          </p>
        </div>
      </section>

      <section class="bg-slate-50/80 px-5 py-24">
        <div class="mx-auto max-w-[1180px]">
          <div class="text-center">
            <div class="inline-flex rounded-full bg-emerald-50 px-5 py-2 text-sm font-semibold text-emerald-600">
              核心能力
            </div>
            <h2 class="mt-7 text-4xl font-black tracking-tight sm:text-5xl">
              专业人士的一站式AI写作工具
            </h2>
            <p class="mt-6 text-xl text-slate-500">
              强大的 AI 能力，让创作变得简单高效
            </p>
          </div>

          <div class="mt-16 grid gap-6 md:grid-cols-3">
            <div
              v-for="item in capabilities"
              :key="item.title"
              class="rounded-xl border border-slate-200 bg-white p-8 shadow-[0_14px_40px_rgba(15,23,42,0.04)] transition hover:-translate-y-1 hover:shadow-[0_18px_50px_rgba(15,23,42,0.08)]"
            >
              <div
                class="grid size-14 place-items-center rounded-xl"
                :class="{
                  'bg-emerald-50 text-emerald-500': item.color === 'emerald',
                  'bg-blue-50 text-blue-500': item.color === 'blue',
                  'bg-violet-50 text-violet-500': item.color === 'violet',
                }"
              >
                <component :is="item.icon" class="size-7" />
              </div>
              <h3 class="mt-5 text-xl font-bold">
                {{ item.title }}
              </h3>
              <p class="mt-3 text-base leading-7 text-slate-500">
                {{ item.desc }}
              </p>
            </div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<route lang="yaml">
meta:
  layout: blank
</route>
