<script setup lang="ts">
import { CheckCircle2Icon, CopyIcon, FileTextIcon, ImageIcon, LoaderCircleIcon, PlusIcon, SparklesIcon, Trash2Icon } from '@lucide/vue'
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRoute } from 'vue-router'

import { confirmArticleOutline, confirmArticleTitle, createArticleTask } from '@/services/api'
import { connectArticleSse } from '@/services/sse'
import type { ArticleImageResult, ArticleOutlineResult, ArticleOutlineSection, ArticlePhase, ArticleTitleOption, SseMessage } from '@/types'
import { parseJsonArray } from '@/utils/format'

const route = useRoute()
const topic = ref(typeof route.query.prompt === 'string' ? route.query.prompt : '')
const taskId = ref('')
const phase = ref<ArticlePhase>('INPUT')
const isBusy = ref(false)
const isConnected = ref(false)
const userDescription = ref('')
const selectedImageMethods = ref(['GPT_IMAGE'])
const selectedTitleIndex = ref<number | null>(null)
const titleOptions = ref<ArticleTitleOption[]>([])
const outlineSections = ref<ArticleOutlineSection[]>([])
const generatedContent = ref('')
const generatedImages = ref<ArticleImageResult[]>([])
const errorMessage = ref('')
let closeSse: (() => void) | null = null

const phaseSteps: Array<{ phase: ArticlePhase, title: string, desc: string }> = [
  { phase: 'TITLE_GENERATING', title: '标题灵感', desc: 'AI 生成多个表达方向' },
  { phase: 'OUTLINE_EDITING', title: '大纲确认', desc: '用户调整结构后继续' },
  { phase: 'CONTENT_GENERATING', title: '正文生成', desc: '按确认结构写作' },
  { phase: 'IMAGE_GENERATING', title: '配图生成', desc: '封面和章节图自动生成' },
  { phase: 'COMPLETED', title: '作品完成', desc: '复制正文或下载图片' },
]

const selectedTitle = computed(() => selectedTitleIndex.value === null ? null : titleOptions.value[selectedTitleIndex.value] ?? null)
const currentStepIndex = computed(() => {
  const index = phaseSteps.findIndex(step => step.phase === phase.value)
  if (phase.value === 'PENDING' || phase.value === 'TITLE_SELECTING') {
    return 0
  }
  if (phase.value === 'OUTLINE_GENERATING') {
    return 1
  }
  if (phase.value === 'IMAGE_ANALYZING' || phase.value === 'CONTENT_MERGING') {
    return 3
  }
  return Math.max(index, 0)
})
const canStart = computed(() => topic.value.trim().length > 0 && !isBusy.value)
const coverImage = computed(() => generatedImages.value.find(image => image.position === 1)?.url || '')

/**
 * 关闭当前 SSE 连接，页面切走后不继续消费旧任务事件。
 */
function stopSse() {
  closeSse?.()
  closeSse = null
  isConnected.value = false
}

/**
 * 订阅后端文章任务事件，所有阶段都由同一条连接推进。
 */
function startSse(nextTaskId: string) {
  stopSse()
  taskId.value = nextTaskId
  isConnected.value = true
  closeSse = connectArticleSse(nextTaskId, {
    onMessage: applySseMessage,
    onError: () => {
      isConnected.value = false
    },
  })
}

/**
 * 解析后端事件并投影到创作页状态。
 */
function applySseMessage(message: SseMessage) {
  if (message.type === 'PHASE_CHANGED') {
    phase.value = message.data as ArticlePhase
  }
  if (message.type === 'TITLES_GENERATED') {
    titleOptions.value = message.data as ArticleTitleOption[]
    phase.value = 'TITLE_SELECTING'
    isBusy.value = false
  }
  if (message.type === 'OUTLINE_GENERATED') {
    applyOutline(message.data as ArticleOutlineResult)
    phase.value = 'OUTLINE_EDITING'
    isBusy.value = false
  }
  if (message.type === 'IMAGE_GENERATED') {
    generatedImages.value = message.data as ArticleImageResult[]
    phase.value = 'CONTENT_MERGING'
  }
  if (message.type === 'ALL_COMPLETE') {
    generatedContent.value = String(message.data ?? generatedContent.value)
    phase.value = 'COMPLETED'
    isBusy.value = false
    stopSse()
  }
  if (message.type === 'PROGRESS' && message.data && typeof message.data === 'object') {
    const snapshot = message.data as { phase?: ArticlePhase, titleOptions?: string, outline?: string, images?: string, fullContent?: string, content?: string }
    phase.value = snapshot.phase ?? phase.value
    titleOptions.value = parseJsonArray<ArticleTitleOption>(snapshot.titleOptions)
    const outline = parseOutline(snapshot.outline)
    if (outline) {
      applyOutline(outline)
    }
    generatedImages.value = parseJsonArray<ArticleImageResult>(snapshot.images)
    generatedContent.value = snapshot.fullContent || snapshot.content || generatedContent.value
  }
  if (message.type === 'ERROR' || message.type === 'WORKFLOW_EXPIRED') {
    errorMessage.value = String(message.message || message.data || '创作任务异常')
    phase.value = message.type === 'WORKFLOW_EXPIRED' ? 'EXPIRED' : 'FAILED'
    isBusy.value = false
    stopSse()
  }
}

