<script setup lang="ts">
import { CopyIcon, ExternalLinkIcon, ImagesIcon, LoaderCircleIcon, RefreshCwIcon, SearchIcon, SparklesIcon } from '@lucide/vue'
import { AiImageIcon, AiMagicIcon, AiSearchIcon, CheckmarkCircle02Icon, Download02Icon, FilePenIcon, ImageCompositionIcon } from '@hugeicons/core-free-icons'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import HugeIcon, { type HugeIconData } from '@/components/common/HugeIcon'
import { createRednoteTask, getRednoteDetail } from '@/services/api'
import { connectRednoteSse } from '@/services/sse'
import type { AppRednoteItem, RednotePhase, SseMessage } from '@/types'
import { parseJsonArray } from '@/utils/format'

interface SearchResult {
  title?: string
  sourceName?: string
  sourceUrl?: string
  summary?: string
}

interface RednoteImage {
  type?: string
  url?: string
  prompt?: string
  position?: number
}

const route = useRoute()
const content = ref(typeof route.query.prompt === 'string' ? route.query.prompt : '')
const taskId = ref('')
const activeNote = ref<AppRednoteItem | null>(null)
const isBusy = ref(false)
const isConnected = ref(false)
const errorMessage = ref('')
let closeSse: (() => void) | null = null

const phaseSteps: Array<{ phase: RednotePhase, title: string, desc: string, icon: HugeIconData }> = [
  { phase: 'PENDING', title: '任务准备', desc: '解析创作需求', icon: AiMagicIcon },
  { phase: 'SEARCH_AGENT', title: '素材检索', desc: '查找参考语境', icon: AiSearchIcon },
  { phase: 'COPY_GENERATING', title: '文案生成', desc: '生成笔记正文', icon: FilePenIcon },
  { phase: 'IMAGE_PROMPT_GENERATING', title: '图片规划', desc: '拆分封面与配图', icon: ImageCompositionIcon },
  { phase: 'IMAGE_GENERATING', title: '图片生成', desc: '生成视觉资产', icon: AiImageIcon },
  { phase: 'COMPLETED', title: '完成确认', desc: '复制和下载结果', icon: Download02Icon },
]

const currentPhase = computed<RednotePhase>(() => activeNote.value?.phase ?? (taskId.value ? 'PENDING' : 'PENDING'))
const currentStepIndex = computed(() => Math.max(0, phaseSteps.findIndex(step => step.phase === currentPhase.value)))
const isCompleted = computed(() => activeNote.value?.status === 'COMPLETED' || currentPhase.value === 'COMPLETED')
const canCreate = computed(() => content.value.trim().length > 0 && !isBusy.value)
const tags = computed(() => parseStringList(activeNote.value?.tags))
const keywords = computed(() => parseStringList(activeNote.value?.keywords))
const searchResults = computed(() => parseJsonArray<SearchResult>(activeNote.value?.searchResults))
const images = computed(() => parseJsonArray<RednoteImage>(activeNote.value?.images).filter(image => image.url && image.type !== 'COVER'))

/**
 * 解析小红书标签字段，兼容 JSON 数组和空格分隔文本。
 */
function parseStringList(value?: string) {
  const parsed = parseJsonArray<string>(value)
  if (parsed.length > 0) {
    return parsed
  }
  return value?.split(/\s+/).map(item => item.trim()).filter(Boolean) ?? []
}

/**
 * 关闭当前实时连接，防止离开页面后继续更新状态。
 */
function stopSse() {
  closeSse?.()
  closeSse = null
  isConnected.value = false
}

/**
 * 将后端详情快照应用到页面。
 */
function applyNote(note: AppRednoteItem) {
  activeNote.value = note
  taskId.value = note.taskId
  if (note.status === 'COMPLETED' || note.status === 'FAILED') {
    stopSse()
  }
}

/**
 * 从通用 workflow 事件中提取可展示字段，让页面无需等轮询也能推进。
 */
