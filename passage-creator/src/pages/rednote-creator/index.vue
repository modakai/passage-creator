<script setup lang="ts">
import {
  CheckCircle2Icon,
  ClipboardIcon,
  DownloadIcon,
  ExternalLinkIcon,
  FileTextIcon,
  ImagesIcon,
  LoaderCircleIcon,
  RefreshCwIcon,
  SearchIcon,
  SparklesIcon,
  WandSparklesIcon,
} from '@lucide/vue'
import { toast } from 'vue-sonner'

import type {
  AppRednoteItem,
  RednoteImagePromptItem,
  RednoteImageResult,
  RednotePhase,
  RednoteSearchResult,
  RednoteSseMessage,
} from '@/services/types/app-rednote.type'

import {
  createAppRednoteTask,
  downloadAppRednoteImage,
  getAppRednoteDetail,
  listAppRednotePage,
  retryAppRednoteTask,
} from '@/services/api/app-rednote.api'
import { connectRednoteSse } from '@/utils/rednote-sse'

const route = useRoute()
const router = useRouter()

const content = ref('')
const taskId = ref('')
const activeNote = ref<AppRednoteItem | null>(null)
const recentNotes = ref<AppRednoteItem[]>([])
const isCreating = ref(false)
const isLoadingDetail = ref(false)
const isLoadingHistory = ref(false)
const isRetrying = ref(false)
const isConnected = ref(false)
let closeSse: (() => void) | null = null

const maxContentLength = 2000
// 关键词字段可能是空白分隔格式，复用正则避免每次解析时重复创建。
const whitespaceSeparatorPattern = /\s+/

const phaseSteps: Array<{ phase: RednotePhase, title: string, desc: string }> = [
  { phase: 'PENDING', title: '任务排队中', desc: '正在准备创作任务' },
  { phase: 'SEARCH_AGENT', title: '网页检索中', desc: '正在查找可参考素材' },
  { phase: 'COPY_GENERATING', title: '文案生成中', desc: '正在撰写小红书正文' },
  { phase: 'IMAGE_PROMPT_GENERATING', title: '图片规划中', desc: '正在整理封面和配图需求' },
  { phase: 'IMAGE_GENERATING', title: '图片生成中', desc: '正在生成封面和配图' },
  { phase: 'COMPLETED', title: '生成完成', desc: '内容和图片已保存' },
]

const phaseDisplayLabels: Record<string, string> = {
  PENDING: '任务排队中',
  SEARCH_AGENT: '网页检索中',
  COPY_GENERATING: '文案生成中',
  IMAGE_PROMPT_GENERATING: '图片规划中',
  IMAGE_GENERATING: '图片生成中',
  COMPLETED: '生成完成',
  FAILED: '生成失败',
}

const statusDisplayLabels: Record<string, string> = {
  PENDING: '等待处理',
  PROCESSING: '生成中',
  COMPLETED: '已完成',
  FAILED: '生成失败',
}

const currentPhase = computed<RednotePhase>(() => activeNote.value?.phase ?? (taskId.value ? 'PENDING' : 'PENDING'))
const currentStatus = computed(() => activeNote.value?.status ?? (taskId.value ? 'PENDING' : undefined))
const isCompleted = computed(() => currentStatus.value === 'COMPLETED' || currentPhase.value === 'COMPLETED')
const isFailed = computed(() => currentStatus.value === 'FAILED' || currentPhase.value === 'FAILED')
const isRunning = computed(() => Boolean(taskId.value) && !isCompleted.value && !isFailed.value)
const contentLength = computed(() => content.value.length)
const canCreate = computed(() => content.value.trim().length > 0 && contentLength.value <= maxContentLength && !isCreating.value)
const activeStepIndex = computed(() => Math.max(0, phaseSteps.findIndex(step => step.phase === currentPhase.value)))
const progressValue = computed(() => {
  if (!taskId.value) {
    return 0
  }
  if (isFailed.value) {
    return 100
  }
  return Math.round(((activeStepIndex.value + 1) / phaseSteps.length) * 100)
})

