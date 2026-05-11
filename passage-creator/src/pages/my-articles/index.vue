<script setup lang="ts">
import {
  CheckCircle2Icon,
  Clock3Icon,
  FileTextIcon,
  LoaderCircleIcon,
  PlayIcon,
  RefreshCwIcon,
  RotateCcwIcon,
  SearchIcon,
  XCircleIcon,
} from '@lucide/vue'

import type { AppArticleItem, AppArticleQuery } from '@/services/types/app-article.type'

import { useGetAppArticlePageQuery } from '@/services/api/app-article.api'

const router = useRouter()

const query = reactive<AppArticleQuery>({
  page: 1,
  pageSize: 8,
  topic: '',
  title: '',
  status: '',
})

const { data, isFetching, refetch } = useGetAppArticlePageQuery(query)

/**
 * 当前用户文章创建记录。
 */
const articleList = computed(() => data.value?.data?.records ?? [])
const total = computed(() => data.value?.data?.totalRow ?? 0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / query.pageSize)))

const statusFilter = computed({
  get: () => query.status || 'all',
  set: value => query.status = value === 'all' ? '' : value as AppArticleQuery['status'],
})

const statusOptions = [
  { label: '全部', value: 'all', icon: FileTextIcon },
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
 * 记录卡片标题优先展示用户选定标题，否则回退到原始选题。
 */
function getDisplayTitle(article: AppArticleItem) {
  return article.mainTitle || article.topic || '未命名文章'
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
  query.page = 1
  refetch()
}

/**
 * 重置用户输入的筛选条件。
 */
function handleReset() {
  query.page = 1
  query.pageSize = 8
  query.topic = ''
  query.title = ''
  query.status = ''
  refetch()
}

/**
 * 切换分页并刷新当前记录页。
 */
function changePage(nextPage: number) {
  query.page = Math.min(Math.max(nextPage, 1), totalPages.value)
  refetch()
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
          查看已创建的文章任务，并从中断的标题、大纲或正文生成阶段继续。
        </p>
      </div>

      <div class="flex flex-wrap gap-2">
        <UiButton variant="outline" :disabled="isFetching" @click="refetch()">
          <RefreshCwIcon class="mr-2 size-4" :class="{ 'animate-spin': isFetching }" />
          刷新
        </UiButton>
        <UiButton @click="router.push('/article-creator')">
          <PlayIcon class="mr-2 size-4" />
          新建文章
        </UiButton>
      </div>
    </section>

    <UiCard class="border-border/70">
      <UiCardHeader>
        <UiCardTitle>筛选记录</UiCardTitle>
        <UiCardDescription>按选题、标题或生成状态缩小范围。</UiCardDescription>
      </UiCardHeader>
      <UiCardContent class="grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_220px_180px]">
        <div class="space-y-2">
          <UiLabel>选题</UiLabel>
          <UiInput v-model="query.topic" placeholder="输入选题关键字" @keydown.enter="handleSearch" />
        </div>

        <div class="space-y-2">
          <UiLabel>标题</UiLabel>
          <UiInput v-model="query.title" placeholder="输入标题关键字" @keydown.enter="handleSearch" />
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

    <div v-if="isFetching" class="flex items-center justify-center rounded-lg border bg-background py-16 text-sm text-muted-foreground">
      <LoaderCircleIcon class="mr-2 size-4 animate-spin" />
      正在加载创作记录...
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
            暂无创作记录
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
      <span>共 {{ total }} 条记录，第 {{ query.page }} / {{ totalPages }} 页</span>
      <div class="flex gap-2">
        <UiButton variant="outline" size="sm" :disabled="query.page <= 1" @click="changePage(query.page - 1)">
          上一页
        </UiButton>
        <UiButton variant="outline" size="sm" :disabled="query.page >= totalPages" @click="changePage(query.page + 1)">
          下一页
        </UiButton>
      </div>
    </div>
  </div>
</template>

<route lang="yaml">
meta:
  layout: user
  auth: true
</route>
