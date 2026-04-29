<script setup lang="ts">
import {
  CrownIcon,
  LightbulbIcon,
  LoaderCircleIcon,
  RocketIcon,
  SparklesIcon,
} from '@lucide/vue'
import { toast } from 'vue-sonner'

import type { AppArticleProgress, ArticlePhase, ArticleTitleOption } from '@/services/types/app-article.type'

import { createAppArticleTask } from '@/services/api/app-article.api'
import { connectArticleSse } from '@/utils/article-sse'

type ArticleStyle = '默认' | '科技风格' | '情感风格' | '教育风格' | '轻松幽默'

const topic = ref('')
const taskId = ref('')
const titleOptions = ref<ArticleTitleOption[]>([])
const isCreating = ref(false)
const isConnected = ref(false)
const currentPhase = ref<ArticlePhase>('INPUT')
const selectedStyle = ref<ArticleStyle>('默认')
const selectedTitleIndex = ref<number | null>(null)
let closeSse: (() => void) | null = null

const maxTopicLength = 500

const steps = computed(() => [
  { title: '生成标题', desc: 'AI 分析选题，生成吸睛标题', active: ['INPUT', 'PENDING', 'TITLE_GENERATING', 'TITLE_SELECTING'].includes(currentPhase.value) },
  { title: '规划大纲', desc: '构建文章结构，理清脉络', active: ['OUTLINE_GENERATING', 'OUTLINE_EDITING'].includes(currentPhase.value) },
  { title: '撰写正文', desc: '流式生成高质量文章内容', active: currentPhase.value === 'CONTENT_GENERATING' },
  { title: '分析配图', desc: '当前版本暂不启用配图', active: false },
  { title: '生成配图', desc: '当前版本暂不启用配图', active: false },
  { title: '图文合成', desc: '正文完成后展示结果', active: currentPhase.value === 'COMPLETED' },
])

const hotTopics = [
  '2026年AI如何改变职场',
  '程序员如何提升竞争力',
  '远程办公的利与弊',
  '如何培养深度思考',
  '新能源汽车趋势',
  '健康饮食指南',
]

const writingTips = [
  { title: '抓住痛点', desc: '直击用户最关心的问题' },
  { title: '制造悬念', desc: '让读者产生好奇心' },
  { title: '数字吸引', desc: '使用具体数据增加说服力' },
]

const styleOptions: ArticleStyle[] = ['默认', '科技风格', '情感风格', '教育风格', '轻松幽默']
const imageOptions = ['Pexels', 'Nano Banana', 'Mermaid', 'Iconify', '表情包', 'SVG']

const topicLength = computed(() => topic.value.length)
const canCreate = computed(() => topic.value.trim().length > 0 && !isCreating.value)
const isInputPhase = computed(() => currentPhase.value === 'INPUT' && !taskId.value)
const isTitleGenerating = computed(() =>
  Boolean(taskId.value) && ['PENDING', 'TITLE_GENERATING'].includes(currentPhase.value),
)
const isTitleSelecting = computed(() =>
  currentPhase.value === 'TITLE_SELECTING' || titleOptions.value.length > 0,
)
const stepperValue = computed(() => {
  if (['OUTLINE_GENERATING', 'OUTLINE_EDITING'].includes(currentPhase.value)) {
    return 2
  }
  if (currentPhase.value === 'CONTENT_GENERATING') {
    return 3
  }
  if (currentPhase.value === 'COMPLETED') {
    return 6
  }
  return 1
})

/**
 * 后端存储的是 JSON 字符串，前端收到进度快照时统一解析成标题候选数组。
 */
function parseTitleOptions(value?: string) {
  if (!value) {
    return []
  }
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed as ArticleTitleOption[] : []
  }
  catch {
    return []
  }
}

/**
 * 使用进度快照恢复当前页面状态，支持刷新页面或重新连接 SSE。
 */
function applyProgress(progress: AppArticleProgress) {
  currentPhase.value = progress.phase ?? 'PENDING'
  taskId.value = progress.taskId
  topic.value = progress.topic
  const options = parseTitleOptions(progress.titleOptions)
  if (options.length > 0) {
    titleOptions.value = options
  }
  isCreating.value = ['PENDING', 'TITLE_GENERATING'].includes(progress.phase ?? 'PENDING')
}

/**
 * 首页带入选题时，直接填充到第一步输入框。
 */
const route = useRoute()
const initialTopic = route.query.topic
if (typeof initialTopic === 'string') {
  topic.value = initialTopic.slice(0, maxTopicLength)
}

/**
 * 关闭当前 SSE 连接，避免页面切换后仍接收旧任务消息。
 */
function closeCurrentSse() {
  closeSse?.()
  closeSse = null
  isConnected.value = false
}