const tags = computed(() => parseStringArray(activeNote.value?.tags))
const keywords = computed(() => parseStringArray(activeNote.value?.keywords))
const searchResults = computed(() => parseJsonArray<RednoteSearchResult>(activeNote.value?.searchResults))
const visibleSearchResults = computed(() => searchResults.value.filter(result => result.title || result.sourceName || result.summary || result.sourceUrl))
const imagePrompts = computed(() => parseJsonArray<RednoteImagePromptItem>(activeNote.value?.imagePrompts))
const normalImages = computed(() => parseJsonArray<RednoteImageResult>(activeNote.value?.images)
  .filter(image => image.url && image.type !== 'COVER')
  .sort((left, right) => (left.position ?? 999) - (right.position ?? 999)))
const coverImage = computed(() => activeNote.value?.coverImage ?? '')
const hasResult = computed(() => Boolean(activeNote.value?.bodyContent || coverImage.value || normalImages.value.length > 0))

/**
 * 安全解析后端 JSON 数组字段，坏数据只影响当前展示，不阻断页面。
 */
function parseJsonArray<T>(value?: string) {
  if (!value) {
    return []
  }
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed as T[] : []
  }
  catch {
    return []
  }
}

/**
 * 解析标签和关键词字段，兼容 JSON 数组与空格分隔的 hashtag 字符串。
 */
function parseStringArray(value?: string) {
  if (!value) {
    return []
  }
  const parsed = parseJsonArray<string>(value)
  if (parsed.length > 0) {
    return parsed
  }
  return value.split(whitespaceSeparatorPattern).map(item => item.trim()).filter(Boolean)
}

/**
 * 优先使用前端中文阶段名，避免把后端节点名展示给用户。
 */
function getPhaseDisplayLabel(note?: AppRednoteItem | null) {
  const phase = note?.phase
  if (phase && phaseDisplayLabels[phase]) {
    return phaseDisplayLabels[phase]
  }
  return note?.phaseLabel || note?.statusLabel || '未开始'
}

/**
 * 统一将状态值转成中文，兜底时才使用后端标签。
 */
function getStatusDisplayLabel(note?: AppRednoteItem | null) {
  const status = note?.status
  if (status && statusDisplayLabels[status]) {
    return statusDisplayLabels[status]
  }
  return note?.statusLabel || '等待处理'
}

/**
 * 搜索结果标题优先取网页标题，缺失时用来源名称兜底。
 */
function getSearchResultTitle(result: RednoteSearchResult) {
  return result.title || result.sourceName || result.sourceUrl || '检索结果'
}

/**
 * 只从 URL 读取恢复任务，避免本地缓存让直接打开创作页时误进入旧任务。
 */
function resolveInitialTaskId() {
  const queryTaskId = typeof route.query.taskId === 'string' ? route.query.taskId.trim() : ''
  return queryTaskId
}

/**
 * 将当前 taskId 同步到 URL，便于复制链接或刷新恢复。
 */
function syncTaskIdToRoute(nextTaskId: string) {
  if (route.query.taskId === nextTaskId) {
    return
  }
  void router.replace({
    path: route.path,
    query: {
      ...route.query,
      taskId: nextTaskId,
    },
  })
}

/**
 * 关闭 SSE，避免页面切换后仍接收旧任务消息。
 */
function stopRealtime() {
  closeSse?.()
  closeSse = null
  isConnected.value = false
}

/**
 * 将后端详情快照应用到当前页面状态。
 */
function applyNote(note: AppRednoteItem) {
  activeNote.value = note
  taskId.value = note.taskId
  content.value = note.content ?? content.value
  if (note.status === 'COMPLETED' || note.status === 'FAILED') {
    stopRealtime()
    void loadRecentNotes()
  }
}

/**
 * 拉取当前任务详情，SSE 事件和页面恢复共用这一条恢复路径。
 */
async function refreshActiveDetail(silent = false) {
  if (!taskId.value) {
    return
  }
  if (!silent) {
    isLoadingDetail.value = true
  }
  try {
    const response = await getAppRednoteDetail(taskId.value)
    if (response.data) {
      applyNote(response.data)
    }
  }
  catch (error) {
    if (!silent) {
      toast.error(error instanceof Error ? error.message : '获取小红书任务详情失败')
    }
  }
  finally {
    isLoadingDetail.value = false
  }
}