/**
 * 将后端大纲 JSON 字符串转换为页面可编辑结构。
 */
function parseOutline(value?: string) {
  if (!value) {
    return null
  }
  try {
    return JSON.parse(value) as ArticleOutlineResult
  }
  catch {
    return null
  }
}

/**
 * 应用大纲时复制数组，避免直接修改后端原始对象。
 */
function applyOutline(outline: ArticleOutlineResult) {
  outlineSections.value = outline.sections.map((section, index) => ({
    section: index + 1,
    title: section.title,
    points: section.points.length > 0 ? [...section.points] : [''],
  }))
}

/**
 * 创建文章任务并立即进入标题生成阶段。
 */
async function startArticle() {
  if (!canStart.value) {
    return
  }
  isBusy.value = true
  errorMessage.value = ''
  phase.value = 'PENDING'
  try {
    const nextTaskId = await createArticleTask(topic.value.trim(), selectedImageMethods.value)
    startSse(nextTaskId)
  }
  catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '创建文章任务失败'
    phase.value = 'FAILED'
    isBusy.value = false
  }
}

/**
 * 用户确认标题后，后端继续生成大纲。
 */
async function submitTitle() {
  if (!taskId.value || !selectedTitle.value) {
    return
  }
  isBusy.value = true
  await confirmArticleTitle(taskId.value, selectedTitle.value.mainTitle, selectedTitle.value.subTitle, userDescription.value.trim())
  phase.value = 'OUTLINE_GENERATING'
}

/**
 * 新增大纲章节，用户可在 AI 初稿上继续细化。
 */
function addSection() {
  outlineSections.value.push({ section: outlineSections.value.length + 1, title: '新章节', points: [''] })
}

/**
 * 删除章节后重排序，保证提交给后端的 section 连续。
 */
function removeSection(index: number) {
  outlineSections.value.splice(index, 1)
  outlineSections.value = outlineSections.value.map((section, nextIndex) => ({ ...section, section: nextIndex + 1 }))
}

/**
 * 确认大纲后进入正文和配图生成。
 */
async function submitOutline() {
  if (!taskId.value || outlineSections.value.length === 0) {
    return
  }
  isBusy.value = true
  const outline: ArticleOutlineResult = {
    sections: outlineSections.value.map((section, index) => ({
      section: index + 1,
      title: section.title.trim(),
      points: section.points.map(point => point.trim()).filter(Boolean),
    })),
  }
  await confirmArticleOutline(taskId.value, outline)
  phase.value = 'CONTENT_GENERATING'
}

/**
 * 复制最终正文，方便发布到公众号或文档编辑器。
 */
async function copyContent() {
  await navigator.clipboard.writeText(generatedContent.value)
}

onBeforeUnmount(stopSse)
</script>

