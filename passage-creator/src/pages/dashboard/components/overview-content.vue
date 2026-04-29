<script lang="ts" setup>
import type { DashboardStatistics } from '@/services/types/dashboard.type'

import OverviewChart from './overview-chart.vue'
import RecentSales from './recent-sales.vue'

const props = defineProps<{
  statistics?: DashboardStatistics
  loading: boolean
  fetching: boolean
  error: Error | null
}>()

const emit = defineEmits<{
  retry: []
}>()

const summaryCards = computed(() => {
  const summary = props.statistics?.summary
  return [
    {
      title: '用户总数',
      value: formatNumber(summary?.userTotalCount),
      helper: '当前未删除用户',
      icon: 'users',
    },
    {
      title: '今日新增',
      value: formatNumber(summary?.todayNewUserCount),
      helper: '按服务器自然日统计',
      icon: 'spark',
    },
    {
      title: '通知数量',
      value: formatNumber(summary?.notificationCount),
      helper: '管理员可见已发布通知',
      icon: 'bell',
    },
    {
      title: '操作日志数',
      value: formatNumber(summary?.operationLogCount),
      helper: '管理员操作审计记录',
      icon: 'activity',
    },
  ]
})

const sampleTimeText = computed(() => {
  if (!props.statistics?.sampleTime) {
    return '暂无采样时间'
  }
  return new Date(props.statistics.sampleTime).toLocaleString()
})

function formatNumber(value?: number) {
  if (props.loading) {
    return '...'
  }
  return Intl.NumberFormat('zh-CN').format(value ?? 0)
}
</script>

<template>
  <div
    v-if="error"
    class="flex flex-col gap-3 rounded-md border border-destructive/30 bg-destructive/5 p-4 sm:flex-row sm:items-center sm:justify-between"
  >
    <div>
      <p class="text-sm font-medium text-destructive">
        Dashboard 统计加载失败
      </p>
      <p class="mt-1 text-sm text-muted-foreground">
        {{ error.message || '请稍后重试。' }}
      </p>
    </div>
    <UiButton variant="outline" size="sm" @click="emit('retry')">
      重新加载
    </UiButton>
  </div>

  <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
    <UiCard v-for="card in summaryCards" :key="card.title">
      <UiCardHeader class="flex flex-row items-center justify-between pb-2 space-y-0">
        <UiCardTitle class="text-sm font-medium">
          {{ card.title }}
        </UiCardTitle>
        <svg
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="2"
          class="size-4 text-muted-foreground"
        >
          <template v-if="card.icon === 'users'">
            <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
            <circle cx="9" cy="7" r="4" />
            <path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
          </template>
          <template v-else-if="card.icon === 'spark'">
            <path d="M12 3v18M3 12h18" />
            <path d="m5 5 14 14M19 5 5 19" />
          </template>
          <template v-else-if="card.icon === 'bell'">
            <path d="M10.268 21a2 2 0 0 0 3.464 0" />
            <path d="M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8a6 6 0 0 0-12 0c0 4.499-1.411 5.956-2.738 7.326" />
          </template>
          <template v-else>
            <path d="M3 3v18h18" />
            <path d="m19 9-5 5-4-4-3 3" />
          </template>
        </svg>
      </UiCardHeader>
      <UiCardContent>
        <div class="text-2xl font-bold">
          {{ card.value }}
        </div>
        <p class="text-xs text-muted-foreground">
          {{ card.helper }}
        </p>
      </UiCardContent>
    </UiCard>
  </div>

  <div class="grid grid-cols-1 items-start gap-4 lg:grid-cols-7">
    <OverviewChart
      class="col-span-1 lg:col-span-4"
      :data="statistics?.loginTrend ?? []"
      :loading="loading"
    />
    <UiCard class="col-span-1 lg:col-span-3">
      <UiCardHeader>
        <UiCardTitle>最近操作日志</UiCardTitle>
        <UiCardDescription>
          采样时间：{{ sampleTimeText }}{{ fetching && !loading ? '，正在刷新' : '' }}
        </UiCardDescription>
      </UiCardHeader>
      <UiCardContent>
        <RecentSales
          :list="statistics?.recentOperations ?? []"
          :loading="loading"
        />
      </UiCardContent>
    </UiCard>
  </div>
</template>