function patchNoteFromEvent(message: SseMessage) {
  const payload = message.payload ?? {}
  activeNote.value = {
    taskId: taskId.value || message.taskId || '',
    content: content.value,
    status: message.type === 'WORKFLOW_COMPLETED' ? 'COMPLETED' : message.type === 'WORKFLOW_FAILED' ? 'FAILED' : 'PROCESSING',
    phase: resolvePhase(message),
    ...(activeNote.value ?? {}),
    subject: stringify(payload.subject) ?? activeNote.value?.subject,
    context: stringify(payload.context) ?? activeNote.value?.context,
    bodyContent: stringify(payload.bodyContent) ?? activeNote.value?.bodyContent,
    coverTitle: stringify(payload.coverTitle) ?? activeNote.value?.coverTitle,
    coverImage: stringify(payload.coverImage) ?? activeNote.value?.coverImage,
    keywords: stringifyJson(payload.keywords) ?? activeNote.value?.keywords,
    searchResults: stringifyJson(payload.searchResults) ?? activeNote.value?.searchResults,
    tags: stringifyJson(payload.tags) ?? activeNote.value?.tags,
    images: stringifyJson(payload.images) ?? activeNote.value?.images,
  }
}

/**
 * 将 workflow 节点名转换为用户端阶段。
 */
function resolvePhase(message: SseMessage): RednotePhase {
  if (message.type === 'WORKFLOW_COMPLETED') {
    return 'COMPLETED'
  }
  if (message.type === 'WORKFLOW_FAILED' || message.type === 'ERROR') {
    return 'FAILED'
  }
  const nodeType = message.nodeType
  if (nodeType === 'RednoteSearchAgent' || nodeType === 'SEARCH_AGENT') {
    return 'SEARCH_AGENT'
  }
  if (nodeType === 'RednoteContentAgent' || nodeType === 'COPY_GENERATING') {
    return 'COPY_GENERATING'
  }
  if (nodeType === 'RednoteNormalImagePromptAgent' || nodeType === 'RednoteCoverImagePromptAgent' || nodeType === 'IMAGE_PROMPT_GENERATING') {
    return 'IMAGE_PROMPT_GENERATING'
  }
  if (nodeType === 'IMAGE_PROMPT_COMPLETED' || nodeType === 'NORMAL_IMAGE_GENERATING' || nodeType === 'COVER_IMAGE_GENERATING') {
    return 'IMAGE_GENERATING'
  }
  return currentPhase.value
}

/**
 * 简单值转字符串，空值不覆盖当前快照。
 */
function stringify(value: unknown) {
  if (typeof value === 'string' && value.trim()) {
    return value
  }
  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value)
  }
  return undefined
}

/**
 * 数组或对象转成详情接口同款 JSON 字符串。
 */
function stringifyJson(value: unknown) {
  if (typeof value === 'string' && value.trim()) {
    return value
  }
  if (Array.isArray(value) || (value && typeof value === 'object')) {
    return JSON.stringify(value)
  }
  return undefined
}

/**
 * 开启小红书实时创作连接。
 */
function startSse(nextTaskId: string) {
  stopSse()
  taskId.value = nextTaskId
  isConnected.value = true
  closeSse = connectRednoteSse(nextTaskId, {
    onMessage(message) {
      patchNoteFromEvent(message)
      if (message.type === 'WORKFLOW_COMPLETED') {
        void refreshDetail(true)
      }
    },
    onError() {
      isConnected.value = false
    },
  })
}

/**
 * 拉取任务详情，用于刷新恢复和终态补全。
 */
async function refreshDetail(silent = false) {
  if (!taskId.value) {
    return
  }
  if (!silent) {
    isBusy.value = true
  }
  try {
    applyNote(await getRednoteDetail(taskId.value))
  }
  catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '获取小红书详情失败'
  }
  finally {
    isBusy.value = false
  }
}

/**
 * 创建小红书任务，后端自动完成搜索、文案和图片流程。
 */
async function createTask() {
  if (!canCreate.value) {
    return
  }
  isBusy.value = true
  errorMessage.value = ''
  try {
    const nextTaskId = await createRednoteTask(content.value.trim())
    activeNote.value = { taskId: nextTaskId, content: content.value.trim(), status: 'PENDING', phase: 'PENDING' }
    startSse(nextTaskId)
  }
  catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '创建小红书任务失败'
  }
  finally {
    isBusy.value = false
  }
}

/**
 * 复制笔记正文，保留换行和标签。
 */
async function copyBody() {
  await navigator.clipboard.writeText(activeNote.value?.bodyContent ?? '')
}

