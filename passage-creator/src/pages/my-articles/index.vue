<script setup lang="ts">
import {
  CheckCircle2Icon,
  Clock3Icon,
  FileTextIcon,
  ImageIcon,
  LoaderCircleIcon,
  PlayIcon,
  RefreshCwIcon,
  RotateCcwIcon,
  SearchIcon,
  SparklesIcon,
  WandSparklesIcon,
  XCircleIcon,
} from '@lucide/vue'

import type { AppArticleItem, AppArticleQuery } from '@/services/types/app-article.type'
import type { AppRednoteItem, AppRednoteQuery } from '@/services/types/app-rednote.type'

import { useGetAppArticlePageQuery } from '@/services/api/app-article.api'
import { useGetAppRednotePageQuery } from '@/services/api/app-rednote.api'

const router = useRouter()
const activeTab = ref('article')

const articleQuery = reactive<AppArticleQuery>({
  page: 1,
  pageSize: 8,
  topic: '',
  title: '',
  status: '',
})

const rednoteQuery = reactive<AppRednoteQuery>({
  page: 1,
  pageSize: 8,
  content: '',
  subject: '',
  status: '',
  phase: '',
})

const { data: articleData, isFetching: isArticleFetching, refetch: refetchArticles } = useGetAppArticlePageQuery(articleQuery)
const { data: rednoteData, isFetching: isRednoteFetching, refetch: refetchRednotes } = useGetAppRednotePageQuery(rednoteQuery)

/**
 * 当前用户文章创建记录。
 */
const articleList = computed(() => articleData.value?.data?.records ?? [])
const articleTotal = computed(() => articleData.value?.data?.totalRow ?? 0)
const articleTotalPages = computed(() => Math.max(1, Math.ceil(articleTotal.value / articleQuery.pageSize)))

/**
 * 当前用户小红书创建记录。
 */
const rednoteList = computed(() => rednoteData.value?.data?.records ?? [])
const rednoteTotal = computed(() => rednoteData.value?.data?.totalRow ?? 0)
const rednoteTotalPages = computed(() => Math.max(1, Math.ceil(rednoteTotal.value / rednoteQuery.pageSize)))

const statusFilter = computed({
  get: () => articleQuery.status || 'all',
  set: value => articleQuery.status = value === 'all' ? '' : value as AppArticleQuery['status'],
})

const rednoteStatusFilter = computed({
  get: () => rednoteQuery.status || 'all',
  set: value => rednoteQuery.status = value === 'all' ? '' : value as AppRednoteQuery['status'],
})

const statusOptions = [
  { label: '全部', value: 'all', icon: FileTextIcon },
  { label: '等待处理', value: 'PENDING', icon: Clock3Icon },
  { label: '处理中', value: 'PROCESSING', icon: LoaderCircleIcon },
  { label: '已完成', value: 'COMPLETED', icon: CheckCircle2Icon },
  { label: '失败', value: 'FAILED', icon: XCircleIcon },
] as const

const rednoteStatusOptions = [
  { label: '全部', value: 'all', icon: WandSparklesIcon },
  { label: '等待处理', value: 'PENDING', icon: Clock3Icon },
  { label: '处理中', value: 'PROCESSING', icon: LoaderCircleIcon },
  { label: '已完成', value: 'COMPLETED', icon: CheckCircle2Icon },
  { label: '失败', value: 'FAILED', icon: XCircleIcon },
] as const

const phaseLabels: Record<string, string> = {
  PENDING: '等待处理',
  TITLE_GENERATING: '生成标题',
  TITLE_SELECTING: '选择标题',
  OUTLINE_GENERATING: '生成大纲',
  OUTLINE_EDITING: '编辑大纲',
  CONTENT_GENERATING: '生成正文',
  COMPLETED: '已完成',
  FAILED: '失败',
}

const rednotePhaseLabels: Record<string, string> = {
  PENDING: '等待处理',
  SEARCH_AGENT: '搜索素材',
  COPY_GENERATING: '生成文案',
  IMAGE_PROMPT_GENERATING: '规划图片',
  IMAGE_GENERATING: '生成图片',
  COMPLETED: '已完成',
  FAILED: '失败',
}

/**
 * 根据状态给徽标选择语义化样式。
 */
