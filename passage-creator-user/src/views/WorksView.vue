<script setup lang="ts">
import { FileTextIcon, LoaderCircleIcon, RefreshCwIcon, SparklesIcon, WandSparklesIcon } from '@lucide/vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { listArticles, listRednotes } from '@/services/api'
import type { AppArticleItem, AppRednoteItem } from '@/types'
import { formatTime, getStatusLabel } from '@/utils/format'

const router = useRouter()
const activeFilter = ref<'all' | 'article' | 'rednote'>('all')
const isLoading = ref(false)
const articles = ref<AppArticleItem[]>([])
const rednotes = ref<AppRednoteItem[]>([])

const works = computed(() => [
  ...articles.value.map(item => ({
    id: `article-${item.taskId}`,
    type: 'article' as const,
    title: item.mainTitle || item.topic || '未命名文章',
    desc: item.subTitle || item.content || item.topic,
    status: item.status,
    phase: item.phase,
    time: item.updateTime || item.createTime,
    taskId: item.taskId,
  })),
  ...rednotes.value.map(item => ({
    id: `rednote-${item.taskId}`,
    type: 'rednote' as const,
    title: item.subject || item.coverTitle || item.content || '未命名小红书',
    desc: item.bodyContent || item.context || item.content,
    status: item.status || 'PENDING',
    phase: item.phase,
    time: item.updateTime || item.createTime,
    taskId: item.taskId,
  })),
])
const visibleWorks = computed(() => activeFilter.value === 'all' ? works.value : works.value.filter(item => item.type === activeFilter.value))

/**
 * 并行拉取两类作品记录，页面仍用卡片瀑布流展示。
 */
async function loadWorks() {
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
 * 根据作品类型进入对应创作流程恢复详情。
 */
function openWork(work: { type: 'article' | 'rednote', taskId: string }) {
  router.push({
    path: work.type === 'article' ? '/article-creator' : '/rednote-creator',
    query: { taskId: work.taskId },
  })
}

onMounted(loadWorks)
</script>

<template>
  <div class="space-y-8">
    <section class="flex flex-col gap-5 md:flex-row md:items-end md:justify-between">
      <div>
        <h1 class="text-4xl font-semibold tracking-[-0.06em] text-slate-950 sm:text-5xl">我的作品</h1>
        <p class="mt-3 max-w-2xl text-sm leading-7 text-slate-500">作品以卡片方式沉淀，适合创作者浏览和继续编辑，不做 CMS 表格。</p>
      </div>
      <button type="button" class="rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 text-sm text-slate-600" :disabled="isLoading" @click="loadWorks">
        <RefreshCwIcon class="mr-2 inline size-4" :class="{ 'animate-spin': isLoading }" />
        刷新
      </button>
    </section>

    <div class="flex flex-wrap gap-2">
      <button v-for="item in [{ label: '全部', value: 'all' }, { label: '文章', value: 'article' }, { label: '小红书', value: 'rednote' }]" :key="item.value" type="button" class="rounded-full border px-4 py-2 text-sm transition" :class="activeFilter === item.value ? 'border-slate-950 bg-slate-950 text-white' : 'border-slate-200 bg-white/70 text-slate-500 hover:bg-white'" @click="activeFilter = item.value as 'all' | 'article' | 'rednote'">
        {{ item.label }}
      </button>
    </div>

    <div v-if="isLoading" class="glass-panel grid min-h-80 place-items-center rounded-[2rem] text-slate-500">
      <LoaderCircleIcon class="mr-2 inline size-5 animate-spin" />
      正在加载作品...
    </div>

    <section v-else class="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
      <article v-for="work in visibleWorks" :key="work.id" class="group overflow-hidden rounded-[2rem] border border-white/90 bg-white/75 shadow-xl shadow-slate-900/5 transition hover:-translate-y-1 hover:bg-white">
        <div class="ai-gradient grid h-40 place-items-center text-white">
          <FileTextIcon v-if="work.type === 'article'" class="size-12 opacity-80" />
          <WandSparklesIcon v-else class="size-12 opacity-80" />
        </div>
        <div class="space-y-4 p-5">
          <div class="flex items-center justify-between gap-3">
            <span class="rounded-full border px-3 py-1 text-xs" :class="work.status === 'COMPLETED' ? 'border-emerald-100 bg-emerald-50 text-emerald-700' : 'border-blue-100 bg-blue-50 text-blue-700'">{{ getStatusLabel(work.status) }}</span>
            <span class="text-xs text-slate-400">{{ formatTime(work.time) }}</span>
          </div>
          <div>
            <h2 class="line-clamp-2 text-xl font-semibold tracking-[-0.04em]">{{ work.title }}</h2>
            <p class="mt-2 line-clamp-3 text-sm leading-6 text-slate-500">{{ work.desc }}</p>
          </div>
          <button type="button" class="w-full rounded-2xl bg-slate-950 px-4 py-3 text-sm font-semibold text-white" @click="openWork(work)">
            {{ work.status === 'COMPLETED' ? '查看作品' : '继续创作' }}
          </button>
        </div>
      </article>

      <div v-if="visibleWorks.length === 0" class="glass-panel col-span-full grid min-h-80 place-items-center rounded-[2rem] p-8 text-center">
        <div>
          <SparklesIcon class="mx-auto size-10 text-slate-400" />
          <h2 class="mt-4 text-xl font-semibold">还没有作品</h2>
          <p class="mt-2 text-sm text-slate-500">从首页输入一句话开始第一条创作。</p>
        </div>
      </div>
    </section>
  </div>
</template>