/**
 * 使用热门选题填充输入框，降低第一步创作门槛。
 */
function useHotTopic(value: string) {
  topic.value = value
}

/**
 * 创建文章任务并等待后端通过 SSE 返回标题候选。
 */
async function handleCreateTask() {
  const normalizedTopic = topic.value.trim()
  if (!normalizedTopic) {
    toast.error('请输入文章选题')
    return
  }

  closeCurrentSse()
  titleOptions.value = []
  selectedTitleIndex.value = null
  currentPhase.value = 'TITLE_GENERATING'
  isCreating.value = true

  try {
    const response = await createAppArticleTask({ topic: normalizedTopic })
    taskId.value = response.data
    isConnected.value = true

    closeSse = connectArticleSse(response.data, {
      onMessage(message) {
        if (message.type === 'PROGRESS') {
          applyProgress(message.data as AppArticleProgress)
        }

        if (message.type === 'TITLES_GENERATED') {
          titleOptions.value = (message.data ?? []) as ArticleTitleOption[]
          currentPhase.value = 'TITLE_SELECTING'
          isCreating.value = false
          toast.success('标题方案已生成')
        }

        if (message.type === 'ERROR') {
          currentPhase.value = 'FAILED'
          isCreating.value = false
          isConnected.value = false
          toast.error(message.message || '文章生成失败')
        }
      },
      onError(error) {
        currentPhase.value = 'FAILED'
        isCreating.value = false
        isConnected.value = false
        const message = error instanceof Error ? error.message : 'SSE 连接异常'
        toast.error(message)
      },
    })
  }
  catch (error: any) {
    currentPhase.value = 'INPUT'
    isCreating.value = false
    const message = error?.data?.message ?? error?.message ?? '创建文章任务失败'
    toast.error(message)
  }
}

onUnmounted(() => {
  closeCurrentSse()
})
</script>