function getStatusVariant(status?: string) {
  if (status === 'COMPLETED') {
    return 'default'
  }
  if (status === 'FAILED') {
    return 'destructive'
  }
  if (status === 'PROCESSING') {
    return 'outline'
  }
  return 'secondary'
}

/**
 * 把后端阶段值转换成用户能理解的流程节点名称。
 */
function getPhaseLabel(phase?: string) {
  if (!phase) {
    return '未开始'
  }
  return phaseLabels[phase] ?? phase
}

/**
 * 把小红书后端阶段值转换成用户可读名称。
 */
function getRednotePhaseLabel(phase?: string) {
  if (!phase) {
    return '未开始'
  }
  return rednotePhaseLabels[phase] ?? phase
}

/**
 * 记录卡片标题优先展示用户选定标题，否则回退到原始选题。
 */
function getDisplayTitle(article: AppArticleItem) {
  return article.mainTitle || article.topic || '未命名文章'
}

/**
 * 小红书记录优先展示主题，其次使用封面标题和原始需求。
 */
function getRednoteDisplayTitle(note: AppRednoteItem) {
  return note.subject || note.coverTitle || note.content || '未命名小红书创作'
}

/**
 * 小红书记录摘要用于卡片二级信息，避免直接暴露过长原始需求。
 */
function getRednoteSummary(note: AppRednoteItem) {
  return note.bodyContent || note.context || note.content || '暂无内容摘要'
}

/**
 * 时间字段统一格式化，空值显示占位符。
 */
function formatTime(value?: string) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}

/**
 * 查询时回到第一页，避免筛选后页码越界。
 */
function handleSearch() {
  articleQuery.page = 1
  refetchArticles()
}

/**
 * 查询小红书记录时回到第一页。
 */
function handleRednoteSearch() {
  rednoteQuery.page = 1
  refetchRednotes()
}

/**
 * 重置用户输入的筛选条件。
 */
function handleReset() {
  articleQuery.page = 1
  articleQuery.pageSize = 8
  articleQuery.topic = ''
  articleQuery.title = ''
  articleQuery.status = ''
  refetchArticles()
}

/**
 * 重置小红书记录筛选条件。
 */
function handleRednoteReset() {
  rednoteQuery.page = 1
  rednoteQuery.pageSize = 8
  rednoteQuery.content = ''
  rednoteQuery.subject = ''
  rednoteQuery.status = ''
  rednoteQuery.phase = ''
  refetchRednotes()
}

/**
 * 切换分页并刷新当前记录页。
 */
function changePage(nextPage: number) {
  articleQuery.page = Math.min(Math.max(nextPage, 1), articleTotalPages.value)
  refetchArticles()
}

/**
 * 切换小红书分页并刷新当前记录页。
 */
function changeRednotePage(nextPage: number) {
  rednoteQuery.page = Math.min(Math.max(nextPage, 1), rednoteTotalPages.value)
  refetchRednotes()
}

/**
 * 回到创作流程页，创作页会用 taskId 拉取快照并恢复到对应阶段。
 */
function continueCreation(article: AppArticleItem) {
  router.push({
    path: '/article-creator',
    query: { taskId: article.taskId },
  })
}

/**
 * 回到小红书创作页，创作页会通过 taskId 拉取详情并恢复实时进度。
 */
function continueRednoteCreation(note: AppRednoteItem) {
  router.push({
    path: '/rednote-creator',
    query: { taskId: note.taskId },
  })
}
</script>