/**
 * 加载最近创作记录，右侧历史列表用来快速切换查看结果。
 */
async function loadRecentNotes() {
  isLoadingHistory.value = true
  try {
    const response = await listAppRednotePage({
      page: 1,
      pageSize: 8,
      status: '',
      phase: '',
    })
    recentNotes.value = response.data?.records ?? []
  }
  finally {
    isLoadingHistory.value = false
  }
}

/**
 * 根据通用 workflow 事件推进页面状态，避免像轮询一样反复请求详情接口。
 */
function applyWorkflowEvent(message: RednoteSseMessage) {
  const progressNote = toRednoteItem(message.data)
  if (message.type === 'PROGRESS' && progressNote) {
    applyNote(progressNote)
    return
  }
  const snapshotNote = toRednoteItem(message.payload?.snapshot)
  if (snapshotNote) {
    applyNote(snapshotNote)
    return
  }

  applyWorkflowPayload(message)

  if (message.type === 'WORKFLOW_COMPLETED' || message.type === 'WORKFLOW_FAILED') {
    void refreshActiveDetail(true)
  }
}

/**
 * 只接受带 taskId 的任务快照，避免通用 workflow 事件被误当成 RednoteNote。
 */
function toRednoteItem(value: unknown): AppRednoteItem | null {
  if (!value || typeof value !== 'object') {
    return null
  }
  const note = value as AppRednoteItem
  return typeof note.taskId === 'string' && note.taskId.trim().length > 0 ? note : null
}

/**
 * 把 workflow state 直接投影到页面模型，参考 article 模块由 SSE 消息本身驱动展示。
 */
function applyWorkflowPayload(message: RednoteSseMessage) {
  const patch = buildNotePatchFromWorkflowEvent(message)
  if (Object.keys(patch).length === 0) {
    return
  }
  activeNote.value = {
    taskId: taskId.value || message.taskId || '',
    content: content.value,
    status: 'PROCESSING',
    phase: 'PENDING',
    ...(activeNote.value ?? {}),
    ...patch,
  }
}

/**
 * 从通用 workflow 事件中提取 rednote 可展示字段。
 */
function buildNotePatchFromWorkflowEvent(message: RednoteSseMessage): Partial<AppRednoteItem> {
  const payload = message.payload ?? {}
  const patch: Partial<AppRednoteItem> = {}
  const nextPhase = resolvePhaseFromWorkflowEvent(message)

  if (nextPhase) {
    patch.phase = nextPhase
  }
  if (message.type === 'WORKFLOW_STARTED' || message.type === 'NODE_STARTED' || message.type === 'NODE_RESULT' || message.type === 'NODE_RETRYING') {
    patch.status = 'PROCESSING'
  }
  if (message.type === 'WORKFLOW_COMPLETED') {
    patch.status = 'COMPLETED'
    patch.phase = 'COMPLETED'
  }
  if (message.type === 'WORKFLOW_FAILED' || message.type === 'ERROR') {
    patch.status = 'FAILED'
    patch.phase = 'FAILED'
    patch.errorMessage = toStringField(payload.error ?? message.message)
  }

  assignStringField(patch, 'subject', payload.subject)
  assignStringField(patch, 'context', payload.context)
  assignStringField(patch, 'contentLength', payload.contentLength)
  assignStringField(patch, 'bodyContent', payload.bodyContent)
  assignStringField(patch, 'coverTitle', payload.coverTitle)
  assignStringField(patch, 'coverPrompt', payload.coverPrompt)
  assignStringField(patch, 'coverImage', payload.coverImage)
  assignNumberField(patch, 'targetWordCount', payload.targetWordCount)
  assignNumberField(patch, 'tagCount', payload.tagCount)
  assignNumberField(patch, 'imageCount', payload.imageCount)
  assignJsonField(patch, 'keywords', payload.keywords)
  assignJsonField(patch, 'searchResults', payload.searchResults)
  assignJsonField(patch, 'tags', payload.tags)
  assignJsonField(patch, 'imagePrompts', payload.imagePrompts)
  assignJsonField(patch, 'images', payload.images)

  return patch
}

/**
 * 根据 workflow 节点名换算业务阶段，避免前端依赖后端数据库详情才能推进进度条。
 */
