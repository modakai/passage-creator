<script setup lang="ts">
import { CreditCardIcon, LoaderCircleIcon, RefreshCwIcon, WalletCardsIcon } from '@lucide/vue'

import type { CreditTransactionQuery } from '@/services/types/credit.type'

import { BasicPage } from '@/components/global-layout'
import { useGetCreditSummaryQuery, useGetMyCreditTransactionsQuery } from '@/services/api/credit.api'
import { getCreditTransactionStatusLabel, getCreditTransactionTypeLabel } from '@/services/types/credit.type'

/**
 * 当前用户积分流水查询条件。
 */
const query = reactive<CreditTransactionQuery>({
  page: 1,
  pageSize: 10,
})

const { data: summaryData, isFetching: isFetchingSummary, refetch: refetchSummary } = useGetCreditSummaryQuery()
const { data: transactionData, isFetching: isFetchingTransactions, refetch: refetchTransactions } = useGetMyCreditTransactionsQuery(query)

const summary = computed(() => summaryData.value?.data)
const transactions = computed(() => transactionData.value?.data?.records ?? [])
const total = computed(() => transactionData.value?.data?.totalRow ?? 0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / query.pageSize)))

/**
 * 统一积分金额展示，避免空值和过长小数影响表格扫描。
 */
function formatCredits(value?: number) {
  return Number(value ?? 0).toFixed(4)
}

/**
 * 格式化流水时间。
 */
function formatTime(value?: string) {
  return value ? new Date(value).toLocaleString() : '-'
}

/**
 * 刷新用户积分账户和流水。
 */
function refreshAll() {
  refetchSummary()
  refetchTransactions()
}
</script>

<template>
  <BasicPage title="积分中心" description="查看当前账户积分余额、累计使用和积分明细。" sticky>
    <template #actions>
      <UiButton variant="outline" :disabled="isFetchingSummary || isFetchingTransactions" @click="refreshAll">
        <RefreshCwIcon class="mr-1 size-4" :class="{ 'animate-spin': isFetchingSummary || isFetchingTransactions }" />
        刷新
      </UiButton>
    </template>

    <div class="grid gap-4 md:grid-cols-3">
      <UiCard class="border-border/70">
        <UiCardHeader class="pb-2">
          <UiCardDescription>当前余额</UiCardDescription>
          <UiCardTitle class="flex items-end gap-2 text-3xl">
            {{ formatCredits(summary?.balance) }}
            <span class="pb-1 text-sm font-normal text-muted-foreground">积分</span>
          </UiCardTitle>
        </UiCardHeader>
      </UiCard>
      <UiCard class="border-border/70">
        <UiCardHeader class="pb-2">
          <UiCardDescription>累计充值</UiCardDescription>
          <UiCardTitle class="text-2xl">
            {{ formatCredits(summary?.totalRecharge) }}
          </UiCardTitle>
        </UiCardHeader>
      </UiCard>
      <UiCard class="border-border/70">
        <UiCardHeader class="pb-2">
          <UiCardDescription>累计使用</UiCardDescription>
          <UiCardTitle class="text-2xl">
            {{ formatCredits(summary?.totalConsume) }}
          </UiCardTitle>
        </UiCardHeader>
      </UiCard>
    </div>

    <UiAlert class="mt-4 border-primary/30 bg-primary/5">
      <WalletCardsIcon class="size-4" />
      <UiAlertTitle>充值方式</UiAlertTitle>
      <UiAlertDescription>第一版由管理员在后台手动充值，充值到账后会显示在积分流水中。</UiAlertDescription>
    </UiAlert>

    <UiCard class="mt-4 border-border/70">
      <UiCardHeader class="border-b bg-muted/30">
        <UiCardTitle class="flex items-center gap-2 text-base">
          <CreditCardIcon class="size-4" />
          积分明细
        </UiCardTitle>
        <UiCardDescription>消费、充值、预扣和退款都会在这里留痕。</UiCardDescription>
      </UiCardHeader>
      <UiCardContent>
        <div v-if="isFetchingTransactions" class="flex items-center justify-center py-12 text-sm text-muted-foreground">
          <LoaderCircleIcon class="mr-2 size-4 animate-spin" />
          正在加载积分明细...
        </div>
        <div v-else class="overflow-x-auto rounded-md border">
          <table class="w-full text-sm">
            <thead class="bg-muted/50 text-left">
              <tr class="border-b">
                <th class="px-4 py-3 font-medium">
                  类型
                </th>
                <th class="px-4 py-3 font-medium">
                  状态
                </th>
                <th class="px-4 py-3 font-medium">
                  积分
                </th>
                <th class="px-4 py-3 font-medium">
                  余额
                </th>
                <th class="px-4 py-3 font-medium">
                  说明
                </th>
                <th class="px-4 py-3 font-medium">
                  时间
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in transactions" :key="item.id" class="border-b last:border-b-0">
                <td class="px-4 py-3">
                  {{ getCreditTransactionTypeLabel(item.transactionType) }}
                </td>
                <td class="px-4 py-3">
                  {{ getCreditTransactionStatusLabel(item.status) }}
                </td>
                <td class="px-4 py-3 font-medium">
                  {{ formatCredits(item.amount) }}
                </td>
                <td class="px-4 py-3 text-muted-foreground">
                  {{ formatCredits(item.balanceAfter) }}
                </td>
                <td class="max-w-[360px] px-4 py-3 text-muted-foreground">
                  <span class="line-clamp-1">{{ item.description || item.bizType || '-' }}</span>
                </td>
                <td class="px-4 py-3 text-muted-foreground">
                  {{ formatTime(item.createTime) }}
                </td>
              </tr>
              <tr v-if="transactions.length === 0">
                <td colspan="6" class="px-4 py-10 text-center text-muted-foreground">
                  暂无积分明细
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="mt-4 flex items-center justify-between text-sm text-muted-foreground">
          <span>第 {{ query.page }} / {{ totalPages }} 页</span>
          <div class="flex gap-2">
            <UiButton variant="outline" size="sm" :disabled="query.page <= 1" @click="query.page--; refetchTransactions()">
              上一页
            </UiButton>
            <UiButton variant="outline" size="sm" :disabled="query.page >= totalPages" @click="query.page++; refetchTransactions()">
              下一页
            </UiButton>
          </div>
        </div>
      </UiCardContent>
    </UiCard>
  </BasicPage>
</template>

<route lang="yaml">
meta:
  layout: user
  auth: true
  section: user
</route>
