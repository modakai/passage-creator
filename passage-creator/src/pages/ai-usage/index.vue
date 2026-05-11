<script setup lang="ts">
import { BotIcon, LoaderCircleIcon, RefreshCwIcon } from '@lucide/vue'

import type { AiUsageQuery } from '@/services/types/ai-usage.type'

import { BasicPage } from '@/components/global-layout'
import { useGetAiUsageRecordPageQuery, useGetAiUsageSummaryQuery, useGetAiUsageUserPageQuery } from '@/services/api/ai-usage.api'

/**
 * AI 用量查询条件。
 */
const query = reactive<AiUsageQuery>({
  page: 1,
  pageSize: 10,
  userId: undefined,
  taskId: '',
  agentName: '',
})

const { data: summaryData, isFetching: isFetchingSummary, refetch: refetchSummary } = useGetAiUsageSummaryQuery(query)
const { data: userData, isFetching: isFetchingUsers, refetch: refetchUsers } = useGetAiUsageUserPageQuery(query)
const { data: recordData, isFetching: isFetchingRecords, refetch: refetchRecords } = useGetAiUsageRecordPageQuery(query)

const summary = computed(() => summaryData.value?.data)
const userRows = computed(() => userData.value?.data?.records ?? [])
const records = computed(() => recordData.value?.data?.records ?? [])
const recordTotal = computed(() => recordData.value?.data?.totalRow ?? 0)
const recordTotalPages = computed(() => Math.max(1, Math.ceil(recordTotal.value / query.pageSize)))

function formatNumber(value?: number) {
  return Number(value ?? 0).toLocaleString()
}

function formatCredits(value?: number) {
  return Number(value ?? 0).toFixed(4)
}

function formatTime(value?: string) {
  return value ? new Date(value).toLocaleString() : '-'
}

function search() {
  query.page = 1
  refetchSummary()
  refetchUsers()
  refetchRecords()
}

function reset() {
  query.page = 1
  query.userId = undefined
  query.taskId = ''
  query.agentName = ''
  refetchSummary()
  refetchUsers()
  refetchRecords()
}
</script>