function resolvePhaseFromWorkflowEvent(message: RednoteSseMessage): RednotePhase | null {
  if (message.type === 'WORKFLOW_COMPLETED') {
    return 'COMPLETED'
  }
  if (message.type === 'WORKFLOW_FAILED' || message.type === 'ERROR') {
    return 'FAILED'
  }

  const nodeType = message.nodeType
  if (!nodeType) {
    return null
  }
  if (nodeType === 'RednoteSearchAgent' || nodeType === 'SEARCH_AGENT') {
    return 'SEARCH_AGENT'
  }
  if (nodeType === 'RednoteContentAgent' || nodeType === 'COPY_GENERATING') {
    return 'COPY_GENERATING'
  }
  if (
    nodeType === 'RednoteNormalImagePromptAgent'
    || nodeType === 'RednoteCoverImagePromptAgent'
    || nodeType === 'IMAGE_PROMPT_GENERATING'
  ) {
    return 'IMAGE_PROMPT_GENERATING'
  }
  if (nodeType === 'IMAGE_PROMPT_COMPLETED' || nodeType === 'NORMAL_IMAGE_GENERATING' || nodeType === 'COVER_IMAGE_GENERATING') {
    return 'IMAGE_GENERATING'
  }
  if (nodeType === 'COMPLETED') {
    return 'COMPLETED'
  }
  return null
}

/**
 * 写入字符串字段，空值不覆盖已有展示。
 */
function assignStringField<K extends keyof AppRednoteItem>(patch: Partial<AppRednoteItem>, key: K, value: unknown) {
  const nextValue = toStringField(value)
  if (nextValue !== undefined) {
    patch[key] = nextValue as AppRednoteItem[K]
  }
}

/**
 * 写入数值字段，非法数值不覆盖已有展示。
 */
function assignNumberField<K extends keyof AppRednoteItem>(patch: Partial<AppRednoteItem>, key: K, value: unknown) {
  const nextValue = typeof value === 'number' ? value : Number(value)
  if (Number.isFinite(nextValue)) {
    patch[key] = nextValue as AppRednoteItem[K]
  }
}

/**
 * 写入 JSON 字段，后端 state 传数组/对象时转成页面已使用的字符串格式。
 */
function assignJsonField<K extends keyof AppRednoteItem>(patch: Partial<AppRednoteItem>, key: K, value: unknown) {
  const nextValue = toJsonField(value)
  if (nextValue !== undefined) {
    patch[key] = nextValue as AppRednoteItem[K]
  }
}

/**
 * 将任意简单值转成字符串，数组和对象交给 JSON 字段专用逻辑处理。
 */
function toStringField(value: unknown) {
  if (typeof value === 'string' && value.trim().length > 0) {
    return value
  }
  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value)
  }
  return undefined
}

/**
 * 将 workflow state 中的集合字段转成后端详情接口同款 JSON 字符串。
 */
function toJsonField(value: unknown) {
  if (typeof value === 'string' && value.trim().length > 0) {
    return value
  }
  if (Array.isArray(value) || (value && typeof value === 'object')) {
    return JSON.stringify(value)
  }
  return undefined
}

/**
 * 建立 SSE，任务进度由后端事件驱动；详情接口只负责恢复和终态确认。
 */
function startRealtime(nextTaskId: string) {
  stopRealtime()
  taskId.value = nextTaskId
  syncTaskIdToRoute(nextTaskId)
  closeSse = connectRednoteSse(nextTaskId, {
    onMessage: (message) => {
      isConnected.value = true
      applyWorkflowEvent(message)
    },
    onError: () => {
      isConnected.value = false
    },
  })
}

/**
 * 创建新的小红书自动创作任务。
 */
async function handleCreate() {
  if (!canCreate.value) {
    toast.error('请输入 1-2000 字的小红书创作需求')
    return
  }
  isCreating.value = true
  try {
    const response = await createAppRednoteTask({ content: content.value.trim() })
    if (!response.data) {
      throw new Error('后端没有返回 taskId')
    }
    taskId.value = response.data
    activeNote.value = {
      taskId: response.data,
      content: content.value.trim(),
      status: 'PENDING',
      phase: 'PENDING',
      statusLabel: '等待处理',
      phaseLabel: '等待处理',
    }
    startRealtime(response.data)
    toast.success('小红书任务已创建')
  }
  catch (error) {
    toast.error(error instanceof Error ? error.message : '创建小红书任务失败')
  }
  finally {
    isCreating.value = false
  }
}

