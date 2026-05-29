<script setup lang="ts">
import { BotIcon, LoaderCircleIcon, RefreshCwIcon } from '@lucide/vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { listArticles, listRednotes } from '@/services/api'
import type { AppArticleItem, AppRednoteItem } from '@/types'
import { formatTime, getStatusLabel } from '@/utils/format'

const router = useRouter()
const isLoading = ref(false)
const articles = ref<AppArticleItem[]>([])
const rednotes = ref<AppRednoteItem[]>([])

const tasks = computed(() => [
  ...articles.value.map(item => ({ type: 'article' as const, kind: '文章', title: item.mainTitle || item.topic, status: item.status, phase: item.phase, taskId: item.taskId, time: item.updateTime })),
  ...rednotes.value.map(item => ({ type: 'rednote' as const, kind: '小红书', title: item.subject || item.coverTitle || item.content, status: item.status, phase: item.phase, taskId: item.taskId, time: item.updateTime })),
].filter(item => item.status !== 'COMPLETED'))

/**
 * 任务页只聚焦进行中和等待确认，不展示后台式全量数据表。
 */
async function loadTasks() {
  isLoading.value = true
  try {
    const [articlePage, rednotePage] = await Promise.all([listArticles(), listRednotes()])
    articles.value = articlePage.records ?? []
    rednotes.value = rednotePage.records ?? []
  }
  finally {
    isLoading.value = false
  }
}

/**
 * 继续处理时进入对应创作页，由创作页用 taskId 恢复流程状态。
 */
function continueTask(task: { type: 'article' | 'rednote', taskId: string }) {
  router.push({
    path: task.type === 'article' ? '/article-creator' : '/rednote-creator',
    query: { taskId: task.taskId },
  })
}

onMounted(loadTasks)
</script>

<template>
  <div class="space-y-8">
    <section class="flex flex-col gap-5 md:flex-row md:items-end md:justify-between">
      <div>
        <h1 class="text-4xl font-semibold tracking-[-0.06em] text-slate-950 sm:text-5xl">创作任务</h1>
        <p class="mt-3 max-w-2xl text-sm leading-7 text-slate-500">把 AI 正在做什么、哪里需要你确认讲清楚，而不是堆字段。</p>
      </div>
      <button type="button" class="rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 text-sm text-slate-600" :disabled="isLoading" @click="loadTasks">
        <RefreshCwIcon class="mr-2 inline size-4" :class="{ 'animate-spin': isLoading }" />
        刷新
      </button>
    </section>

    <div v-if="isLoading" class="glass-panel grid min-h-80 place-items-center rounded-[2rem] text-slate-500">
      <LoaderCircleIcon class="mr-2 inline size-5 animate-spin" />
      正在同步任务...
    </div>

    <section v-else class="space-y-4">
      <article v-for="task in tasks" :key="task.taskId" class="glass-panel grid gap-4 rounded-[2rem] p-5 md:grid-cols-[minmax(0,1fr)_260px_140px] md:items-center">
        <div class="min-w-0">
          <div class="mb-2 flex items-center gap-2 text-sm text-slate-500">
            <BotIcon class="size-4" />
            {{ task.kind }} · {{ task.taskId }}
          </div>
          <h2 class="line-clamp-2 text-xl font-semibold tracking-[-0.04em]">{{ task.title || '未命名任务' }}</h2>
          <p class="mt-2 text-sm text-slate-500">当前节点：{{ task.phase || '等待处理' }}</p>
        </div>
        <div>
          <span class="rounded-full border border-blue-100 bg-blue-50 px-3 py-1.5 text-sm text-blue-700">{{ getStatusLabel(task.status) }}</span>
          <div class="mt-3 h-2 overflow-hidden rounded-full bg-slate-100">
            <span class="block h-full w-2/3 rounded-full ai-gradient" />
          </div>
          <p class="mt-2 text-xs text-slate-400">更新：{{ formatTime(task.time) }}</p>
        </div>
        <button type="button" class="rounded-2xl bg-slate-950 px-4 py-3 text-sm font-semibold text-white" @click="continueTask(task)">继续处理</button>
      </article>

      <div v-if="tasks.length === 0" class="glass-panel grid min-h-80 place-items-center rounded-[2rem] p-8 text-center">
        <div>
          <BotIcon class="mx-auto size-10 text-slate-400" />
          <h2 class="mt-4 text-xl font-semibold">暂无进行中任务</h2>
          <p class="mt-2 text-sm text-slate-500">所有创作流程都已完成或尚未开始。</p>
        </div>
      </div>
    </section>
  </div>
</template>