<template>
  <div class="space-y-8">
    <section class="flex flex-col gap-5 md:flex-row md:items-end md:justify-between">
      <div>
        <div class="mb-3 inline-flex items-center gap-2 rounded-full border border-blue-100 bg-blue-50 px-3 py-1.5 text-sm text-blue-700">
          <SparklesIcon class="size-4" />
          AI 文章创作
        </div>
        <h1 class="text-4xl font-semibold tracking-[-0.06em] text-slate-950 sm:text-5xl">从一句选题到完整图文</h1>
        <p class="mt-3 max-w-2xl text-sm leading-7 text-slate-500">标题和大纲都需要你确认，AI 不会替你做关键判断。</p>
      </div>
      <span class="rounded-full border px-4 py-2 text-sm" :class="isConnected ? 'border-emerald-100 bg-emerald-50 text-emerald-700' : 'border-slate-200 bg-white/70 text-slate-500'">
        {{ isConnected ? '实时连接中' : '未连接' }}
      </span>
    </section>

    <section class="grid gap-4 md:grid-cols-5">
      <div v-for="(step, index) in phaseSteps" :key="step.title" class="subtle-card rounded-3xl p-4" :class="index === currentStepIndex ? 'ring-2 ring-blue-200' : ''">
        <div class="mb-4 flex items-center justify-between">
          <span class="grid size-8 place-items-center rounded-full text-xs font-semibold" :class="index <= currentStepIndex ? 'ai-gradient text-white' : 'bg-slate-100 text-slate-400'">{{ index + 1 }}</span>
          <LoaderCircleIcon v-if="index === currentStepIndex && isBusy" class="size-4 animate-spin text-blue-500" />
          <CheckCircle2Icon v-else-if="index < currentStepIndex || phase === 'COMPLETED'" class="size-4 text-emerald-500" />
        </div>
        <strong class="text-sm">{{ step.title }}</strong>
        <p class="mt-2 text-xs leading-5 text-slate-500">{{ step.desc }}</p>
      </div>
    </section>

    <section class="grid gap-6 lg:grid-cols-[380px_minmax(0,1fr)]">
      <aside class="glass-panel rounded-[2rem] p-6">
        <label class="text-sm font-semibold text-slate-700">创作主题</label>
        <textarea v-model="topic" class="mt-3 min-h-36 w-full resize-none rounded-3xl border border-slate-200 bg-white/80 p-4 text-sm leading-7 outline-none focus:border-blue-300" placeholder="输入你想创作的文章主题" />
        <label class="mt-5 block text-sm font-semibold text-slate-700">补充要求</label>
        <textarea v-model="userDescription" class="mt-3 min-h-24 w-full resize-none rounded-3xl border border-slate-200 bg-white/80 p-4 text-sm leading-7 outline-none focus:border-blue-300" placeholder="风格、读者、字数、必须包含的观点..." />
        <div class="mt-5 grid gap-2">
          <button type="button" class="ai-gradient inline-flex min-h-12 items-center justify-center gap-2 rounded-2xl font-semibold text-white" :disabled="!canStart" @click="startArticle">
            <LoaderCircleIcon v-if="isBusy && phase === 'PENDING'" class="size-4 animate-spin" />
            <SparklesIcon v-else class="size-4" />
            生成标题方案
          </button>
        </div>
        <p v-if="errorMessage" class="mt-4 rounded-2xl border border-rose-100 bg-rose-50 p-3 text-sm text-rose-700">{{ errorMessage }}</p>
      </aside>

      <div class="space-y-6">
        <section v-if="phase === 'INPUT' || phase === 'PENDING' || phase === 'TITLE_GENERATING'" class="glass-panel grid min-h-[28rem] place-items-center rounded-[2rem] p-8 text-center">
          <div>
            <LoaderCircleIcon v-if="phase !== 'INPUT'" class="mx-auto size-10 animate-spin text-blue-500" />
            <FileTextIcon v-else class="mx-auto size-10 text-slate-400" />
            <h2 class="mt-4 text-2xl font-semibold tracking-[-0.04em]">{{ phase === 'INPUT' ? '等待你的第一句话' : 'AI 正在拆解选题' }}</h2>
            <p class="mt-2 text-sm leading-6 text-slate-500">生成完成后会展示多个标题方向，你需要选择一个继续。</p>
          </div>
        </section>

        <section v-else-if="phase === 'TITLE_SELECTING'" class="glass-panel rounded-[2rem] p-6">
          <h2 class="text-2xl font-semibold tracking-[-0.04em]">选择标题方向</h2>
          <p class="mt-2 text-sm text-slate-500">这是第一个人机协作节点，别默认相信第一个方案。</p>
          <div class="mt-5 grid gap-3">
            <button v-for="(item, index) in titleOptions" :key="item.mainTitle" type="button" class="rounded-3xl border p-5 text-left transition" :class="selectedTitleIndex === index ? 'border-blue-300 bg-blue-50/80' : 'border-slate-200 bg-white/70 hover:bg-white'" @click="selectedTitleIndex = index">
              <strong class="block text-lg tracking-[-0.04em]">{{ item.mainTitle }}</strong>
              <span class="mt-2 block text-sm leading-6 text-slate-500">{{ item.subTitle }}</span>
            </button>
          </div>
          <button type="button" class="mt-5 rounded-2xl bg-slate-950 px-5 py-3 font-semibold text-white" :disabled="!selectedTitle || isBusy" @click="submitTitle">确认标题，生成大纲</button>
        </section>

        <section v-else-if="phase === 'OUTLINE_GENERATING'" class="glass-panel grid min-h-[26rem] place-items-center rounded-[2rem] p-8 text-center">
          <div>
            <LoaderCircleIcon class="mx-auto size-10 animate-spin text-blue-500" />
            <h2 class="mt-4 text-2xl font-semibold tracking-[-0.04em]">正在规划大纲</h2>
            <p class="mt-2 text-sm text-slate-500">完成后你可以直接修改章节和要点。</p>
          </div>
        </section>

        <section v-else-if="phase === 'OUTLINE_EDITING'" class="glass-panel rounded-[2rem] p-6">
          <div class="flex items-center justify-between gap-3">
            <div>
              <h2 class="text-2xl font-semibold tracking-[-0.04em]">确认大纲</h2>
              <p class="mt-2 text-sm text-slate-500">第二个人机协作节点。结构不对，后面正文只会更偏。</p>
            </div>
            <button type="button" class="rounded-2xl border border-slate-200 bg-white px-4 py-2 text-sm" @click="addSection">
              <PlusIcon class="mr-1 inline size-4" />
              加章节
            </button>
          </div>
          <div class="mt-5 space-y-4">
            <article v-for="(section, index) in outlineSections" :key="`${section.section}-${index}`" class="rounded-3xl border border-slate-200 bg-white/75 p-5">
              <div class="mb-3 flex items-center gap-3">
                <span class="grid size-8 place-items-center rounded-full bg-slate-950 text-xs font-semibold text-white">{{ index + 1 }}</span>
                <input v-model="section.title" class="min-w-0 flex-1 border-0 bg-transparent text-lg font-semibold outline-none" />
                <button type="button" class="text-slate-400 hover:text-rose-500" :disabled="outlineSections.length <= 1" @click="removeSection(index)">
                  <Trash2Icon class="size-4" />
                </button>
              </div>
              <div class="space-y-2">
                <input v-for="(_, pointIndex) in section.points" :key="pointIndex" v-model="section.points[pointIndex]" class="w-full rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3 text-sm outline-none focus:border-blue-200" placeholder="章节要点" />
              </div>
              <button type="button" class="mt-3 text-sm text-blue-600" @click="section.points.push('')">添加要点</button>
            </article>
          </div>
          <button type="button" class="mt-5 rounded-2xl bg-slate-950 px-5 py-3 font-semibold text-white" :disabled="isBusy" @click="submitOutline">确认大纲，生成正文</button>
        </section>

        <section v-else-if="['CONTENT_GENERATING', 'IMAGE_ANALYZING', 'IMAGE_GENERATING', 'CONTENT_MERGING'].includes(phase)" class="glass-panel grid min-h-[26rem] place-items-center rounded-[2rem] p-8 text-center">
          <div>
            <LoaderCircleIcon class="mx-auto size-10 animate-spin text-blue-500" />
            <h2 class="mt-4 text-2xl font-semibold tracking-[-0.04em]">AI 正在执行创作流程</h2>
            <p class="mt-2 text-sm text-slate-500">正文、图片分析和图文融合会自动推进。</p>
          </div>
        </section>

        <section v-else-if="phase === 'COMPLETED'" class="grid gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
          <article class="glass-panel rounded-[2rem] p-6">
            <div class="mb-5 flex items-center justify-between gap-3">
              <h2 class="text-2xl font-semibold tracking-[-0.04em]">生成结果</h2>
              <button type="button" class="rounded-2xl border border-slate-200 bg-white px-4 py-2 text-sm" @click="copyContent">
                <CopyIcon class="mr-1 inline size-4" />
                复制正文
              </button>
            </div>
            <div class="prose max-w-none whitespace-pre-wrap rounded-3xl border border-slate-200 bg-white p-6 text-sm leading-8 text-slate-700">{{ generatedContent }}</div>
          </article>
          <aside class="glass-panel rounded-[2rem] p-6">
            <h3 class="mb-4 flex items-center gap-2 font-semibold">
              <ImageIcon class="size-5" />
              配图资产
            </h3>
            <img v-if="coverImage" :src="coverImage" alt="文章封面图" class="aspect-video w-full rounded-3xl object-cover">
            <div v-else class="grid aspect-video place-items-center rounded-3xl border border-dashed text-sm text-slate-400">暂无封面</div>
            <div class="mt-4 space-y-3">
              <div v-for="image in generatedImages" :key="image.url || image.description" class="rounded-2xl bg-white/70 p-3 text-sm text-slate-600">{{ image.description || image.sectionTitle || image.method || '配图' }}</div>
            </div>
          </aside>
        </section>
      </div>
    </section>
  </div>
</template>
