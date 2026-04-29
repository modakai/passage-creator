<script setup lang="ts">
import { LoaderCircleIcon, RefreshCwIcon, SearchIcon } from '@lucide/vue'
import { VisAxis, VisLine, VisXYContainer } from '@unovis/vue'

import type { ErrorTrendBucket, ObservabilityEventQuery } from '@/services/types/observability.type'

import { BasicPage } from '@/components/global-layout'
import { useGetApiSummaryQuery, useGetErrorTrendQuery, useGetSlowApiPageQuery } from '@/services/api/observability.api'

const query = reactive<ObservabilityEventQuery>({
  page: 1,
  pageSize: 10,
  requestPath: '',
  ipAddress: '',
  startTime: '',
  endTime: '',
})

const { data: summaryData, isFetching: isFetchingSummary, refetch: refetchSummary } = useGetApiSummaryQuery(query)
const { data: trendData, isFetching: isFetchingTrend, refetch: refetchTrend } = useGetErrorTrendQuery(query)
const { data: slowData, isFetching: isFetchingSlow, refetch: refetchSlow } = useGetSlowApiPageQuery(query)

const summary = computed(() => summaryData.value?.data)
const trend = computed(() => trendData.value?.data ?? [])
const slowApis = computed(() => slowData.value?.data?.records ?? [])
const total = computed(() => slowData.value?.data?.totalRow ?? 0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / query.pageSize)))

/**
 * 查询第一页接口监控数据。
 */
function handleSearch() {
  query.page = 1
  refetchAll()
}

/**
 * 刷新全部接口监控数据。
 */
function refetchAll() {
  refetchSummary()
  refetchTrend()
  refetchSlow()
}

/**
 * 切换分页。
 */
function changePage(page: number) {
  query.page = Math.min(Math.max(page, 1), totalPages.value)
  refetchSlow()
}

/**
 * 格式化时间。
 */
function formatTime(value?: string) {
  return value ? new Date(value).toLocaleString() : '-'
}
</script>

