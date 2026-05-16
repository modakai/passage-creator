<script setup lang="ts">
import { FrownIcon, HeartIcon, LoaderCircleIcon, MehIcon, MessageSquareTextIcon, RefreshCwIcon, SearchIcon, SmileIcon } from '@lucide/vue'

import type {
  PromptFeedbackQuery,
  PromptFeedbackRating,
  PromptFeedbackStage,
} from '@/services/types/prompt-template.type'

import { BasicPage } from '@/components/global-layout'
import {
  useGetPromptFeedbackPageQuery,
  useGetPromptFeedbackStatsQuery,
} from '@/services/api/prompt-template.api'

const ALL_SELECT_VALUE = '__all__'

/**
 * Prompt 反馈分页与统计共用查询条件。
 */
const query = reactive<PromptFeedbackQuery>({
  page: 1,
  pageSize: 10,
  taskId: '',
  feedbackStage: '',
  rating: '',
  templateKey: '',
  version: '',
  startTime: '',
  endTime: '',
})

const { data: feedbackData, isFetching: isFetchingFeedback, refetch: refetchFeedback } = useGetPromptFeedbackPageQuery(query)
const { data: statsData, isFetching: isFetchingStats, refetch: refetchStats } = useGetPromptFeedbackStatsQuery(query)

const feedbackRecords = computed(() => feedbackData.value?.data?.records ?? [])
const feedbackTotal = computed(() => feedbackData.value?.data?.totalRow ?? 0)
const totalPages = computed(() => Math.max(1, Math.ceil(feedbackTotal.value / query.pageSize)))
const stats = computed(() => statsData.value?.data ?? [])
const isFetching = computed(() => isFetchingFeedback.value || isFetchingStats.value)

/**
 * 四档满意度展示配置，管理统计卡片和筛选项共用同一份定义。
 */
const ratingOptions = [
  {
    value: 'VERY_SATISFIED',
    label: '非常满意',
    icon: HeartIcon,
    countKey: 'verySatisfiedCount',
    ratioKey: 'verySatisfiedRatio',
    iconClass: 'text-rose-500',
  },
  {
    value: 'SATISFIED',
    label: '满意',
    icon: SmileIcon,
    countKey: 'satisfiedCount',
    ratioKey: 'satisfiedRatio',
    iconClass: 'text-emerald-600',
  },
  {
    value: 'NEUTRAL',
    label: '一般',
    icon: MehIcon,
    countKey: 'neutralCount',
    ratioKey: 'neutralRatio',
    iconClass: 'text-amber-600',
  },
  {
    value: 'UNSATISFIED',
    label: '不满意',
    icon: FrownIcon,
    countKey: 'unsatisfiedCount',
    ratioKey: 'unsatisfiedRatio',
    iconClass: 'text-red-600',
  },
] as const

const stageFilter = computed({
  // Reka Select 不能使用空字符串选项，使用哨兵值代表全部环节。
  get: () => query.feedbackStage === '' ? ALL_SELECT_VALUE : String(query.feedbackStage),
  set: (value) => {
    query.feedbackStage = value === ALL_SELECT_VALUE ? '' : value as PromptFeedbackStage
  },
})

const ratingFilter = computed({
  // Reka Select 不能使用空字符串选项，使用哨兵值代表全部评价。
  get: () => query.rating === '' ? ALL_SELECT_VALUE : String(query.rating),
  set: (value) => {
    query.rating = value === ALL_SELECT_VALUE ? '' : value as PromptFeedbackRating
  },
})

/**
 * 格式化统计占比为百分比。
 */
function formatRatio(value: number | string) {
  const ratio = Number(value ?? 0)
  return `${(ratio * 100).toFixed(1)}%`
}

/**
 * 格式化时间显示。
 */
function formatTime(value?: string) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}

/**
 * 根据评价结果映射标签样式。
 */
function getRatingVariant(rating?: PromptFeedbackRating) {
  if (rating === 'VERY_SATISFIED') {
    return 'default'
  }
  if (rating === 'UNSATISFIED') {
    return 'destructive'
  }
  if (rating === 'NEUTRAL') {
    return 'outline'
  }
  return 'secondary'
}

