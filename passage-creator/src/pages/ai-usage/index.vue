<script setup lang="ts">
import { BotIcon, ExternalLinkIcon, GaugeIcon, LoaderCircleIcon, RefreshCwIcon, TerminalIcon } from '@lucide/vue'

import type { AiUsageQuery } from '@/services/types/ai-usage.type'

import { BasicPage } from '@/components/global-layout'
import CopyButton from '@/components/prop-ui/copy/Copy.vue'
import { API_BASE_URL } from '@/constants/app-config'
import { useGetAiUsageRecordPageQuery, useGetAiUsageSummaryQuery, useGetAiUsageUserPageQuery } from '@/services/api/ai-usage.api'

/**
 * Prometheus 抓取地址沿用后端 API context-path，避免前端重复配置监控端口。
 */
const prometheusEndpoint = `${API_BASE_URL}/actuator/prometheus`

/**
 * Grafana 面板推荐查询，覆盖成本、Token、调用量和平均耗时四个运维视角。
 */
const grafanaPanels = [
  {
    title: '实时积分成本',
    description: '按供应商和模型观察最近 5 分钟成本燃烧速度。',
    query: 'sum(rate(ai_cost_credits_total[5m])) by (provider, model)',
  },
  {
    title: '阶段成本分布',
    description: '定位哪个创作阶段在最近 1 小时消耗最高。',
    query: 'sum(increase(ai_cost_credits_total[1h])) by (phase)',
  },
  {
    title: 'Token 吞吐',
    description: '按模型查看总 Token 实时吞吐，辅助判断流量和账单波动。',
    query: 'sum(rate(ai_cost_tokens_total{token_type="total"}[5m])) by (provider, model)',
  },
  {
    title: '平均调用耗时',
    description: '用 Timer 的 sum/count 计算最近 5 分钟平均模型延迟。',
    query: 'sum(rate(ai_cost_latency_seconds_sum[5m])) by (provider, model) / sum(rate(ai_cost_latency_seconds_count[5m])) by (provider, model)',
  },
] as const

/**
 * Prometheus 指标维度说明，只展示低基数字段，避免鼓励把用户或任务 ID 放进时序标签。
 */
const metricTags = ['provider', 'model', 'request_type', 'phase', 'agent_name', 'status'] as const

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

    <UiCard class="border-border/70">
      <UiCardHeader class="border-b bg-muted/30">
        <div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <UiCardTitle class="flex items-center gap-2 text-base">
              <GaugeIcon class="size-4" />
              Prometheus / Grafana 实时监控
            </UiCardTitle>
            <UiCardDescription class="mt-1">
              这里给出实时运维指标接入信息；历史明细和用户维度仍由下方账本数据提供。
            </UiCardDescription>
          </div>
          <div class="flex flex-wrap gap-2">
            <UiButton as="a" variant="outline" :href="prometheusEndpoint" target="_blank" rel="noreferrer">
              <ExternalLinkIcon class="mr-1 size-4" />
              Prometheus 指标
            </UiButton>
            <CopyButton :content="prometheusEndpoint" copy-tooltip-text="复制抓取地址" copied-tooltip-text="已复制抓取地址" />
          </div>
        </div>
      </UiCardHeader>
      <UiCardContent class="grid gap-4 pt-5 xl:grid-cols-[minmax(0,1fr)_320px]">
        <div class="grid gap-3 md:grid-cols-2">
          <div v-for="panel in grafanaPanels" :key="panel.title" class="rounded-md border bg-background p-4">
            <div class="flex items-start justify-between gap-3">
              <div>
                <div class="font-medium">
                  {{ panel.title }}
                </div>
                <p class="mt-1 text-xs leading-5 text-muted-foreground">
                  {{ panel.description }}
                </p>
              </div>
              <CopyButton :content="panel.query" size="sm" variant="ghost" copy-tooltip-text="复制 PromQL" copied-tooltip-text="已复制 PromQL" />
            </div>
            <pre class="mt-3 overflow-x-auto rounded-md bg-muted p-3 text-xs leading-5"><code>{{ panel.query }}</code></pre>
          </div>
        </div>
        <div class="rounded-md border bg-background p-4">
          <div class="flex items-center gap-2 font-medium">
            <TerminalIcon class="size-4" />
            指标口径
          </div>
          <div class="mt-3 space-y-3 text-sm text-muted-foreground">
            <div>
              <div class="text-foreground">
                Micrometer 指标
              </div>
              <p class="mt-1 break-all">
                ai_cost_calls_total、ai_cost_credits_total、ai_cost_tokens_total、ai_cost_latency_seconds
              </p>
            </div>
            <div>
              <div class="text-foreground">
                低基数标签
              </div>
              <div class="mt-2 flex flex-wrap gap-2">
                <UiBadge v-for="tag in metricTags" :key="tag" variant="secondary">
                  {{ tag }}
                </UiBadge>
              </div>
            </div>
            <p>
              用户 ID、任务 ID、错误详情只保存在历史明细中，不进入 Prometheus 标签。
            </p>
          </div>
        </div>
      </UiCardContent>
    </UiCard>

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

<route lang="yaml">
meta:
  auth: true
  section: admin
  requiresAdmin: true
</route>