<template>
  <BasicPage title="AI 成本" description="按 Token、模型、阶段和用户维度查看 AI 调用成本。" sticky>
    <template #actions>
      <UiButton variant="outline" :disabled="isFetchingSummary || isFetchingUsers || isFetchingRecords" @click="search">
        <RefreshCwIcon class="mr-1 size-4" :class="{ 'animate-spin': isFetchingSummary || isFetchingUsers || isFetchingRecords }" />
        刷新
      </UiButton>
    </template>

    <div class="grid gap-4 md:grid-cols-4">
      <UiCard class="border-border/70">
        <UiCardHeader><UiCardDescription>调用次数</UiCardDescription><UiCardTitle>{{ formatNumber(summary?.callCount) }}</UiCardTitle></UiCardHeader>
      </UiCard>
      <UiCard class="border-border/70">
        <UiCardHeader><UiCardDescription>总 Token</UiCardDescription><UiCardTitle>{{ formatNumber(summary?.totalTokens) }}</UiCardTitle></UiCardHeader>
      </UiCard>
      <UiCard class="border-border/70">
        <UiCardHeader><UiCardDescription>输出 Token</UiCardDescription><UiCardTitle>{{ formatNumber(summary?.completionTokens) }}</UiCardTitle></UiCardHeader>
      </UiCard>
      <UiCard class="border-border/70">
        <UiCardHeader><UiCardDescription>积分成本</UiCardDescription><UiCardTitle>{{ formatCredits(summary?.creditCost) }}</UiCardTitle></UiCardHeader>
      </UiCard>
    </div>

    <UiCard class="mt-4 border-border/70">
      <UiCardHeader class="border-b bg-muted/30">
        <UiCardTitle class="text-base">
          筛选
        </UiCardTitle>
      </UiCardHeader>
      <UiCardContent class="grid gap-3 pt-5 md:grid-cols-[160px_1fr_1fr_auto]">
        <UiInput v-model="query.userId" inputmode="numeric" placeholder="用户 ID" />
        <UiInput v-model="query.taskId" placeholder="任务 ID" />
        <UiInput v-model="query.agentName" placeholder="Agent 名称" />
        <div class="flex gap-2">
          <UiButton @click="search">
            查询
          </UiButton>
          <UiButton variant="outline" @click="reset">
            重置
          </UiButton>
        </div>
      </UiCardContent>
    </UiCard>

    <div class="mt-4 grid gap-4 xl:grid-cols-[360px_minmax(0,1fr)]">
      <UiCard class="border-border/70">
        <UiCardHeader class="border-b bg-muted/30">
          <UiCardTitle class="flex items-center gap-2 text-base">
            <BotIcon class="size-4" />用户排行
          </UiCardTitle>
        </UiCardHeader>
        <UiCardContent>
          <div v-if="isFetchingUsers" class="flex items-center justify-center py-10 text-sm text-muted-foreground">
            <LoaderCircleIcon class="mr-2 size-4 animate-spin" />加载中...
          </div>
          <div v-else class="divide-y">
            <div v-for="item in userRows" :key="item.userId" class="flex items-center justify-between py-3 text-sm">
              <div>
                <div class="font-medium">
                  用户 {{ item.userId }}
                </div>
                <div class="text-xs text-muted-foreground">
                  {{ formatNumber(item.totalTokens) }} Token
                </div>
              </div>
              <div class="text-right font-medium">
                {{ formatCredits(item.creditCost) }}
              </div>
            </div>
            <div v-if="userRows.length === 0" class="py-10 text-center text-sm text-muted-foreground">
              暂无用户用量
            </div>
          </div>
        </UiCardContent>
      </UiCard>

      <UiCard class="border-border/70">
        <UiCardHeader class="border-b bg-muted/30">
          <UiCardTitle class="text-base">
            调用明细
          </UiCardTitle>
        </UiCardHeader>
        <UiCardContent>
          <div v-if="isFetchingRecords" class="flex items-center justify-center py-12 text-sm text-muted-foreground">
            <LoaderCircleIcon class="mr-2 size-4 animate-spin" />正在加载调用明细...
          </div>
          <div v-else class="overflow-x-auto rounded-md border">
            <table class="w-full text-sm">
              <thead class="bg-muted/50 text-left">
                <tr class="border-b">
                  <th class="px-4 py-3 font-medium">
                    用户
                  </th>
                  <th class="px-4 py-3 font-medium">
                    Agent
                  </th>
                  <th class="px-4 py-3 font-medium">
                    模型
                  </th>
                  <th class="px-4 py-3 font-medium">
                    Token
                  </th>
                  <th class="px-4 py-3 font-medium">
                    成本
                  </th>
                  <th class="px-4 py-3 font-medium">
                    时间
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in records" :key="item.id" class="border-b last:border-b-0">
                  <td class="px-4 py-3">
                    {{ item.userId }}
                  </td>
                  <td class="px-4 py-3">
                    {{ item.agentName }}
                  </td>
                  <td class="px-4 py-3 text-muted-foreground">
                    {{ item.provider }}/{{ item.model }}
                  </td>
                  <td class="px-4 py-3">
                    {{ formatNumber(item.totalTokens) }}
                  </td>
                  <td class="px-4 py-3 font-medium">
                    {{ formatCredits(item.creditCost) }}
                  </td>
                  <td class="px-4 py-3 text-muted-foreground">
                    {{ formatTime(item.usedAt) }}
                  </td>
                </tr>
                <tr v-if="records.length === 0">
                  <td colspan="6" class="px-4 py-10 text-center text-muted-foreground">
                    暂无调用记录
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="mt-4 flex items-center justify-between text-sm text-muted-foreground">
            <span>第 {{ query.page }} / {{ recordTotalPages }} 页</span>
            <div class="flex gap-2">
              <UiButton variant="outline" size="sm" :disabled="query.page <= 1" @click="query.page--; refetchRecords()">
                上一页
              </UiButton>
              <UiButton variant="outline" size="sm" :disabled="query.page >= recordTotalPages" @click="query.page++; refetchRecords()">
                下一页
              </UiButton>
            </div>
          </div>
        </UiCardContent>
      </UiCard>
    </div>
  </BasicPage>
</template>
