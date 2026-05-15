<script setup lang="ts">
import {
  CheckCircle2Icon,
  ClipboardCheckIcon,
  ClipboardIcon,
  DownloadIcon,
  FileTextIcon,
  LightbulbIcon,
  LoaderCircleIcon,
  PenLineIcon,
  PlusIcon,
  RocketIcon,
  SparklesIcon,
  Trash2Icon,
  WalletCardsIcon,
} from '@lucide/vue'
import { useClipboard } from '@vueuse/core'
import { toast } from 'vue-sonner'

import type {
  AppArticleProgress,
  ArticleImageResult,
  ArticleOutlineResult,
  ArticleOutlineSection,
  ArticlePhase,
  ArticleSseMessage,
  ArticleTitleOption,
} from '@/services/types/app-article.type'
import type { PromptFeedbackStage } from '@/services/types/prompt-template.type'

import MarkdownContentRenderer from '@/components/article/markdown-content-renderer.vue'
import {
  confirmAppArticleOutline,
  confirmAppArticleTitle,
  createAppArticleTask,
  downloadAppArticleImage,
} from '@/services/api/app-article.api'
import { useGetCreditSummaryQuery } from '@/services/api/credit.api'
import { connectArticleSse } from '@/utils/article-sse'

import PromptFeedbackCard from './components/prompt-feedback-card.vue'

type ArticleStyle = '默认' | '科技风格' | '情感风格' | '教育风格' | '轻松幽默'
type ImageMethod = 'GPT_IMAGE' | 'PEXELS' | 'MERMAID' | 'ICONIFY' | 'SVG_DIAGRAM'

const topic = ref('')
const taskId = ref('')
const titleOptions = ref<ArticleTitleOption[]>([])
const isCreating = ref(false)
const isConnected = ref(false)
const currentPhase = ref<ArticlePhase>('INPUT')
const selectedStyle = ref<ArticleStyle>('默认')
const selectedImageMethods = ref<ImageMethod[]>(['GPT_IMAGE'])
const selectedTitleIndex = ref<number | null>(null)
const userDescription = ref('')
const outlineSections = ref<ArticleOutlineSection[]>([])
const generatedImages = ref<ArticleImageResult[]>([])
const coverImage = ref('')
const generatedContent = ref('')
const isConfirmingTitle = ref(false)
const isConfirmingOutline = ref(false)
let closeSse: (() => void) | null = null
const route = useRoute()
const router = useRouter()

const { data: creditSummaryData, isFetching: isFetchingCreditSummary } = useGetCreditSummaryQuery()

const maxTopicLength = 500
const activeArticleTaskStorageKey = 'sakura_article_creator_active_task'
const activeArticleTaskTtlMs = 7 * 24 * 60 * 60 * 1000

interface ActiveArticleTaskCache {
  taskId: string
  savedAt: number
}

interface ActivePromptFeedback {
  stage: PromptFeedbackStage
  title: string
  description: string
}