/**
 * 选择历史记录并恢复进度连接。
 */
async function openHistory(note: AppRednoteItem) {
  taskId.value = note.taskId
  applyNote(note)
  syncTaskIdToRoute(note.taskId)
  await refreshActiveDetail()
  if (note.status !== 'COMPLETED' && note.status !== 'FAILED') {
    startRealtime(note.taskId)
  }
}

/**
 * 失败任务重新生成。
 */
async function handleRetry() {
  if (!taskId.value) {
    return
  }
  isRetrying.value = true
  try {
    await retryAppRednoteTask(taskId.value)
    startRealtime(taskId.value)
    await refreshActiveDetail(true)
    toast.success('已重新开始生成')
  }
  catch (error) {
    toast.error(error instanceof Error ? error.message : '重新生成失败')
  }
  finally {
    isRetrying.value = false
  }
}

/**
 * 下载单张图片并交给浏览器保存。
 */
async function downloadImage(imageUrl: string, fileName: string) {
  if (!taskId.value || !imageUrl) {
    return
  }
  try {
    const blob = await downloadAppRednoteImage(taskId.value, imageUrl)
    const objectUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = fileName
    link.click()
    URL.revokeObjectURL(objectUrl)
  }
  catch (error) {
    toast.error(error instanceof Error ? error.message : '图片下载失败')
  }
}

/**
 * 复制小红书正文，方便用户直接粘贴到发布平台。
 */
async function copyBodyContent() {
  const text = activeNote.value?.bodyContent
  if (!text) {
    return
  }
  await navigator.clipboard.writeText(text)
  toast.success('正文已复制')
}

/**
 * 重置页面到新建任务状态。
 */
function resetCreator() {
  stopRealtime()
  taskId.value = ''
  activeNote.value = null
  content.value = ''
  void router.replace({ path: route.path, query: {} })
}

/**
 * 浏览器刷新、关闭标签页或进入 bfcache 前同步断开 SSE。
 */
function handlePageExit() {
  stopRealtime()
}