<template>
  <div class="min-h-[calc(100vh-72px)] bg-[#f8faf9] text-slate-950">
    <main class="grid min-h-[calc(100vh-72px)] grid-cols-1 lg:grid-cols-[280px_minmax(0,1fr)_320px]">
      <aside class="hidden border-r bg-background px-7 py-9 lg:block">
        <UiCard class="border-0 bg-transparent shadow-none">
          <UiCardHeader class="px-0">
            <UiCardTitle>创作流程</UiCardTitle>
            <UiCardDescription>智能体协作可视化</UiCardDescription>
          </UiCardHeader>

          <UiCardContent class="px-0">
            <UiStepper :model-value="stepperValue" orientation="vertical" class="flex-col items-start gap-0">
              <UiStepperItem
                v-for="(step, index) in steps"
                :key="step.title"
                :step="index + 1"
                class="relative w-full items-start gap-4 pb-8 last:pb-0"
                :data-state="step.active ? 'active' : 'inactive'"
              >
                <UiStepperSeparator
                  v-if="index < steps.length - 1"
                  class="absolute left-[19px] top-10 h-full"
                />
                <UiStepperTrigger class="flex-row items-start gap-4 p-0 text-left hover:bg-transparent">
                  <UiStepperIndicator
                    class="size-10 border-2 text-base data-[state=active]:border-emerald-500 data-[state=active]:text-emerald-600 data-[state=inactive]:text-muted-foreground"
                  >
                    {{ index + 1 }}
                  </UiStepperIndicator>
                  <div class="min-w-0 pt-1">
                    <UiStepperTitle :class="step.active ? 'text-emerald-600' : 'text-muted-foreground'">
                      {{ step.title }}
                    </UiStepperTitle>
                    <UiStepperDescription>{{ step.desc }}</UiStepperDescription>
                  </div>
                </UiStepperTrigger>
              </UiStepperItem>
            </UiStepper>
          </UiCardContent>
        </UiCard>
      </aside>

      <section class="flex justify-center px-4 py-10 sm:px-8 lg:py-24">
        <div class="w-full max-w-[780px]">
          <UiCard v-if="isInputPhase" class="rounded-2xl border-0 bg-muted/40 shadow-sm">
            <UiCardHeader class="items-center px-6 py-10 text-center sm:px-12">
              <UiBadge variant="secondary" class="mb-2 gap-2 bg-emerald-50 text-emerald-600">
                <SparklesIcon class="size-4" />
                第一步：生成标题
              </UiBadge>
              <UiCardTitle class="text-3xl font-bold tracking-tight sm:text-4xl">
                创作新文章
              </UiCardTitle>
              <UiCardDescription class="text-base">
                输入选题，AI 帮你生成爆款文章标题
              </UiCardDescription>
            </UiCardHeader>

            <UiCardContent class="space-y-6 px-6 pb-8 sm:px-12">
              <UiCard>
                <UiCardContent class="p-5">
                  <UiField>
                    <UiTextarea
                      v-model="topic"
                      :maxlength="maxTopicLength"
                      class="min-h-40 resize-none text-base leading-7"
                      placeholder="请输入您想创作的文章选题，例如：2026年AI如何改变职场"
                      :disabled="isCreating"
                    />
                    <UiFieldDescription class="text-right">
                      {{ topicLength }} / {{ maxTopicLength }}
                    </UiFieldDescription>
                  </UiField>
                </UiCardContent>
              </UiCard>

              <UiCard>
                <UiCardHeader>
                  <UiCardTitle class="text-base">
                    文章风格
                  </UiCardTitle>
                  <UiCardDescription>不选择使用默认风格</UiCardDescription>
                </UiCardHeader>
                <UiCardContent>
                  <UiRadioGroup v-model="selectedStyle" class="flex flex-wrap gap-3">
                    <UiField
                      v-for="item in styleOptions"
                      :key="item"
                      orientation="horizontal"
                      class="w-auto items-center rounded-md border px-4 py-2.5 has-[[data-state=checked]]:border-emerald-500 has-[[data-state=checked]]:bg-emerald-50"
                    >
                      <UiRadioGroupItem :id="`style-${item}`" :value="item" />
                      <UiFieldLabel :for="`style-${item}`" class="cursor-pointer font-medium">
                        {{ item }}
                      </UiFieldLabel>
                    </UiField>
                  </UiRadioGroup>
                </UiCardContent>
              </UiCard>

              <UiCard class="opacity-70">
                <UiCardHeader>
                  <UiCardTitle class="text-base">
                    配图方式
                  </UiCardTitle>
                  <UiCardDescription>当前版本暂不启用配图</UiCardDescription>
                </UiCardHeader>
                <UiCardContent>
                  <div class="flex flex-wrap gap-3">
                    <UiField
                      v-for="item in imageOptions"
                      :key="item"
                      orientation="horizontal"
                      class="w-auto items-center rounded-md border px-4 py-2.5"
                    >
                      <UiCheckbox :id="`image-${item}`" disabled />
                      <UiFieldLabel :for="`image-${item}`" class="font-medium text-muted-foreground">
                        {{ item }}
                      </UiFieldLabel>
                    </UiField>
                  </div>
                </UiCardContent>
              </UiCard>

              <UiButton
                class="h-14 w-full text-base font-semibold"
                :disabled="!canCreate"
                @click="handleCreateTask"
              >
                <LoaderCircleIcon v-if="isCreating" class="mr-2 size-5 animate-spin" />
                <RocketIcon v-else class="mr-2 size-5" />
                {{ isCreating ? '正在生成标题...' : '开始创作' }}
              </UiButton>

              <UiAlert v-if="taskId">
                <UiAlertTitle>任务已创建</UiAlertTitle>
                <UiAlertDescription>
                  任务 ID：{{ taskId }}
                  <UiBadge variant="outline" class="ml-2">
                    {{ isConnected ? 'SSE 已连接' : 'SSE 未连接' }}
                  </UiBadge>
                </UiAlertDescription>
              </UiAlert>
            </UiCardContent>
          </UiCard>

          <UiCard v-else-if="isTitleGenerating" class="rounded-2xl border-0 bg-muted/40 shadow-sm">
            <UiCardHeader class="items-center px-6 py-10 text-center sm:px-12">
              <UiBadge variant="secondary" class="mb-2 gap-2 bg-emerald-50 text-emerald-600">
                <LoaderCircleIcon class="size-4 animate-spin" />
                第一步：生成标题
              </UiBadge>
              <UiCardTitle class="text-3xl font-bold tracking-tight sm:text-4xl">
                正在生成标题
              </UiCardTitle>
              <UiCardDescription class="text-base">
                AI 正在分析选题，请稍候
              </UiCardDescription>
            </UiCardHeader>

            <UiCardContent class="space-y-6 px-6 pb-8 sm:px-12">
              <UiCard>
                <UiCardContent class="space-y-5 p-6">
                  <div class="flex items-start gap-4">
                    <UiSpinner class="mt-1 size-6 text-emerald-600" />
                    <div class="min-w-0">
                      <div class="font-semibold">
                        {{ topic }}
                      </div>
                      <p class="mt-2 text-sm leading-6 text-muted-foreground">
                        正在生成 3-5 个爆款标题方案，完成后会自动切换到标题候选。
                      </p>
                    </div>
                  </div>
                  <UiProgress :model-value="45" />
                </UiCardContent>
              </UiCard>

              <UiAlert v-if="taskId">
                <UiAlertTitle>任务进行中</UiAlertTitle>
                <UiAlertDescription>
                  任务 ID：{{ taskId }}
                  <UiBadge variant="outline" class="ml-2">
                    {{ isConnected ? 'SSE 已连接' : 'SSE 连接中' }}
                  </UiBadge>
                </UiAlertDescription>
              </UiAlert>
            </UiCardContent>
          </UiCard>

          <UiCard v-else-if="isTitleSelecting" class="rounded-2xl border-0 bg-muted/40 shadow-sm">
            <UiCardHeader class="flex flex-row items-center justify-between gap-3">
              <div>
                <UiCardTitle>
                  标题候选
                </UiCardTitle>
                <UiCardDescription>
                  选择一个标题后进入下一步大纲生成
                </UiCardDescription>
              </div>
              <UiBadge variant="outline">
                {{ titleOptions.length }} 个方案
              </UiBadge>
            </UiCardHeader>

            <UiCardContent class="grid gap-3">
              <UiCard
                v-for="(item, index) in titleOptions"
                :key="`${item.mainTitle}-${index}`"
                class="cursor-pointer transition-colors hover:border-emerald-400 hover:bg-emerald-50/60"
                :class="selectedTitleIndex === index ? 'border-emerald-500 bg-emerald-50' : ''"
                @click="selectedTitleIndex = index"
              >
                <UiCardHeader class="flex flex-row items-center justify-between gap-3 pb-2">
                  <UiCardTitle class="text-base">
                    {{ item.mainTitle }}
                  </UiCardTitle>
                  <UiBadge variant="outline">
                    方案 {{ index + 1 }}
                  </UiBadge>
                </UiCardHeader>
                <UiCardContent class="text-sm leading-6 text-muted-foreground">
                  {{ item.subTitle }}
                </UiCardContent>
              </UiCard>
            </UiCardContent>
          </UiCard>

          <UiCard v-else class="rounded-2xl border-0 bg-muted/40 shadow-sm">
            <UiCardHeader>
              <UiCardTitle>创作任务异常</UiCardTitle>
              <UiCardDescription>请返回输入阶段后重试。</UiCardDescription>
            </UiCardHeader>
            <UiCardContent>
              <UiButton @click="currentPhase = 'INPUT'; taskId = ''; titleOptions = []">
                重新开始
              </UiButton>
            </UiCardContent>
          </UiCard>
        </div>
      </section>

      <aside class="hidden border-l bg-background px-7 py-8 xl:block">
        <div class="space-y-8">
          <UiCard class="bg-emerald-50/60">
            <UiCardHeader>
              <UiCardTitle class="flex items-center gap-2 text-base">
                <CrownIcon class="size-5" />
                创作配额
              </UiCardTitle>
            </UiCardHeader>
            <UiCardContent class="flex items-center gap-4">
              <UiBadge class="rounded-full bg-slate-950 px-4 py-2 text-white">
                管理员
              </UiBadge>
              <span class="text-muted-foreground">无限次</span>
            </UiCardContent>
          </UiCard>

          <UiCard class="border-0 bg-transparent shadow-none">
            <UiCardHeader class="px-0">
              <UiCardTitle class="flex items-center gap-2 text-lg">
                <LightbulbIcon class="size-5" />
                热门选题
              </UiCardTitle>
            </UiCardHeader>
            <UiCardContent class="flex flex-wrap gap-3 px-0">
              <UiButton
                v-for="item in hotTopics"
                :key="item"
                type="button"
                variant="outline"
                size="sm"
                @click="useHotTopic(item)"
              >
                {{ item }}
              </UiButton>
            </UiCardContent>
          </UiCard>

          <UiSeparator />

          <UiCard class="border-0 bg-transparent shadow-none">
            <UiCardHeader class="px-0">
              <UiCardTitle class="flex items-center gap-2 text-lg">
                <SparklesIcon class="size-5" />
                爆款技巧
              </UiCardTitle>
            </UiCardHeader>
            <UiCardContent class="space-y-4 px-0">
              <UiCard
                v-for="(item, index) in writingTips"
                :key="item.title"
                class="bg-muted/40"
              >
                <UiCardContent class="flex gap-4 p-4">
                  <UiBadge class="grid size-8 shrink-0 place-items-center rounded-full p-0">
                    {{ index + 1 }}
                  </UiBadge>
                  <div>
                    <div class="font-semibold">
                      {{ item.title }}
                    </div>
                    <p class="mt-1 text-sm text-muted-foreground">
                      {{ item.desc }}
                    </p>
                  </div>
                </UiCardContent>
              </UiCard>
            </UiCardContent>
          </UiCard>
        </div>
      </aside>
    </main>
  </div>
</template>

<route lang="yaml">
meta:
  layout: user
  fullWidth: true
</route>