<template>
  <div class="mx-auto max-w-[1160px] space-y-6 py-2">
    <section class="flex flex-col gap-4 border-b border-emerald-100 pb-6 md:flex-row md:items-end md:justify-between">
      <div>
        <UiBadge variant="secondary" class="mb-3 gap-2 bg-emerald-50 text-emerald-700">
          <FileTextIcon class="size-4" />
          用户端创作记录
        </UiBadge>
        <h1 class="text-3xl font-bold tracking-tight text-slate-950">
          我的创作记录
        </h1>
        <p class="mt-2 text-sm leading-6 text-muted-foreground">
          查看已创建的文章和小红书任务，并从中断的生成阶段继续。
        </p>
      </div>

      <div class="flex flex-wrap gap-2">
        <UiButton variant="outline" :disabled="activeTab === 'article' ? isArticleFetching : isRednoteFetching" @click="activeTab === 'article' ? refetchArticles() : refetchRednotes()">
          <RefreshCwIcon class="mr-2 size-4" :class="{ 'animate-spin': activeTab === 'article' ? isArticleFetching : isRednoteFetching }" />
          刷新
        </UiButton>
        <UiButton @click="router.push('/article-creator')">
          <PlayIcon class="mr-2 size-4" />
          新建文章
        </UiButton>
        <UiButton
          variant="outline"
          class="border-rose-200 bg-rose-50 text-rose-700 shadow-sm transition-all hover:border-rose-300 hover:bg-rose-100 hover:text-rose-800 hover:shadow-md"
          @click="router.push('/rednote-creator')"
        >
          <WandSparklesIcon class="mr-2 size-4" />
          新建小红书
        </UiButton>
      </div>
    </section>

    <UiTabs v-model="activeTab" class="space-y-6">
      <UiTabsList class="grid w-full grid-cols-2 md:w-[420px]">
        <UiTabsTrigger value="article" class="gap-2">
          <FileTextIcon class="size-4" />
          文章记录
        </UiTabsTrigger>
        <UiTabsTrigger value="rednote" class="gap-2">
          <WandSparklesIcon class="size-4" />
          小红书记录
        </UiTabsTrigger>
      </UiTabsList>

      <UiTabsContent value="article" class="space-y-6">
        <UiCard class="border-border/70">
          <UiCardHeader>
            <UiCardTitle>筛选文章记录</UiCardTitle>
            <UiCardDescription>按选题、标题或生成状态缩小范围。</UiCardDescription>
          </UiCardHeader>
          <UiCardContent class="grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_220px_180px]">
            <div class="space-y-2">
              <UiLabel>选题</UiLabel>
              <UiInput v-model="articleQuery.topic" placeholder="输入选题关键字" @keydown.enter="handleSearch" />
            </div>

            <div class="space-y-2">
              <UiLabel>标题</UiLabel>
              <UiInput v-model="articleQuery.title" placeholder="输入标题关键字" @keydown.enter="handleSearch" />
            </div>

            <div class="space-y-2">
              <UiLabel>状态</UiLabel>
              <UiSelect v-model="statusFilter">
                <UiSelectTrigger class="w-full">
                  <UiSelectValue placeholder="全部状态" />
                </UiSelectTrigger>
                <UiSelectContent>
                  <UiSelectItem v-for="item in statusOptions" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </UiSelectItem>
                </UiSelectContent>
              </UiSelect>
            </div>

            <div class="flex items-end gap-2">
              <UiButton class="flex-1" @click="handleSearch">
                <SearchIcon class="mr-2 size-4" />
                查询
              </UiButton>
              <UiButton variant="outline" size="icon" @click="handleReset">
                <RotateCcwIcon class="size-4" />
              </UiButton>
            </div>
          </UiCardContent>
        </UiCard>

        <div v-if="isArticleFetching" class="flex items-center justify-center rounded-lg border bg-background py-16 text-sm text-muted-foreground">
          <LoaderCircleIcon class="mr-2 size-4 animate-spin" />
          正在加载文章记录...
        </div>

        <section v-else class="grid gap-4">
          <UiCard
            v-for="article in articleList"
            :key="article.id"
            class="overflow-hidden border-border/70 transition-colors hover:border-emerald-200"
          >
            <UiCardContent class="grid gap-5 p-5 lg:grid-cols-[minmax(0,1fr)_220px] lg:items-center">
              <div class="min-w-0 space-y-3">
                <div class="flex flex-wrap items-center gap-2">
                  <UiBadge :variant="getStatusVariant(article.status)">
                    {{ statusOptions.find(item => item.value === article.status)?.label ?? article.status }}
                  </UiBadge>
                  <UiBadge variant="outline">
                    {{ getPhaseLabel(article.phase) }}
                  </UiBadge>
                  <span class="text-xs text-muted-foreground">任务 ID：{{ article.taskId }}</span>
                </div>

                <div>
                  <h2 class="line-clamp-2 text-xl font-semibold tracking-tight">
                    {{ getDisplayTitle(article) }}
                  </h2>
                  <p v-if="article.subTitle" class="mt-1 line-clamp-2 text-sm leading-6 text-muted-foreground">
                    {{ article.subTitle }}
                  </p>
                  <p v-else class="mt-1 line-clamp-2 text-sm leading-6 text-muted-foreground">
                    {{ article.topic }}
                  </p>
                </div>

                <div v-if="article.errorMessage" class="rounded-md border border-destructive/20 bg-destructive/5 px-3 py-2 text-sm text-destructive">
                  {{ article.errorMessage }}
                </div>

                <div class="grid gap-2 text-xs text-muted-foreground sm:grid-cols-3">
                  <span>创建：{{ formatTime(article.createTime) }}</span>
                  <span>更新：{{ formatTime(article.updateTime) }}</span>
                  <span>完成：{{ formatTime(article.completedTime) }}</span>
                </div>
              </div>

              <div class="flex flex-col gap-2 lg:items-end">
                <UiButton class="w-full lg:w-auto" @click="continueCreation(article)">
                  <PlayIcon class="mr-2 size-4" />
                  {{ article.status === 'COMPLETED' ? '查看文章' : '继续创作' }}
                </UiButton>
              </div>
            </UiCardContent>
          </UiCard>

          <UiCard v-if="articleList.length === 0" class="border-dashed">
            <UiCardContent class="flex flex-col items-center justify-center py-16 text-center">
              <div class="grid size-12 place-items-center rounded-full bg-emerald-50 text-emerald-600">
                <FileTextIcon class="size-6" />
              </div>
              <h2 class="mt-4 text-lg font-semibold">
                暂无文章记录
              </h2>
              <p class="mt-2 max-w-md text-sm leading-6 text-muted-foreground">
                创建文章任务后，这里会保存标题选择、大纲编辑和正文生成进度。
              </p>
              <UiButton class="mt-5" @click="router.push('/article-creator')">
                <PlayIcon class="mr-2 size-4" />
                开始创作
              </UiButton>
            </UiCardContent>
          </UiCard>
        </section>

        <div class="flex items-center justify-between text-sm text-muted-foreground">
          <span>共 {{ articleTotal }} 条记录，第 {{ articleQuery.page }} / {{ articleTotalPages }} 页</span>
          <div class="flex gap-2">
            <UiButton variant="outline" size="sm" :disabled="articleQuery.page <= 1" @click="changePage(articleQuery.page - 1)">
              上一页
            </UiButton>
            <UiButton variant="outline" size="sm" :disabled="articleQuery.page >= articleTotalPages" @click="changePage(articleQuery.page + 1)">
              下一页
            </UiButton>
          </div>
        </div>
      </UiTabsContent>

      <UiTabsContent value="rednote" class="space-y-6">
        <UiCard class="border-border/70">
          <UiCardHeader>
            <UiCardTitle>筛选小红书记录</UiCardTitle>
            <UiCardDescription>按需求、主题或生成状态缩小范围。</UiCardDescription>
          </UiCardHeader>
          <UiCardContent class="grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_220px_180px]">
            <div class="space-y-2">
              <UiLabel>创作需求</UiLabel>
              <UiInput v-model="rednoteQuery.content" placeholder="输入需求关键字" @keydown.enter="handleRednoteSearch" />
            </div>

            <div class="space-y-2">
              <UiLabel>主题</UiLabel>
              <UiInput v-model="rednoteQuery.subject" placeholder="输入主题关键字" @keydown.enter="handleRednoteSearch" />
            </div>

            <div class="space-y-2">
              <UiLabel>状态</UiLabel>
              <UiSelect v-model="rednoteStatusFilter">
                <UiSelectTrigger class="w-full">
                  <UiSelectValue placeholder="全部状态" />
                </UiSelectTrigger>
                <UiSelectContent>
                  <UiSelectItem v-for="item in rednoteStatusOptions" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </UiSelectItem>
                </UiSelectContent>
              </UiSelect>
            </div>

            <div class="flex items-end gap-2">
              <UiButton class="flex-1" @click="handleRednoteSearch">
                <SearchIcon class="mr-2 size-4" />
                查询
              </UiButton>
              <UiButton variant="outline" size="icon" @click="handleRednoteReset">
                <RotateCcwIcon class="size-4" />
              </UiButton>
            </div>
          </UiCardContent>
        </UiCard>

        <div v-if="isRednoteFetching" class="flex items-center justify-center rounded-lg border bg-background py-16 text-sm text-muted-foreground">
          <LoaderCircleIcon class="mr-2 size-4 animate-spin" />
          正在加载小红书记录...
        </div>

        <section v-else class="grid gap-4">
          <UiCard
            v-for="note in rednoteList"
            :key="note.id ?? note.taskId"
            class="overflow-hidden border-border/70 transition-colors hover:border-rose-200"
          >
            <UiCardContent class="grid gap-5 p-5 lg:grid-cols-[minmax(0,1fr)_220px] lg:items-center">
              <div class="min-w-0 space-y-3">
                <div class="flex flex-wrap items-center gap-2">
                  <UiBadge :variant="getStatusVariant(note.status)">
                    {{ note.statusLabel || rednoteStatusOptions.find(item => item.value === note.status)?.label || note.status }}
                  </UiBadge>
                  <UiBadge variant="outline">
                    {{ note.phaseLabel || getRednotePhaseLabel(note.phase) }}
                  </UiBadge>
                  <UiBadge v-if="note.imageCount" variant="secondary" class="gap-1">
                    <ImageIcon class="size-3.5" />
                    {{ note.imageCount }} 张图
                  </UiBadge>
                  <span class="text-xs text-muted-foreground">任务 ID：{{ note.taskId }}</span>
                </div>

                <div>
                  <h2 class="line-clamp-2 text-xl font-semibold tracking-tight">
                    {{ getRednoteDisplayTitle(note) }}
                  </h2>
                  <p class="mt-1 line-clamp-2 text-sm leading-6 text-muted-foreground">
                    {{ getRednoteSummary(note) }}
                  </p>
                </div>

                <div v-if="note.errorMessage" class="rounded-md border border-destructive/20 bg-destructive/5 px-3 py-2 text-sm text-destructive">
                  {{ note.errorMessage }}
                </div>

                <div class="grid gap-2 text-xs text-muted-foreground sm:grid-cols-3">
                  <span>创建：{{ formatTime(note.createTime) }}</span>
                  <span>更新：{{ formatTime(note.updateTime) }}</span>
                  <span>完成：{{ formatTime(note.completedTime) }}</span>
                </div>
              </div>

              <div class="flex flex-col gap-2 lg:items-end">
                <UiButton class="w-full lg:w-auto" @click="continueRednoteCreation(note)">
                  <SparklesIcon class="mr-2 size-4" />
                  {{ note.status === 'COMPLETED' ? '查看小红书' : '继续创作' }}
                </UiButton>
              </div>
            </UiCardContent>
          </UiCard>

          <UiCard v-if="rednoteList.length === 0" class="border-dashed">
            <UiCardContent class="flex flex-col items-center justify-center py-16 text-center">
              <div class="grid size-12 place-items-center rounded-full bg-rose-50 text-rose-600">
                <WandSparklesIcon class="size-6" />
              </div>
              <h2 class="mt-4 text-lg font-semibold">
                暂无小红书记录
              </h2>
              <p class="mt-2 max-w-md text-sm leading-6 text-muted-foreground">
                创建小红书任务后，这里会保存素材搜索、文案生成和图片生成进度。
              </p>
              <UiButton class="mt-5" @click="router.push('/rednote-creator')">
                <WandSparklesIcon class="mr-2 size-4" />
                开始创作
              </UiButton>
            </UiCardContent>
          </UiCard>
        </section>

        <div class="flex items-center justify-between text-sm text-muted-foreground">
          <span>共 {{ rednoteTotal }} 条记录，第 {{ rednoteQuery.page }} / {{ rednoteTotalPages }} 页</span>
          <div class="flex gap-2">
            <UiButton variant="outline" size="sm" :disabled="rednoteQuery.page <= 1" @click="changeRednotePage(rednoteQuery.page - 1)">
              上一页
            </UiButton>
            <UiButton variant="outline" size="sm" :disabled="rednoteQuery.page >= rednoteTotalPages" @click="changeRednotePage(rednoteQuery.page + 1)">
              下一页
            </UiButton>
          </div>
        </div>
      </UiTabsContent>
    </UiTabs>
  </div>
</template>

<route lang="yaml">
meta:
  layout: user
  auth: true
  section: user
</route>