const steps = computed(() => [
  { title: '生成标题', desc: 'AI 分析选题，生成吸睛标题', active: ['INPUT', 'PENDING', 'TITLE_GENERATING', 'TITLE_SELECTING'].includes(currentPhase.value) },
  { title: '规划大纲', desc: '构建文章结构，理清脉络', active: ['OUTLINE_GENERATING', 'OUTLINE_EDITING'].includes(currentPhase.value) },
  { title: '撰写正文', desc: '流式生成高质量文章内容', active: currentPhase.value === 'CONTENT_GENERATING' },
  { title: '分析配图', desc: '分析封面和章节配图需求', active: currentPhase.value === 'IMAGE_ANALYZING' },
  { title: '生成配图', desc: '多策略生成并保存到 OSS', active: currentPhase.value === 'IMAGE_GENERATING' },
  { title: '图文合成', desc: '将图片插入正文并保存结果', active: ['CONTENT_MERGING', 'COMPLETED'].includes(currentPhase.value) },
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
const imageOptions: Array<{ label: string, value: ImageMethod }> = [
  { label: 'GPT Image 2', value: 'GPT_IMAGE' },
  { label: 'Pexels', value: 'PEXELS' },
  { label: 'Mermaid', value: 'MERMAID' },
  { label: 'Iconify', value: 'ICONIFY' },
  { label: 'SVG 概念图', value: 'SVG_DIAGRAM' },
]

const topicLength = computed(() => topic.value.length)
const canCreate = computed(() =>
  topic.value.trim().length > 0
  && selectedImageMethods.value.length > 0
  && !isCreating.value,
)
const isInputPhase = computed(() => currentPhase.value === 'INPUT' && !taskId.value)
const isTitleGenerating = computed(() =>
  Boolean(taskId.value) && ['PENDING', 'TITLE_GENERATING'].includes(currentPhase.value),
)
const isTitleSelecting = computed(() =>
  currentPhase.value === 'TITLE_SELECTING' && titleOptions.value.length > 0,
)
const isOutlineGenerating = computed(() => currentPhase.value === 'OUTLINE_GENERATING')
const isOutlineEditing = computed(() => currentPhase.value === 'OUTLINE_EDITING')
const isContentGenerating = computed(() =>
  ['CONTENT_GENERATING', 'IMAGE_ANALYZING', 'IMAGE_GENERATING', 'CONTENT_MERGING'].includes(currentPhase.value),
)
const generationTitle = computed(() => {
  if (currentPhase.value === 'IMAGE_ANALYZING') {
    return '配图分析中'
  }
  if (currentPhase.value === 'IMAGE_GENERATING') {
    return '配图生成中'
  }
  if (currentPhase.value === 'CONTENT_MERGING') {
    return '图文合成中'
  }
  return '正文生成中'
})
const isCompleted = computed(() => currentPhase.value === 'COMPLETED')
const isExpired = computed(() => currentPhase.value === 'EXPIRED')
const activePromptFeedback = computed<ActivePromptFeedback | null>(() => {
  // 反馈提示是页面级浮层，只在三个可评价环节出现。
  if (!taskId.value) {
    return null
  }
  if (isTitleSelecting.value) {
    return {
      stage: 'TITLE_SELECTION',
      title: '这轮标题效果如何？',
      description: '反馈只用于改进 Prompt，不影响继续创作。',
    }
  }
  if (isOutlineEditing.value) {
    return {
      stage: 'OUTLINE_EDITING',
      title: '这轮大纲效果如何？',
      description: '反馈可以跳过，确认大纲不受影响。',
    }
  }
  if (isCompleted.value) {
    return {
      stage: 'CONTENT_MERGED',
      title: '正文融合效果如何？',
      description: '这条反馈会关联本次正文 Prompt 版本。',
    }
  }
  return null
})
const selectedTitle = computed(() => {
  if (selectedTitleIndex.value === null) {
    return null
  }
  return titleOptions.value[selectedTitleIndex.value] ?? null
})
const outlineSectionCount = computed(() => outlineSections.value.length)
const outlinePointCount = computed(() =>
  outlineSections.value.reduce((total, section) => total + section.points.length, 0),
)
const downloadableImages = computed(() => {
  const imageMap = new Map<string, ArticleImageResult>()
  for (const image of generatedImages.value) {
    if (image.url) {
      imageMap.set(image.url, image)
    }
  }
  if (coverImage.value && !imageMap.has(coverImage.value)) {
    imageMap.set(coverImage.value, {
      position: 1,
      url: coverImage.value,
      description: '文章封面图',
    })
  }
  return Array.from(imageMap.values()).sort((left, right) => (left.position ?? 999) - (right.position ?? 999))
})
const coverImageAlt = computed(() =>
  generatedImages.value.find(image => image.position === 1)?.description || '文章封面图',
)
const generatedContentLength = computed(() => generatedContent.value.length)
const creditSummary = computed(() => creditSummaryData.value?.data)
const {
  copy: copyGeneratedContent,
  copied: isGeneratedContentCopied,
  isSupported: isClipboardSupported,
} = useClipboard({ source: generatedContent })
const stepperValue = computed(() => {
  if (['OUTLINE_GENERATING', 'OUTLINE_EDITING'].includes(currentPhase.value)) {
    return 2
  }
  if (currentPhase.value === 'CONTENT_GENERATING') {
    return 3
  }
  if (currentPhase.value === 'IMAGE_ANALYZING') {
    return 4
  }
  if (currentPhase.value === 'IMAGE_GENERATING') {
    return 5
  }
  if (['CONTENT_MERGING', 'COMPLETED'].includes(currentPhase.value)) {
    return 6
  }
  return 1
})

/**
 * 解析后端快照中的大纲 JSON 字符串。
 */
function parseOutline(value?: string): ArticleOutlineResult | null {
  if (!value) {
    return null
  }
  try {
    const parsed = JSON.parse(value) as ArticleOutlineResult
    return isValidOutline(parsed) ? parsed : null
  }
  catch {
    return null
  }
}

/**
 * 校验大纲结构，避免把错误 JSON 提交给后端正文生成阶段。
 */
function isValidOutline(value: unknown): value is ArticleOutlineResult {
  const outline = value as ArticleOutlineResult
  return Array.isArray(outline?.sections)
    && outline.sections.length > 0
    && outline.sections.every((section: ArticleOutlineSection) =>
      Number.isFinite(section.section)
      && typeof section.title === 'string'
      && section.title.trim().length > 0
      && Array.isArray(section.points)
      && section.points.every(point => typeof point === 'string'),
    )
}

/**
 * 根据当前数组下标重排章节序号，避免用户增删后提交跳号。
 */
function normalizeOutlineSectionOrder() {
  outlineSections.value = outlineSections.value.map((section, index) => ({
    ...section,
    section: index + 1,
  }))
}

/**
 * 将后端大纲结果转换为结构化编辑状态，用户看到的是章节卡片而不是 JSON。
 */
function applyOutlineResult(outline: ArticleOutlineResult) {
  outlineSections.value = outline.sections.map((section, index) => ({
    section: index + 1,
    title: section.title,
    points: section.points.length > 0 ? [...section.points] : [''],
  }))
}

/**
 * 新增一个空章节，默认给一个要点输入框，降低用户编辑成本。
 */
function addOutlineSection() {
  outlineSections.value.push({
    section: outlineSections.value.length + 1,
    title: '新章节',
    points: [''],
  })
}

/**
 * 删除章节后立即重排序号，保证后端收到的 section 连续。
 */
function removeOutlineSection(sectionIndex: number) {
  if (outlineSections.value.length <= 1) {
    toast.error('至少保留一个章节')
    return
  }
  outlineSections.value.splice(sectionIndex, 1)
  normalizeOutlineSectionOrder()
}

/**
 * 在指定章节下追加一个新要点。
 */
function addOutlinePoint(sectionIndex: number) {
  outlineSections.value[sectionIndex]?.points.push('')
}

/**
 * 删除指定要点，每个章节至少保留一个要点输入框。
 */
function removeOutlinePoint(sectionIndex: number, pointIndex: number) {
  const section = outlineSections.value[sectionIndex]
  if (!section) {
    return
  }
  if (section.points.length <= 1) {
    toast.error('每个章节至少保留一个要点')
    return
  }
  section.points.splice(pointIndex, 1)
}

/**
 * 将用户编辑界面转换为后端需要的大纲 JSON 结构。
 */
function buildOutlineForSubmit(): ArticleOutlineResult | null {
  const sections = outlineSections.value.map((section, index) => ({
    section: index + 1,
    title: section.title.trim(),
    points: section.points.map(point => point.trim()).filter(Boolean),
  }))

  const outline: ArticleOutlineResult = { sections }
  if (!isValidOutline(outline)) {
    return null
  }
  return outline
}

/**
 * 组装用户补充要求，把风格选择传给后端大纲生成 Agent。
 */
function buildUserDescription() {
  const parts: string[] = []
  if (selectedStyle.value !== '默认') {
    parts.push(`文章风格：${selectedStyle.value}`)
  }
  if (userDescription.value.trim()) {
    parts.push(`补充要求：${userDescription.value.trim()}`)
  }
  return parts.join('\n')
}

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
 * 解析后端保存的配图结果，封面图和章节图共用这一份 JSON 数组。
 */
function parseImageResults(value?: string) {
  if (!value) {
    return []
  }
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed as ArticleImageResult[] : []
  }
  catch {
    return []
  }
}

/**
 * 从配图结果中同步封面，position=1 是后端约定的封面图。
 */
function applyImageResults(images: ArticleImageResult[]) {
  generatedImages.value = images
  coverImage.value = images.find(image => image.position === 1)?.url ?? coverImage.value
}

/**
 * 使用进度快照恢复当前页面状态，支持刷新页面或重新连接 SSE。
 */
function applyProgress(progress: AppArticleProgress) {
  currentPhase.value = progress.phase ?? 'PENDING'
  taskId.value = progress.taskId
  topic.value = progress.topic
  generatedContent.value = progress.fullContent ?? progress.content ?? generatedContent.value
  coverImage.value = progress.coverImage ?? coverImage.value
  const options = parseTitleOptions(progress.titleOptions)
  if (options.length > 0) {
    titleOptions.value = options
  }
  const outline = parseOutline(progress.outline)
  if (outline) {
    applyOutlineResult(outline)
  }
  const images = parseImageResults(progress.images)
  if (images.length > 0) {
    applyImageResults(images)
  }
  isCreating.value = ['PENDING', 'TITLE_GENERATING'].includes(progress.phase ?? 'PENDING')
}

/**
 * 读取上次未完成创作任务，7 天外的任务交给后端过期策略处理，前端直接回到新建页。
 */
function readActiveArticleTaskId() {
  if (typeof window === 'undefined') {
    return ''
  }

  try {
    const rawCache = window.localStorage.getItem(activeArticleTaskStorageKey)
    if (!rawCache) {
      return ''
    }

    const cache = JSON.parse(rawCache) as ActiveArticleTaskCache
    const isValidTaskId = typeof cache.taskId === 'string' && cache.taskId.trim().length > 0
    const isExpired = Date.now() - Number(cache.savedAt ?? 0) > activeArticleTaskTtlMs
    if (!isValidTaskId || isExpired) {
      window.localStorage.removeItem(activeArticleTaskStorageKey)
      return ''
    }
    return cache.taskId
  }
  catch {
    window.localStorage.removeItem(activeArticleTaskStorageKey)
    return ''
  }
}

/**
 * 记录当前未完成任务，用户刷新或重新进入创作页时可以重新连接 SSE。
 */
function rememberActiveArticleTask(nextTaskId: string) {
  if (typeof window === 'undefined' || !nextTaskId) {
    return
  }

  window.localStorage.setItem(activeArticleTaskStorageKey, JSON.stringify({
    taskId: nextTaskId,
    savedAt: Date.now(),
  } satisfies ActiveArticleTaskCache))
}

/**
 * 任务完成、失败或用户重新开始时清除恢复入口，避免旧任务反复抢占新建页面。
 */
function forgetActiveArticleTask() {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.removeItem(activeArticleTaskStorageKey)
}

/**
 * 将 taskId 同步到 URL，浏览器刷新时不依赖内存状态也能恢复到人工确认节点。
 */
function syncTaskIdToRouteQuery(nextTaskId: string) {
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
 * 清理 URL 中的 taskId，确保后续打开创作页默认展示重新生成界面。
 */
function clearTaskIdRouteQuery() {
  if (!route.query.taskId) {
    return
  }

  const nextQuery = { ...route.query }
  delete nextQuery.taskId
  void router.replace({
    path: route.path,
    query: nextQuery,
  })
}

/**
 * 首页带入选题时，直接填充到第一步输入框。
 */
const initialTopic = route.query.topic
if (typeof initialTopic === 'string') {
  topic.value = initialTopic.slice(0, maxTopicLength)
}
const initialTaskId = typeof route.query.taskId === 'string' ? route.query.taskId.trim() : ''

/**
 * 关闭当前 SSE 连接，避免页面切换后仍接收旧任务消息。
 */
function closeCurrentSse() {
  closeSse?.()
  closeSse = null
  isConnected.value = false
}

/**
 * 清空当前任务状态，用于重新开始或创建新任务前复位。
 */
function resetCreatorState() {
  closeCurrentSse()
  forgetActiveArticleTask()
  clearTaskIdRouteQuery()
  taskId.value = ''
  titleOptions.value = []
  selectedTitleIndex.value = null
  outlineSections.value = []
  generatedImages.value = []
  coverImage.value = ''
  generatedContent.value = ''
  isCreating.value = false
  isConfirmingTitle.value = false
  isConfirmingOutline.value = false
  selectedImageMethods.value = ['GPT_IMAGE']
  currentPhase.value = 'INPUT'
}

/**
 * 切换用户允许的配图方式，至少保留一种，避免后端无法规划配图。
 */
function toggleImageMethod(method: ImageMethod, checked: boolean | 'indeterminate') {
  if (checked === true) {
    if (!selectedImageMethods.value.includes(method)) {
      selectedImageMethods.value.push(method)
    }
    return
  }
  if (selectedImageMethods.value.length <= 1) {
    toast.error('至少保留一种配图方式')
    return
  }
  selectedImageMethods.value = selectedImageMethods.value.filter(item => item !== method)
}

/**
 * 判断当前配图方式是否是唯一已选方式，唯一项不允许取消。
 */
function isImageMethodLocked(method: ImageMethod) {
  return selectedImageMethods.value.length <= 1 && selectedImageMethods.value.includes(method)
}

/**
 * 点击整个选项块切换配图方式，避免 checkbox 文本标签无法触发状态更新。
 */
function handleImageMethodOptionClick(method: ImageMethod) {
  if (isImageMethodLocked(method)) {
    toast.error('至少保留一种配图方式')
    return
  }
  toggleImageMethod(method, !selectedImageMethods.value.includes(method))
}

/**
 * 统一处理文章任务 SSE 消息，创建任务和 taskId 恢复共用同一套状态推进逻辑。
 */
function handleArticleSseMessage(message: ArticleSseMessage) {
  if (message.type === 'PROGRESS') {
    applyProgress(message.data as AppArticleProgress)
  }

  if (message.type === 'PHASE_CHANGED') {
    currentPhase.value = message.data as ArticlePhase
  }

  if (message.type === 'TITLES_GENERATED') {
    titleOptions.value = (message.data ?? []) as ArticleTitleOption[]
    currentPhase.value = 'TITLE_SELECTING'
    isCreating.value = false
    toast.success('标题方案已生成')
  }

  if (message.type === 'OUTLINE_GENERATED') {
    applyOutlineResult(message.data as ArticleOutlineResult)
    currentPhase.value = 'OUTLINE_EDITING'
    isConfirmingTitle.value = false
    toast.success('大纲已生成')
  }

  if (message.type === 'ALL_COMPLETE') {
    generatedContent.value = String(message.data ?? '')
    currentPhase.value = 'COMPLETED'
    isConfirmingOutline.value = false
    isConnected.value = false
    forgetActiveArticleTask()
    toast.success('正文已生成')
  }

  if (message.type === 'IMAGE_ANALYZED') {
    currentPhase.value = 'IMAGE_GENERATING'
  }

  if (message.type === 'IMAGE_GENERATED') {
    applyImageResults((message.data ?? []) as ArticleImageResult[])
    currentPhase.value = 'CONTENT_MERGING'
  }

  if (message.type === 'MERGE_COMPLETE') {
    generatedContent.value = String(message.data ?? generatedContent.value)
  }

  if (message.type === 'WORKFLOW_EXPIRED') {
    currentPhase.value = 'EXPIRED'
    isCreating.value = false
    isConfirmingTitle.value = false
    isConfirmingOutline.value = false
    isConnected.value = false
    forgetActiveArticleTask()
    toast.error('任务已过期，请重新生成')
  }

  if (message.type === 'ERROR') {
    currentPhase.value = 'FAILED'
    isCreating.value = false
    isConfirmingTitle.value = false
    isConfirmingOutline.value = false
    isConnected.value = false
    forgetActiveArticleTask()
    toast.error(String(message.data ?? message.message ?? '文章生成失败'))
  }
}

/**
 * 建立任务进度连接；刷新页面后可通过 taskId 查询参数恢复到对应阶段。
 */
function connectTaskProgress(nextTaskId: string) {
  closeCurrentSse()
  taskId.value = nextTaskId
  isConnected.value = true
  rememberActiveArticleTask(nextTaskId)
  syncTaskIdToRouteQuery(nextTaskId)

  closeSse = connectArticleSse(nextTaskId, {
    onMessage: handleArticleSseMessage,
    onError(error) {
      currentPhase.value = 'FAILED'
      isCreating.value = false
      isConnected.value = false
      const message = error instanceof Error ? error.message : 'SSE 连接异常'
      toast.error(message)
    },
  })
}

/**
 * 使用热门选题填充输入框，降低第一步创作门槛。
 */
function useHotTopic(value: string) {
  topic.value = value
}

/**
 * 复制正文 Markdown 原文，方便用户粘贴到公众号、文档或后续编辑器。
 */
async function handleCopyGeneratedContent() {
  if (!generatedContent.value.trim()) {
    toast.error('暂无正文可复制')
    return
  }
  if (!isClipboardSupported.value) {
    toast.error('当前浏览器不支持剪贴板复制')
    return
  }
  await copyGeneratedContent()
  toast.success('正文 Markdown 已复制')
}

/**
 * 从图片 URL 中推断扩展名，无法识别时回退到 png。
 */
function resolveImageExtension(url: string, contentType?: string | null) {
  if (contentType?.includes('jpeg') || contentType?.includes('jpg')) {
    return 'jpg'
  }
  if (contentType?.includes('webp')) {
    return 'webp'
  }
  if (contentType?.includes('gif')) {
    return 'gif'
  }
  if (contentType?.includes('svg')) {
    return 'svg'
  }

  try {
    const pathname = new URL(url).pathname
    const matched = pathname.match(/\.([a-zA-Z0-9]+)$/)
    return matched?.[1]?.toLowerCase() || 'png'
  }
  catch {
    return 'png'
  }
}

/**
 * 生成稳定的下载文件名，避免不同图片保存到本地后互相覆盖。
 */
function buildImageFileName(image: ArticleImageResult, index: number, extension: string) {
  const imageLabel = image.position === 1 ? 'cover' : `image-${image.position ?? index + 1}`
  return `article-${taskId.value || 'draft'}-${imageLabel}.${extension}`
}

/**
 * 下载单张图片；如果 OSS 跨域限制阻止 fetch，则打开原图地址让浏览器处理保存。
 */
async function downloadImage(image: ArticleImageResult, index: number) {
  if (!image.url) {
    toast.error('图片地址不存在')
    return
  }
  if (!taskId.value) {
    toast.error('任务 ID 缺失，无法下载图片')
    return
  }

  try {
    const blob = await downloadAppArticleImage(taskId.value, image.url)
    const extension = resolveImageExtension(image.url, blob.type)
    const objectUrl = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = objectUrl
    anchor.download = buildImageFileName(image, index, extension)
    document.body.append(anchor)
    anchor.click()
    anchor.remove()
    URL.revokeObjectURL(objectUrl)
  }
  catch {
    window.open(image.url, '_blank', 'noopener,noreferrer')
    toast.info('浏览器已打开原图，请在新页面保存图片')
  }
}

/**
 * 顺序下载所有已生成图片，避免同时触发多个下载被浏览器拦截。
 */
async function handleDownloadAllImages() {
  if (downloadableImages.value.length === 0) {
    toast.error('暂无可下载图片')
    return
  }

  for (const [index, image] of downloadableImages.value.entries()) {
    await downloadImage(image, index)
  }
}

/**
 * 统一展示积分数值，避免余额接口为空或小数过长时影响侧边栏扫描。
 */
function formatCredits(value?: number) {
  return Number(value ?? 0).toFixed(4)
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
  outlineSections.value = []
  generatedImages.value = []
  coverImage.value = ''
  generatedContent.value = ''
  currentPhase.value = 'TITLE_GENERATING'
  isCreating.value = true

  try {
    const response = await createAppArticleTask({
      topic: normalizedTopic,
      enabledImageMethods: selectedImageMethods.value,
    })
    connectTaskProgress(response.data)
  }
  catch (error: any) {
    currentPhase.value = 'INPUT'
    isCreating.value = false
    const message = error?.data?.message ?? error?.message ?? '创建文章任务失败'
    toast.error(message)
  }
}

/**
 * 确认选中的标题并进入大纲生成阶段。
 */
async function handleConfirmTitle() {
  if (!taskId.value || !selectedTitle.value) {
    toast.error('请先选择一个标题方案')
    return
  }

  isConfirmingTitle.value = true
  try {
    const response = await confirmAppArticleTitle({
      taskId: taskId.value,
      selectedMainTitle: selectedTitle.value.mainTitle,
      selectedSubTitle: selectedTitle.value.subTitle,
      userDescription: buildUserDescription(),
    })

    if (!response.data) {
      throw new Error('标题确认失败，请刷新任务状态后重试')
    }

    currentPhase.value = 'OUTLINE_GENERATING'
    toast.success('标题已确认，正在生成大纲')
  }
  catch (error: any) {
    const message = error?.data?.message ?? error?.message ?? '确认标题失败'
    toast.error(message)
  }
  finally {
    isConfirmingTitle.value = false
  }
}

/**
 * 确认大纲并进入正文生成阶段。
 */
async function handleConfirmOutline() {
  if (!taskId.value) {
    toast.error('任务 ID 缺失，请重新创建任务')
    return
  }

  const outline = buildOutlineForSubmit()
  if (!outline) {
    toast.error('请补全章节标题，并为每个章节至少保留一个有效要点')
    return
  }

  isConfirmingOutline.value = true
  try {
    const response = await confirmAppArticleOutline({
      taskId: taskId.value,
      outline,
    })

    if (!response.data) {
      throw new Error('大纲确认失败，请确认当前任务处于大纲编辑阶段')
    }

    currentPhase.value = 'CONTENT_GENERATING'
    toast.success('大纲已确认，正在生成正文')
  }
  catch (error: any) {
    const message = error?.data?.message ?? error?.message ?? '确认大纲失败'
    toast.error(message)
  }
  finally {
    isConfirmingOutline.value = false
  }
}

onUnmounted(() => {
  closeCurrentSse()
})

onMounted(() => {
  // 首页带 topic 进入时优先展示新建表单，避免本地缓存覆盖用户这次的新选题。
  const resumeTaskId = initialTaskId || (typeof initialTopic === 'string' ? '' : readActiveArticleTaskId())
  if (!resumeTaskId) {
    return
  }
  currentPhase.value = 'PENDING'
  connectTaskProgress(resumeTaskId)
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

              <UiCard>
                <UiCardHeader>
                  <UiCardTitle class="text-base">
                    补充要求
                  </UiCardTitle>
                  <UiCardDescription>可选，确认标题后会带入大纲生成</UiCardDescription>
                </UiCardHeader>
                <UiCardContent>
                  <UiTextarea
                    v-model="userDescription"
                    class="min-h-24 resize-none"
                    placeholder="例如：面向职场新人，语气更犀利，重点写可执行建议"
                    :disabled="isCreating"
                  />
                </UiCardContent>
              </UiCard>

              <UiCard>
                <UiCardHeader>
                  <UiCardTitle class="text-base">
                    配图方式
                  </UiCardTitle>
                  <UiCardDescription>配图 Agent 会在已选方式内规划，失败时自动降级</UiCardDescription>
                </UiCardHeader>
                <UiCardContent>
                  <div class="flex flex-wrap gap-3">
                    <button
                      v-for="item in imageOptions"
                      :key="item.value"
                      type="button"
                      class="flex w-auto items-center gap-2 rounded-md border px-4 py-2.5 text-sm font-medium transition-colors"
                      :class="[
                        selectedImageMethods.includes(item.value) ? 'border-emerald-500 bg-emerald-50' : 'bg-background hover:bg-muted/60',
                        isImageMethodLocked(item.value) ? 'cursor-not-allowed opacity-80' : 'cursor-pointer',
                      ]"
                      @click="handleImageMethodOptionClick(item.value)"
                    >
                      <UiCheckbox
                        :id="`image-${item.value}`"
                        class="pointer-events-none"
                        :model-value="selectedImageMethods.includes(item.value)"
                        :disabled="isImageMethodLocked(item.value)"
                      />
                      <span>{{ item.label }}</span>
                    </button>
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

            <UiCardFooter class="flex flex-col items-stretch gap-3 sm:flex-row sm:items-center sm:justify-between">
              <p class="text-sm text-muted-foreground">
                {{ selectedTitle ? `已选择：${selectedTitle.mainTitle}` : '请选择一个标题方案' }}
              </p>
              <UiButton :disabled="!selectedTitle || isConfirmingTitle" @click="handleConfirmTitle">
                <LoaderCircleIcon v-if="isConfirmingTitle" class="mr-2 size-4 animate-spin" />
                <PenLineIcon v-else class="mr-2 size-4" />
                确认标题，生成大纲
              </UiButton>
            </UiCardFooter>
          </UiCard>

          <UiCard v-else-if="isOutlineGenerating" class="rounded-2xl border-0 bg-muted/40 shadow-sm">
            <UiCardHeader class="items-center px-6 py-10 text-center sm:px-12">
              <UiBadge variant="secondary" class="mb-2 gap-2 bg-emerald-50 text-emerald-600">
                <LoaderCircleIcon class="size-4 animate-spin" />
                第二步：规划大纲
              </UiBadge>
              <UiCardTitle class="text-3xl font-bold tracking-tight sm:text-4xl">
                正在生成大纲
              </UiCardTitle>
              <UiCardDescription class="text-base">
                AI 正在根据标题和补充要求规划文章结构
              </UiCardDescription>
            </UiCardHeader>

            <UiCardContent class="space-y-6 px-6 pb-8 sm:px-12">
              <UiCard>
                <UiCardContent class="space-y-5 p-6">
                  <div class="flex items-start gap-4">
                    <UiSpinner class="mt-1 size-6 text-emerald-600" />
                    <div class="min-w-0">
                      <div class="font-semibold">
                        {{ selectedTitle?.mainTitle }}
                      </div>
                      <p class="mt-2 text-sm leading-6 text-muted-foreground">
                        大纲完成后会自动进入可编辑页面，你可以调整章节和要点后再生成正文。
                      </p>
                    </div>
                  </div>
                  <UiProgress :model-value="62" />
                </UiCardContent>
              </UiCard>
            </UiCardContent>
          </UiCard>

          <UiCard v-else-if="isOutlineEditing" class="rounded-2xl border-0 bg-muted/40 shadow-sm">
            <UiCardHeader class="flex flex-row items-center justify-between gap-3">
              <div>
                <UiCardTitle class="flex items-center gap-2">
                  <FileTextIcon class="size-5" />
                  编辑大纲
                </UiCardTitle>
                <UiCardDescription>
                  直接编辑章节和要点，确认后进入正文生成
                </UiCardDescription>
              </div>
              <div class="flex flex-wrap justify-end gap-2">
                <UiBadge variant="outline">
                  {{ outlineSectionCount }} 个章节
                </UiBadge>
                <UiBadge variant="outline">
                  {{ outlinePointCount }} 个要点
                </UiBadge>
              </div>
            </UiCardHeader>

            <UiCardContent class="space-y-5">
              <UiAlert class="border-emerald-200 bg-emerald-50/70">
                <UiAlertTitle>严格提醒</UiAlertTitle>
                <UiAlertDescription>
                  不要把大纲改成散文。每个章节必须有标题和至少一个有效要点，这样正文生成才有清晰结构。
                </UiAlertDescription>
              </UiAlert>

              <div class="space-y-4">
                <UiCard
                  v-for="(section, sectionIndex) in outlineSections"
                  :key="`outline-section-${section.section}-${sectionIndex}`"
                  class="overflow-hidden border-emerald-100 bg-white/90 shadow-sm"
                >
                  <UiCardHeader class="gap-4 border-b bg-gradient-to-r from-emerald-50 to-white">
                    <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                      <div class="flex items-center gap-3">
                        <UiBadge class="grid size-9 place-items-center rounded-full bg-emerald-600 p-0 text-white">
                          {{ sectionIndex + 1 }}
                        </UiBadge>
                        <div>
                          <UiCardTitle class="text-base">
                            章节 {{ sectionIndex + 1 }}
                          </UiCardTitle>
                          <UiCardDescription>先定章节，再补要点</UiCardDescription>
                        </div>
                      </div>
                      <UiButton
                        variant="ghost"
                        size="sm"
                        class="text-muted-foreground hover:text-destructive"
                        :disabled="outlineSections.length <= 1"
                        @click="removeOutlineSection(sectionIndex)"
                      >
                        <Trash2Icon class="mr-2 size-4" />
                        删除章节
                      </UiButton>
                    </div>

                    <UiField>
                      <UiFieldLabel :for="`outline-section-title-${sectionIndex}`">
                        章节标题
                      </UiFieldLabel>
                      <UiInput
                        :id="`outline-section-title-${sectionIndex}`"
                        v-model="section.title"
                        class="h-12 bg-background text-base font-semibold"
                        placeholder="请输入章节标题"
                      />
                    </UiField>
                  </UiCardHeader>

                  <UiCardContent class="space-y-3 p-5">
                    <div class="flex items-center justify-between gap-3">
                      <div>
                        <div class="font-medium">
                          核心要点
                        </div>
                        <p class="text-sm text-muted-foreground">
                          用短句说明这一节必须展开的内容
                        </p>
                      </div>
                      <UiButton variant="outline" size="sm" @click="addOutlinePoint(sectionIndex)">
                        <PlusIcon class="mr-2 size-4" />
                        添加要点
                      </UiButton>
                    </div>

                    <div class="space-y-3">
                      <div
                        v-for="(_, pointIndex) in section.points"
                        :key="`outline-point-${sectionIndex}-${pointIndex}`"
                        class="flex items-center gap-3 rounded-xl border bg-muted/20 p-3"
                      >
                        <UiBadge variant="secondary" class="shrink-0">
                          {{ pointIndex + 1 }}
                        </UiBadge>
                        <UiInput
                          v-model="section.points[pointIndex]"
                          class="h-10 bg-background"
                          placeholder="请输入要点，例如：解释问题背景并给出具体例子"
                        />
                        <UiButton
                          variant="ghost"
                          size="icon"
                          class="shrink-0 text-muted-foreground hover:text-destructive"
                          :disabled="section.points.length <= 1"
                          @click="removeOutlinePoint(sectionIndex, pointIndex)"
                        >
                          <Trash2Icon class="size-4" />
                        </UiButton>
                      </div>
                    </div>
                  </UiCardContent>
                </UiCard>
              </div>
            </UiCardContent>

            <UiCardFooter class="flex flex-col items-stretch gap-3 sm:flex-row sm:justify-between">
              <UiButton variant="outline" @click="addOutlineSection">
                <PlusIcon class="mr-2 size-4" />
                添加章节
              </UiButton>
              <UiButton :disabled="isConfirmingOutline" @click="handleConfirmOutline">
                <LoaderCircleIcon v-if="isConfirmingOutline" class="mr-2 size-4 animate-spin" />
                <RocketIcon v-else class="mr-2 size-4" />
                确认大纲，生成正文
              </UiButton>
            </UiCardFooter>
          </UiCard>

          <UiCard v-else-if="isContentGenerating" class="rounded-2xl border-0 bg-muted/40 shadow-sm">
            <UiCardHeader class="items-center px-6 py-10 text-center sm:px-12">
              <UiBadge variant="secondary" class="mb-2 gap-2 bg-emerald-50 text-emerald-600">
                <LoaderCircleIcon class="size-4 animate-spin" />
                第三步：撰写正文
              </UiBadge>
              <UiCardTitle class="text-3xl font-bold tracking-tight sm:text-4xl">
                {{ generationTitle }}
              </UiCardTitle>
              <UiCardDescription class="text-base">
                正在扩写每个章节，完成后会展示 Markdown 正文
              </UiCardDescription>
            </UiCardHeader>

            <UiCardContent class="space-y-6 px-6 pb-8 sm:px-12">
              <UiCard>
                <UiCardContent class="space-y-5 p-6">
                  <div class="flex items-start gap-4">
                    <UiSpinner class="mt-1 size-6 text-emerald-600" />
                    <div>
                      <div class="font-semibold">
                        正在写作：{{ selectedTitle?.mainTitle || '已确认标题' }}
                      </div>
                      <p class="mt-2 text-sm leading-6 text-muted-foreground">
                        当前后端一次性返回正文结果，页面会在完成后自动切换到预览。
                      </p>
                    </div>
                  </div>
                  <UiProgress :model-value="82" />
                </UiCardContent>
              </UiCard>
            </UiCardContent>
          </UiCard>

          <UiCard v-else-if="isCompleted" class="rounded-2xl border-0 bg-muted/40 shadow-sm">
            <UiCardHeader class="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
              <div>
                <UiBadge variant="secondary" class="mb-3 gap-2 bg-emerald-50 text-emerald-600">
                  <CheckCircle2Icon class="size-4" />
                  已完成
                </UiBadge>
                <UiCardTitle class="text-2xl">
                  正文生成结果
                </UiCardTitle>
                <UiCardDescription>
                  Markdown 正文已保存，下面已转换为富文本阅读视图
                </UiCardDescription>
              </div>
              <div class="flex flex-col items-start gap-2 sm:items-end">
                <UiBadge variant="outline" class="max-w-full truncate">
                  {{ taskId }}
                </UiBadge>
                <UiButton
                  variant="outline"
                  size="sm"
                  :disabled="!generatedContent || !isClipboardSupported"
                  @click="handleCopyGeneratedContent"
                >
                  <ClipboardCheckIcon v-if="isGeneratedContentCopied" class="mr-2 size-4 text-emerald-600" />
                  <ClipboardIcon v-else class="mr-2 size-4" />
                  {{ isGeneratedContentCopied ? '已复制' : '复制正文' }}
                </UiButton>
                <UiButton
                  variant="outline"
                  size="sm"
                  :disabled="downloadableImages.length === 0"
                  @click="handleDownloadAllImages"
                >
                  <DownloadIcon class="mr-2 size-4" />
                  下载全部图片
                </UiButton>
              </div>
            </UiCardHeader>

            <UiCardContent class="space-y-4">
              <div v-if="coverImage" class="overflow-hidden rounded-2xl border bg-white shadow-sm">
                <img
                  :src="coverImage"
                  :alt="coverImageAlt"
                  class="aspect-[16/9] w-full object-cover"
                >
                <div class="border-t px-4 py-3">
                  <div class="flex items-center justify-between gap-3">
                    <div class="text-sm font-medium">
                      封面图
                    </div>
                    <UiButton
                      variant="ghost"
                      size="sm"
                      @click="downloadImage(downloadableImages.find(image => image.position === 1) ?? { position: 1, url: coverImage, description: coverImageAlt }, 0)"
                    >
                      <DownloadIcon class="mr-2 size-4" />
                      下载封面
                    </UiButton>
                  </div>
                </div>
              </div>

              <div class="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-emerald-100 bg-emerald-50/70 px-4 py-3 text-sm text-emerald-900">
                <span>富文本预览已按标题、段落、列表和代码块排版。</span>
                <UiBadge variant="secondary">
                  {{ generatedContentLength }} 字符
                </UiBadge>
              </div>

              <div class="max-h-[720px] overflow-auto rounded-2xl border bg-white p-6 shadow-inner">
                <MarkdownContentRenderer :content="generatedContent" />
              </div>
            </UiCardContent>
          </UiCard>

          <UiCard v-else-if="isExpired" class="rounded-2xl border-0 bg-muted/40 shadow-sm">
            <UiCardHeader>
              <UiCardTitle>创作任务已过期</UiCardTitle>
              <UiCardDescription>标题或大纲确认等待超过有效期，旧流程不能继续恢复。</UiCardDescription>
            </UiCardHeader>
            <UiCardContent>
              <UiButton @click="resetCreatorState">
                <RocketIcon class="mr-2 size-4" />
                重新生成
              </UiButton>
            </UiCardContent>
          </UiCard>

          <UiCard v-else class="rounded-2xl border-0 bg-muted/40 shadow-sm">
            <UiCardHeader>
              <UiCardTitle>创作任务异常</UiCardTitle>
              <UiCardDescription>请返回输入阶段后重试。</UiCardDescription>
            </UiCardHeader>
            <UiCardContent>
              <UiButton @click="resetCreatorState">
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
                <WalletCardsIcon class="size-5" />
                积分余额
              </UiCardTitle>
            </UiCardHeader>
            <UiCardContent class="space-y-3">
              <div class="flex items-end justify-between gap-3">
                <div>
                  <div class="text-2xl font-semibold leading-none">
                    {{ isFetchingCreditSummary ? '加载中' : formatCredits(creditSummary?.balance) }}
                  </div>
                  <div class="mt-1 text-xs text-muted-foreground">
                    当前可用积分
                  </div>
                </div>
                <UiBadge variant="secondary" class="rounded-full">
                  按 AI 用量扣减
                </UiBadge>
              </div>
              <UiSeparator />
              <div class="grid grid-cols-2 gap-3 text-sm">
                <div>
                  <div class="text-muted-foreground">
                    累计充值
                  </div>
                  <div class="mt-1 font-medium">
                    {{ formatCredits(creditSummary?.totalRecharge) }}
                  </div>
                </div>
                <div>
                  <div class="text-muted-foreground">
                    累计使用
                  </div>
                  <div class="mt-1 font-medium">
                    {{ formatCredits(creditSummary?.totalConsume) }}
                  </div>
                </div>
              </div>
              <UiBadge v-if="!isFetchingCreditSummary && (creditSummary?.balance ?? 0) <= 0" variant="destructive" class="rounded-full">
                余额不足，请联系管理员充值
              </UiBadge>
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

    <PromptFeedbackCard
      v-if="activePromptFeedback"
      :task-id="taskId"
      :stage="activePromptFeedback.stage"
      :title="activePromptFeedback.title"
      :description="activePromptFeedback.description"
    />
  </div>
</template>

<route lang="yaml">
meta:
  layout: user
  fullWidth: true
</route>