<template>
  <BasicPage title="接口监控" description="查看接口耗时、慢接口列表和错误趋势。" sticky>
    <template #actions>
      <UiButton variant="outline" :disabled="isFetchingSummary || isFetchingTrend || isFetchingSlow" @click="refetchAll">
        <RefreshCwIcon class="mr-1 size-4" :class="{ 'animate-spin': isFetchingSummary || isFetchingTrend || isFetchingSlow }" />
        刷新
      </UiButton>
    </template>

    <div class="space-y-4">
      <div class="grid gap-4 md:grid-cols-3">
        <UiCard class="border-border/70">
          <UiCardHeader>
            <UiCardTitle class="text-sm">
              慢接口
            </UiCardTitle>
          </UiCardHeader>
          <UiCardContent>
            <div class="text-2xl font-semibold">
              {{ summary?.slowApiCount ?? 0 }}
            </div>
          </UiCardContent>
        </UiCard>
        <UiCard class="border-border/70">
          <UiCardHeader>
            <UiCardTitle class="text-sm">
              错误事件
            </UiCardTitle>
          </UiCardHeader>
          <UiCardContent>
            <div class="text-2xl font-semibold">
              {{ summary?.errorCount ?? 0 }}
            </div>
          </UiCardContent>
        </UiCard>
        <UiCard class="border-border/70">
          <UiCardHeader>
            <UiCardTitle class="text-sm">
              平均慢接口耗时
            </UiCardTitle>
          </UiCardHeader>
          <UiCardContent>
            <div class="text-2xl font-semibold">
              {{ (summary?.averageSlowDurationMillis ?? 0).toFixed(0) }} ms
            </div>
          </UiCardContent>
        </UiCard>
      </div>

      <UiCard class="border-border/70">
        <UiCardHeader>
          <UiCardTitle>筛选条件</UiCardTitle>
          <UiCardDescription>按路径、IP 和时间范围定位接口质量问题。</UiCardDescription>
        </UiCardHeader>
        <UiCardContent class="grid gap-3 md:grid-cols-5">
          <UiInput v-model="query.requestPath" placeholder="请求路径" />
          <UiInput v-model="query.ipAddress" placeholder="IP 地址" />
          <UiInput v-model="query.startTime" type="datetime-local" />
          <UiInput v-model="query.endTime" type="datetime-local" />
          <UiButton @click="handleSearch">
            <SearchIcon class="mr-1 size-4" />
            查询
          </UiButton>
        </UiCardContent>
      </UiCard>

      <UiCard class="border-border/70">
        <UiCardHeader>
          <UiCardTitle>错误趋势</UiCardTitle>
          <UiCardDescription>按小时聚合 4xx、5xx 和异常数量。</UiCardDescription>
        </UiCardHeader>
        <UiCardContent>
          <div v-if="isFetchingTrend" class="flex items-center justify-center py-12 text-sm text-muted-foreground">
            <LoaderCircleIcon class="mr-2 size-4 animate-spin" />
            正在加载错误趋势...
          </div>
          <div v-else class="h-[260px]">
            <VisXYContainer :data="trend" :margin="{ left: -20 }">
              <VisLine :x="(d: ErrorTrendBucket) => d.bucket" :y="[(d: ErrorTrendBucket) => d.clientErrorCount, (d: ErrorTrendBucket) => d.serverErrorCount, (d: ErrorTrendBucket) => d.exceptionCount]" />
              <VisAxis type="x" :x="(d: ErrorTrendBucket) => d.bucket" :num-ticks="4" />
              <VisAxis type="y" :num-ticks="4" />
            </VisXYContainer>
          </div>
        </UiCardContent>
      </UiCard>

      <UiCard class="border-border/70">
        <UiCardHeader>
          <UiCardTitle>慢接口列表</UiCardTitle>
          <UiCardDescription>当前共 {{ total }} 条慢接口记录。</UiCardDescription>
        </UiCardHeader>
        <UiCardContent>
          <div class="overflow-x-auto rounded-lg border">
            <table class="w-full text-sm">
              <thead class="bg-muted/50">
                <tr class="border-b text-left">
                  <th class="px-4 py-3">
                    路径
                  </th>
                  <th class="px-4 py-3">
                    方法
                  </th>
                  <th class="px-4 py-3">
                    状态码
                  </th>
                  <th class="px-4 py-3">
                    耗时
                  </th>
                  <th class="px-4 py-3">
                    IP
                  </th>
                  <th class="px-4 py-3">
                    账号
                  </th>
                  <th class="px-4 py-3">
                    时间
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in slowApis" :key="item.id" class="border-b last:border-b-0">
                  <td class="max-w-[360px] truncate px-4 py-3">
                    {{ item.requestPath }}
                  </td>
                  <td class="px-4 py-3">
                    {{ item.httpMethod || '-' }}
                  </td>
                  <td class="px-4 py-3">
                    {{ item.statusCode ?? '-' }}
                  </td>
                  <td class="px-4 py-3 font-medium">
                    {{ item.durationMillis ?? 0 }} ms
                  </td>
                  <td class="px-4 py-3">
                    {{ item.ipAddress || '-' }}
                  </td>
                  <td class="px-4 py-3">
                    {{ item.accountIdentifier || '-' }}
                  </td>
                  <td class="px-4 py-3 text-muted-foreground">
                    {{ formatTime(item.eventTime) }}
                  </td>
                </tr>
                <tr v-if="slowApis.length === 0">
                  <td colspan="7" class="px-4 py-10 text-center text-muted-foreground">
                    暂无慢接口记录
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="mt-4 flex items-center justify-between text-sm text-muted-foreground">
            <span>第 {{ query.page }} / {{ totalPages }} 页</span>
            <div class="flex gap-2">
              <UiButton variant="outline" size="sm" :disabled="query.page <= 1" @click="changePage(query.page - 1)">
                上一页
              </UiButton>
              <UiButton variant="outline" size="sm" :disabled="query.page >= totalPages" @click="changePage(query.page + 1)">
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