onMounted(async () => {
  window.addEventListener('pagehide', handlePageExit)
  window.addEventListener('beforeunload', handlePageExit)
  await loadRecentNotes()
  const initialTaskId = resolveInitialTaskId()
  if (initialTaskId) {
    taskId.value = initialTaskId
    await refreshActiveDetail()
    if (isRunning.value) {
      startRealtime(initialTaskId)
    }
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('pagehide', handlePageExit)
  window.removeEventListener('beforeunload', handlePageExit)
  stopRealtime()
})
</script>

<template>
  <div class="min-h-screen bg-background">
    <header class="border-b bg-background/95 px-5 py-4 lg:px-8">
      <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <div class="flex items-center gap-2 text-sm text-muted-foreground">
            <WandSparklesIcon class="size-4" />
            小红书智能创作
          </div>
          <h1 class="mt-1 text-2xl font-semibold tracking-tight">
            小红书爆款创作
          </h1>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <UiBadge v-if="taskId" variant="outline" class="max-w-[260px] truncate">
            {{ taskId }}
          </UiBadge>
          <UiBadge :variant="isConnected ? 'secondary' : 'outline'">
            {{ isConnected ? '实时连接中' : '未连接' }}
          </UiBadge>
          <UiButton variant="outline" size="sm" @click="resetCreator">
            <RefreshCwIcon class="mr-2 size-4" />
            新建
          </UiButton>
        </div>
      </div>
    </header>

    <main class="grid min-h-[calc(100vh-73px)] grid-cols-1 lg:grid-cols-[380px_minmax(0,1fr)]">
      <aside class="border-b bg-muted/30 p-5 lg:border-b-0 lg:border-r lg:p-6">
        <section class="space-y-4">
          <div>
            <h2 class="text-base font-semibold">
              创作需求
            </h2>
            <p class="mt-1 text-sm text-muted-foreground">
              用自然语言写清主题、字数、关键词、标签数和图片数量。
            </p>
          </div>
          <UiTextarea
            v-model="content"
            class="min-h-52 resize-none bg-background"
            :maxlength="maxContentLength"
            placeholder="例如：帮我写一篇关于居家胸部训练的小红书爆文，200字左右，动作数量5个，标签5个，普通配图3张。"
          />
          <div class="flex items-center justify-between text-xs text-muted-foreground">
            <span>{{ contentLength }}/{{ maxContentLength }}</span>
            <span>普通配图最多 5 张，不含封面</span>
          </div>
          <UiButton class="w-full" :disabled="!canCreate" @click="handleCreate">
            <LoaderCircleIcon v-if="isCreating" class="mr-2 size-4 animate-spin" />
            <SparklesIcon v-else class="mr-2 size-4" />
            全自动生成
          </UiButton>
        </section>

        <UiSeparator class="my-6" />

        <section class="space-y-4">
          <div class="flex items-center justify-between">
            <h2 class="text-base font-semibold">
              生成进度
            </h2>
            <UiBadge :variant="isFailed ? 'destructive' : 'secondary'">
              {{ getPhaseDisplayLabel(activeNote) }}
            </UiBadge>
          </div>
          <UiProgress :model-value="progressValue" />
          <div class="space-y-3">
            <div
              v-for="(step, index) in phaseSteps"
              :key="step.phase"
              class="flex items-start gap-3 rounded-lg border bg-background px-3 py-3"
              :class="index <= activeStepIndex && taskId ? 'border-primary/30' : 'border-border'"
            >
              <div
                class="grid size-8 shrink-0 place-items-center rounded-full border text-xs font-medium"
                :class="index < activeStepIndex || isCompleted ? 'border-emerald-500 bg-emerald-50 text-emerald-700' : 'border-muted-foreground/30 text-muted-foreground'"
              >
                <CheckCircle2Icon v-if="index < activeStepIndex || isCompleted" class="size-4" />
                <LoaderCircleIcon v-else-if="index === activeStepIndex && isRunning" class="size-4 animate-spin" />
                <span v-else>{{ index + 1 }}</span>
              </div>
              <div class="min-w-0">
                <div class="text-sm font-medium">
                  {{ step.title }}
                </div>
                <div class="text-xs text-muted-foreground">
                  {{ step.desc }}
                </div>
              </div>
            </div>
          </div>
          <UiButton
            v-if="isFailed"
            variant="destructive"
            class="w-full"
            :disabled="isRetrying"
            @click="handleRetry"
          >
            <LoaderCircleIcon v-if="isRetrying" class="mr-2 size-4 animate-spin" />
            <RefreshCwIcon v-else class="mr-2 size-4" />
            重新生成失败任务
          </UiButton>
        </section>

        <UiSeparator class="my-6" />

        <section class="space-y-3">
          <div class="flex items-center justify-between">
            <h2 class="text-base font-semibold">
              最近任务
            </h2>
            <UiButton variant="ghost" size="sm" :disabled="isLoadingHistory" @click="loadRecentNotes">
              <RefreshCwIcon class="size-4" />
            </UiButton>
          </div>
          <div class="space-y-2">
            <button
              v-for="note in recentNotes"
              :key="note.taskId"
              type="button"
              class="w-full rounded-lg border bg-background p-3 text-left transition hover:border-primary/40"
              :class="note.taskId === taskId ? 'border-primary/60' : 'border-border'"
              @click="openHistory(note)"
            >
              <div class="line-clamp-2 text-sm font-medium">
                {{ note.subject || note.content || note.taskId }}
              </div>
              <div class="mt-2 flex items-center justify-between gap-2 text-xs text-muted-foreground">
                <span>{{ getPhaseDisplayLabel(note) }}</span>
                <span>{{ note.createTime?.slice(0, 10) }}</span>
              </div>
            </button>
            <div v-if="recentNotes.length === 0" class="rounded-lg border border-dashed p-4 text-sm text-muted-foreground">
              暂无小红书创作记录
            </div>
          </div>
        </section>
      </aside>

      <section class="min-w-0 p-5 lg:p-8">
        <div v-if="!taskId" class="grid min-h-[520px] place-items-center rounded-lg border border-dashed bg-muted/20 p-8 text-center">
          <div>
            <SparklesIcon class="mx-auto size-10 text-muted-foreground" />
            <h2 class="mt-4 text-xl font-semibold">
              输入需求后开始生成
            </h2>
            <p class="mt-2 max-w-md text-sm leading-6 text-muted-foreground">
              系统会自动完成网页检索、文案生成、图片规划和图片生成。
            </p>
          </div>
        </div>

        <div v-else class="space-y-6">
          <section class="grid gap-4 md:grid-cols-4">
            <div class="rounded-lg border bg-background p-4">
              <div class="text-xs text-muted-foreground">
                主体
              </div>
              <div class="mt-2 truncate text-sm font-semibold">
                {{ activeNote?.subject || '网页检索中' }}
              </div>
            </div>
            <div class="rounded-lg border bg-background p-4">
              <div class="text-xs text-muted-foreground">
                目标字数
              </div>
              <div class="mt-2 text-sm font-semibold">
                {{ activeNote?.targetWordCount || '-' }}
              </div>
            </div>
            <div class="rounded-lg border bg-background p-4">
              <div class="text-xs text-muted-foreground">
                普通配图
              </div>
              <div class="mt-2 text-sm font-semibold">
                {{ activeNote?.imageCount ?? normalImages.length ?? '-' }} 张
              </div>
            </div>
            <div class="rounded-lg border bg-background p-4">
              <div class="text-xs text-muted-foreground">
                状态
              </div>
              <div class="mt-2 text-sm font-semibold">
                {{ getStatusDisplayLabel(activeNote) || currentStatus || '等待处理' }}
              </div>
            </div>
          </section>

          <section v-if="isLoadingDetail" class="rounded-lg border bg-muted/20 p-6">
            <div class="flex items-center gap-3 text-sm text-muted-foreground">
              <LoaderCircleIcon class="size-5 animate-spin" />
              正在加载任务详情
            </div>
          </section>

          <section v-if="isFailed" class="rounded-lg border border-destructive/40 bg-destructive/5 p-5">
            <div class="flex items-start gap-3">
              <RefreshCwIcon class="mt-0.5 size-5 text-destructive" />
              <div>
                <h2 class="font-semibold text-destructive">
                  任务失败
                </h2>
                <p class="mt-1 text-sm text-muted-foreground">
                  {{ activeNote?.errorMessage || '暂未获取到失败原因' }}
                </p>
              </div>
            </div>
          </section>

          <section v-if="hasResult" class="grid gap-6 xl:grid-cols-[minmax(0,1fr)_360px]">
            <div class="space-y-6">
              <div class="rounded-lg border bg-background">
                <div class="flex items-center justify-between gap-3 border-b px-5 py-4">
                  <div>
                    <h2 class="flex items-center gap-2 font-semibold">
                      <FileTextIcon class="size-5" />
                      小红书内容
                    </h2>
                    <p class="mt-1 text-sm text-muted-foreground">
                      正文和标签分开展示，封面标题由后续节点单独生成。
                    </p>
                  </div>
                  <UiButton variant="outline" size="sm" :disabled="!activeNote?.bodyContent" @click="copyBodyContent">
                    <ClipboardIcon class="mr-2 size-4" />
                    复制正文
                  </UiButton>
                </div>
                <div class="space-y-4 p-5">
                  <pre class="min-h-64 whitespace-pre-wrap rounded-md bg-muted/40 p-5 text-sm leading-7">{{ activeNote?.bodyContent || '正文生成中...' }}</pre>
                  <div v-if="tags.length > 0" class="flex flex-wrap gap-2">
                    <UiBadge v-for="tag in tags" :key="tag" variant="secondary">
                      {{ tag }}
                    </UiBadge>
                  </div>
                </div>
              </div>

              <div class="rounded-lg border bg-background">
                <div class="border-b px-5 py-4">
                  <h2 class="flex items-center gap-2 font-semibold">
                    <ImagesIcon class="size-5" />
                    普通配图
                  </h2>
                </div>
                <div class="grid gap-4 p-5 sm:grid-cols-2 xl:grid-cols-3">
                  <div
                    v-for="(image, index) in normalImages"
                    :key="image.url || index"
                    class="overflow-hidden rounded-lg border bg-muted/20"
                  >
                    <img v-if="image.url" :src="image.url" :alt="image.prompt || '小红书配图'" class="aspect-square w-full object-cover">
                    <div class="space-y-3 p-3">
                      <div class="line-clamp-2 text-sm text-muted-foreground">
                        {{ image.prompt || imagePrompts[index]?.prompt || '图片提示词生成中' }}
                      </div>
                      <UiButton v-if="image.url" variant="outline" size="sm" class="w-full" @click="downloadImage(image.url, `rednote-image-${index + 1}.png`)">
                        <DownloadIcon class="mr-2 size-4" />
                        下载图片
                      </UiButton>
                    </div>
                  </div>
                  <div v-if="normalImages.length === 0" class="rounded-lg border border-dashed p-6 text-sm text-muted-foreground">
                    普通配图生成中
                  </div>
                </div>
              </div>
            </div>

            <div class="space-y-6">
              <div class="rounded-lg border bg-background">
                <div class="border-b px-5 py-4">
                  <h2 class="font-semibold">
                    封面
                  </h2>
                  <p class="mt-1 text-sm text-muted-foreground">
                    {{ activeNote?.coverTitle || '封面标题生成中' }}
                  </p>
                </div>
                <div class="space-y-4 p-5">
                  <img v-if="coverImage" :src="coverImage" :alt="activeNote?.coverTitle || '小红书封面'" class="aspect-square w-full rounded-lg object-cover">
                  <div v-else class="grid aspect-square place-items-center rounded-lg border border-dashed text-sm text-muted-foreground">
                    封面生成中
                  </div>
                  <UiButton v-if="coverImage" variant="outline" class="w-full" @click="downloadImage(coverImage, 'rednote-cover.png')">
                    <DownloadIcon class="mr-2 size-4" />
                    下载封面
                  </UiButton>
                </div>
              </div>

              <div class="rounded-lg border bg-background">
                <div class="border-b px-5 py-4">
                  <h2 class="font-semibold">
                    检索结果
                  </h2>
                </div>
                <div class="space-y-4 p-5 text-sm">
                  <p v-if="visibleSearchResults.length === 0" class="leading-6 text-muted-foreground">
                    {{ activeNote?.context || '等待搜索和摘要结果' }}
                  </p>
                  <div v-if="keywords.length > 0" class="flex flex-wrap gap-2">
                    <UiBadge v-for="keyword in keywords" :key="keyword" variant="outline">
                      {{ keyword }}
                    </UiBadge>
                  </div>
                  <UiSeparator v-if="visibleSearchResults.length > 0" />
                  <div v-for="result in visibleSearchResults" :key="result.sourceUrl || result.title || result.summary" class="space-y-1 rounded-md border bg-muted/20 p-3">
                    <a
                      v-if="result.sourceUrl"
                      :href="result.sourceUrl"
                      target="_blank"
                      rel="noopener noreferrer"
                      class="flex items-center gap-2 font-medium text-foreground transition-colors hover:text-primary"
                    >
                      <SearchIcon class="size-4 text-muted-foreground" />
                      <span class="line-clamp-1">{{ getSearchResultTitle(result) }}</span>
                      <ExternalLinkIcon class="ml-auto size-3.5 shrink-0 text-muted-foreground" />
                    </a>
                    <div v-else class="flex items-center gap-2 font-medium">
                      <SearchIcon class="size-4 text-muted-foreground" />
                      <span class="line-clamp-1">{{ getSearchResultTitle(result) }}</span>
                    </div>
                    <p class="text-xs leading-5 text-muted-foreground">
                      {{ result.summary }}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </section>

          <section v-else class="rounded-lg border bg-muted/20 p-8 text-center">
            <LoaderCircleIcon class="mx-auto size-8 animate-spin text-muted-foreground" />
            <h2 class="mt-4 text-lg font-semibold">
              正在自动生成
            </h2>
            <p class="mt-2 text-sm text-muted-foreground">
              当前阶段：{{ getPhaseDisplayLabel(activeNote) }}
            </p>
          </section>
        </div>
      </section>
    </main>
  </div>
</template>

<route lang="yaml">
meta:
  layout: user
  fullWidth: true
</route>