onMounted(() => {
  if (typeof route.query.taskId === 'string') {
    taskId.value = route.query.taskId
    void refreshDetail()
  }
})
onBeforeUnmount(stopSse)
</script>

<template>
  <div class="space-y-8">
    <section class="flex flex-col gap-5 md:flex-row md:items-end md:justify-between">
      <div>
        <div class="mb-3 inline-flex items-center gap-2 rounded-full border border-rose-100 bg-rose-50 px-3 py-1.5 text-sm text-rose-700">
          <SparklesIcon class="size-4" />
          小红书智能创作
        </div>
        <h1 class="text-4xl font-semibold tracking-[-0.06em] text-slate-950 sm:text-5xl">从生活灵感到图文笔记</h1>
        <p class="mt-3 max-w-2xl text-sm leading-7 text-slate-500">当前后端支持全自动生成，因此用户端突出过程透明和结果确认。</p>
      </div>
      <span class="rounded-full border px-4 py-2 text-sm" :class="isConnected ? 'border-emerald-100 bg-emerald-50 text-emerald-700' : 'border-slate-200 bg-white/70 text-slate-500'">
        {{ isConnected ? '实时连接中' : '未连接' }}
      </span>
    </section>

    <section class="grid gap-4" :class="isCompleted ? 'lg:grid-cols-1' : 'lg:grid-cols-[390px_minmax(0,1fr)]'">
      <aside v-if="!isCompleted" class="glass-panel rounded-[2rem] p-6">
        <label class="text-sm font-semibold text-slate-700">创作需求</label>
        <textarea v-model="content" class="mt-3 min-h-56 w-full resize-none rounded-3xl border border-slate-200 bg-white/80 p-4 text-sm leading-7 outline-none focus:border-rose-300" placeholder="例如：帮我写一篇夏季露营装备清单小红书笔记，200 字左右，标签 5 个，配图 3 张。" />
        <div class="mt-4 flex items-center justify-between text-xs text-slate-500">
          <span>{{ content.length }}/2000</span>
          <span>支持素材检索和图片生成</span>
        </div>
        <button type="button" class="ai-gradient mt-5 inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-2xl font-semibold text-white" :disabled="!canCreate" @click="createTask">
          <LoaderCircleIcon v-if="isBusy" class="size-4 animate-spin" />
          <SparklesIcon v-else class="size-4" />
          全自动生成
        </button>
        <button v-if="taskId" type="button" class="mt-3 inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white text-sm text-slate-600" @click="refreshDetail()">
          <RefreshCwIcon class="size-4" />
          刷新详情
        </button>
        <p v-if="errorMessage" class="mt-4 rounded-2xl border border-rose-100 bg-rose-50 p-3 text-sm text-rose-700">{{ errorMessage }}</p>
      </aside>

      <div class="space-y-6">
        <section class="glass-panel rounded-[2rem] p-4 sm:p-5">
          <!-- 流程条使用 hugeicons 强化创作阶段感，数字只作为辅助序号。 -->
          <div class="grid gap-3 md:grid-cols-3 xl:grid-cols-6">
            <div
              v-for="(step, index) in phaseSteps"
              :key="step.phase"
              class="relative rounded-[1.5rem] border p-4 transition"
              :class="index <= currentStepIndex && taskId ? 'border-blue-100 bg-white shadow-lg shadow-blue-500/5' : 'border-slate-200 bg-white/55'"
            >
              <div class="mb-4 flex items-center justify-between">
                <span class="grid size-11 place-items-center rounded-2xl" :class="index <= currentStepIndex && taskId ? 'ai-gradient text-white' : 'bg-slate-100 text-slate-400'">
                  <HugeIcon :icon="step.icon" :size="22" :stroke-width="1.7" />
                </span>
                <span class="grid size-6 place-items-center rounded-full text-[11px] font-semibold" :class="index <= currentStepIndex && taskId ? 'bg-slate-950 text-white' : 'bg-slate-100 text-slate-400'">{{ index + 1 }}</span>
              </div>
              <div class="mb-3 flex items-center gap-2">
                <HugeIcon v-if="index < currentStepIndex || currentPhase === 'COMPLETED'" :icon="CheckmarkCircle02Icon" :size="16" class="text-emerald-500" />
                <LoaderCircleIcon v-else-if="index === currentStepIndex && taskId && !isCompleted" class="size-4 animate-spin text-blue-500" />
                <span v-else class="size-4 rounded-full border border-slate-200" />
                <strong class="text-sm">{{ step.title }}</strong>
              </div>
              <p class="mt-2 text-xs leading-5 text-slate-500">{{ step.desc }}</p>
            </div>
          </div>
        </section>

        <section v-if="!taskId" class="glass-panel grid min-h-[28rem] place-items-center rounded-[2rem] p-8 text-center">
          <div>
            <SparklesIcon class="mx-auto size-10 text-slate-400" />
            <h2 class="mt-4 text-2xl font-semibold tracking-[-0.04em]">写下你的笔记目标</h2>
            <p class="mt-2 text-sm text-slate-500">系统会自动完成检索、文案、图片规划和图片生成。</p>
          </div>
        </section>

        <section v-else class="grid gap-6 xl:grid-cols-[minmax(0,1fr)_360px]">
          <article class="glass-panel rounded-[2rem] p-6">
            <div class="mb-5 flex items-start justify-between gap-3">
              <div>
                <h2 class="text-2xl font-semibold tracking-[-0.04em]">{{ activeNote?.subject || activeNote?.coverTitle || '生成中' }}</h2>
                <p class="mt-2 text-sm leading-6 text-slate-500">{{ activeNote?.context || 'AI 正在整理参考素材和创作语境。' }}</p>
              </div>
              <button type="button" class="rounded-2xl border border-slate-200 bg-white px-4 py-2 text-sm" :disabled="!activeNote?.bodyContent" @click="copyBody">
                <CopyIcon class="mr-1 inline size-4" />
                复制
              </button>
            </div>
            <pre class="min-h-80 whitespace-pre-wrap rounded-3xl border border-slate-200 bg-white/80 p-6 text-sm leading-8 text-slate-700">{{ activeNote?.bodyContent || '文案生成中...' }}</pre>
            <div v-if="tags.length" class="mt-5 flex flex-wrap gap-2">
              <span v-for="tag in tags" :key="tag" class="rounded-full bg-blue-50 px-3 py-1.5 text-sm text-blue-700">{{ tag }}</span>
            </div>
          </article>

          <aside class="space-y-6">
            <div class="glass-panel rounded-[2rem] p-5">
              <h3 class="mb-4 flex items-center gap-2 font-semibold">
                <ImagesIcon class="size-5" />
                封面与配图
              </h3>
              <img v-if="activeNote?.coverImage" :src="activeNote.coverImage" :alt="activeNote.coverTitle || '小红书封面'" class="aspect-square w-full rounded-3xl object-cover">
              <div v-else class="grid aspect-square place-items-center rounded-3xl border border-dashed text-sm text-slate-400">封面生成中</div>
              <div class="mt-4 grid grid-cols-2 gap-3">
                <img v-for="image in images" :key="image.url" :src="image.url" :alt="image.prompt || '小红书配图'" class="aspect-square rounded-2xl object-cover">
              </div>
            </div>

            <div class="glass-panel rounded-[2rem] p-5">
              <h3 class="mb-4 flex items-center gap-2 font-semibold">
                <SearchIcon class="size-5" />
                灵感来源
              </h3>
              <div v-if="keywords.length" class="mb-4 flex flex-wrap gap-2">
                <span v-for="keyword in keywords" :key="keyword" class="rounded-full bg-slate-100 px-3 py-1.5 text-xs text-slate-600">{{ keyword }}</span>
              </div>
              <div class="space-y-3">
                <a v-for="result in searchResults" :key="result.sourceUrl || result.title" :href="result.sourceUrl" target="_blank" rel="noreferrer" class="block rounded-2xl border border-slate-200 bg-white/70 p-3 text-sm transition hover:bg-white">
                  <span class="flex items-center gap-2 font-medium text-slate-800">
                    {{ result.title || result.sourceName || '检索结果' }}
                    <ExternalLinkIcon class="ml-auto size-3.5 text-slate-400" />
                  </span>
                  <span class="mt-2 block text-xs leading-5 text-slate-500">{{ result.summary }}</span>
                </a>
                <p v-if="!searchResults.length" class="text-sm leading-6 text-slate-500">等待检索结果。</p>
              </div>
            </div>
          </aside>
        </section>
      </div>
    </section>
  </div>
</template>