/**
 * 触发查询并回到第一页。
 */
function handleSearch() {
  query.page = 1
  refetchFeedback()
  refetchStats()
}

/**
 * 重置查询条件。
 */
function handleReset() {
  query.page = 1
  query.taskId = ''
  query.feedbackStage = ''
  query.rating = ''
  query.templateKey = ''
  query.version = ''
  query.startTime = ''
  query.endTime = ''
  refetchFeedback()
  refetchStats()
}
</script>

<template>
  <BasicPage title="Prompt 反馈" description="查看用户对标题、大纲和正文融合 Prompt 的效果反馈。" sticky>
    <template #actions>
      <UiButton variant="outline" :disabled="isFetching" @click="() => { refetchFeedback(); refetchStats() }">
        <RefreshCwIcon class="mr-1 size-4" :class="{ 'animate-spin': isFetching }" />
        刷新
      </UiButton>
    </template>

    <div class="space-y-4">
      <div class="grid gap-4 md:grid-cols-3">
        <UiCard v-for="item in stats" :key="item.feedbackStage" class="border-border/70">
          <UiCardHeader class="pb-3">
            <UiCardTitle class="flex items-center gap-2 text-base">
              <MessageSquareTextIcon class="size-4 text-primary" />
              {{ item.feedbackStageLabel || item.feedbackStage }}
            </UiCardTitle>
            <UiCardDescription>
              已提交反馈 {{ item.totalCount }} 条
            </UiCardDescription>
          </UiCardHeader>
          <UiCardContent class="space-y-3">
            <div v-for="option in ratingOptions" :key="option.value" class="space-y-1.5">
              <div class="flex items-center justify-between gap-3">
                <span class="flex items-center gap-2 text-sm text-muted-foreground">
                  <component :is="option.icon" class="size-4" :class="option.iconClass" />
                  {{ option.label }}
                </span>
                <span class="font-semibold">{{ item[option.countKey] }} / {{ formatRatio(item[option.ratioKey]) }}</span>
              </div>
              <UiProgress :model-value="Number(item[option.ratioKey]) * 100" />
            </div>
          </UiCardContent>
        </UiCard>
      </div>

      <UiCard class="border-border/70">
        <UiCardHeader class="border-b bg-muted/30">
          <UiCardTitle class="text-base">
            筛选反馈
          </UiCardTitle>
          <UiCardDescription>按环节、评价、任务、模板版本和时间范围定位反馈记录。</UiCardDescription>
        </UiCardHeader>
        <UiCardContent class="grid gap-3 pt-5 lg:grid-cols-[1fr_0.8fr_0.8fr_1fr_0.7fr_0.9fr_0.9fr_auto]">
          <UiInput v-model="query.taskId" placeholder="任务 ID" />
          <UiSelect v-model="stageFilter">
            <UiSelectTrigger>
              <UiSelectValue placeholder="全部环节" />
            </UiSelectTrigger>
            <UiSelectContent>
              <UiSelectItem :value="ALL_SELECT_VALUE">
                全部环节
              </UiSelectItem>
              <UiSelectItem value="TITLE_SELECTION">
                标题生成
              </UiSelectItem>
              <UiSelectItem value="OUTLINE_EDITING">
                大纲生成
              </UiSelectItem>
              <UiSelectItem value="CONTENT_MERGED">
                正文融合
              </UiSelectItem>
            </UiSelectContent>
          </UiSelect>
          <UiSelect v-model="ratingFilter">
            <UiSelectTrigger>
              <UiSelectValue placeholder="全部评价" />
            </UiSelectTrigger>
            <UiSelectContent>
              <UiSelectItem :value="ALL_SELECT_VALUE">
                全部评价
              </UiSelectItem>
              <UiSelectItem
                v-for="option in ratingOptions"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </UiSelectItem>
            </UiSelectContent>
          </UiSelect>
          <UiInput v-model="query.templateKey" placeholder="模板标识" />
          <UiInput v-model="query.version" placeholder="版本" />
          <UiInput v-model="query.startTime" type="datetime-local" />
          <UiInput v-model="query.endTime" type="datetime-local" />
          <div class="flex gap-2">
            <UiButton @click="handleSearch">
              <SearchIcon class="mr-1 size-4" />
              查询
            </UiButton>
            <UiButton variant="outline" @click="handleReset">
              重置
            </UiButton>
          </div>
        </UiCardContent>
      </UiCard>

      <UiCard class="border-border/70">
        <UiCardHeader class="border-b bg-gradient-to-br from-background to-muted/40">
          <UiCardTitle>反馈记录</UiCardTitle>
          <UiCardDescription>当前共 {{ feedbackTotal }} 条记录，未关联日志会单独标记。</UiCardDescription>
        </UiCardHeader>
        <UiCardContent>
          <div v-if="isFetchingFeedback" class="flex items-center justify-center py-12 text-sm text-muted-foreground">
            <LoaderCircleIcon class="mr-2 size-4 animate-spin" />
            正在加载 Prompt 反馈...
          </div>
          <div v-else class="overflow-x-auto rounded-xl border border-border/70">
            <table class="w-full text-sm">
              <thead class="bg-muted/50">
                <tr class="border-b text-left">
                  <th class="px-4 py-3 font-medium">
                    用户/任务
                  </th>
                  <th class="px-4 py-3 font-medium">
                    环节
                  </th>
                  <th class="px-4 py-3 font-medium">
                    评价
                  </th>
                  <th class="px-4 py-3 font-medium">
                    Prompt
                  </th>
                  <th class="px-4 py-3 font-medium">
                    备注
                  </th>
                  <th class="px-4 py-3 font-medium">
                    时间
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in feedbackRecords" :key="item.id" class="border-b last:border-b-0">
                  <td class="px-4 py-3 align-top">
                    <div class="font-medium">
                      用户 {{ item.userId }}
                    </div>
                    <div class="mt-1 line-clamp-1 max-w-[260px] text-xs text-muted-foreground">
                      {{ item.taskId }}
                    </div>
                  </td>
                  <td class="px-4 py-3 align-top">
                    <UiBadge variant="outline">
                      {{ item.feedbackStageLabel || item.feedbackStage }}
                    </UiBadge>
                  </td>
                  <td class="px-4 py-3 align-top">
                    <UiBadge :variant="getRatingVariant(item.rating)">
                      {{ item.ratingLabel || item.rating }}
                    </UiBadge>
                  </td>
                  <td class="px-4 py-3 align-top">
                    <div v-if="item.promptLinked">
                      <div class="font-medium">
                        {{ item.templateKey }}
                      </div>
                      <div class="mt-1 text-xs text-muted-foreground">
                        {{ item.version || '-' }} / {{ item.environment || '-' }}
                      </div>
                    </div>
                    <UiBadge v-else variant="secondary">
                      未关联日志
                    </UiBadge>
                  </td>
                  <td class="max-w-[320px] px-4 py-3 align-top text-muted-foreground">
                    <span class="line-clamp-3">{{ item.remark || '-' }}</span>
                  </td>
                  <td class="px-4 py-3 align-top text-muted-foreground">
                    {{ formatTime(item.createTime) }}
                  </td>
                </tr>
                <tr v-if="feedbackRecords.length === 0">
                  <td colspan="6" class="px-4 py-10 text-center text-muted-foreground">
                    暂无 Prompt 反馈
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="mt-4 flex items-center justify-between text-sm text-muted-foreground">
            <span>第 {{ query.page }} / {{ totalPages }} 页</span>
            <div class="flex gap-2">
              <UiButton variant="outline" size="sm" :disabled="query.page <= 1" @click="query.page--; refetchFeedback()">
                上一页
              </UiButton>
              <UiButton variant="outline" size="sm" :disabled="query.page >= totalPages" @click="query.page++; refetchFeedback()">
                下一页
              </UiButton>
            </div>
          </div>
        </UiCardContent>
      </UiCard>
    </div>
  </BasicPage>
</template>

<route lang="yaml">
meta:
  auth: true
</route>
