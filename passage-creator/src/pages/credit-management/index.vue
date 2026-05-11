<script setup lang="ts">
import { LoaderCircleIcon, PlusIcon, RefreshCwIcon, WalletCardsIcon } from '@lucide/vue'
import { toast } from 'vue-sonner'

import type { CreditAccountQuery, CreditRechargeForm, CreditTransactionQuery } from '@/services/types/credit.type'

import { BasicPage } from '@/components/global-layout'
import { useGetAdminCreditAccountsQuery, useGetAdminCreditTransactionsQuery, useRechargeCreditMutation } from '@/services/api/credit.api'
import { getCreditTransactionStatusLabel, getCreditTransactionTypeLabel } from '@/services/types/credit.type'

const accountQuery = reactive<CreditAccountQuery>({
  page: 1,
  pageSize: 8,
  userId: undefined,
})

const transactionQuery = reactive<CreditTransactionQuery>({
  page: 1,
  pageSize: 10,
  userId: undefined,
  transactionType: '',
})

const form = reactive<CreditRechargeForm>({
  userId: undefined,
  amount: undefined,
  description: '',
})

const { data: accountData, isFetching: isFetchingAccounts, refetch: refetchAccounts } = useGetAdminCreditAccountsQuery(accountQuery)
const { data: transactionData, isFetching: isFetchingTransactions, refetch: refetchTransactions } = useGetAdminCreditTransactionsQuery(transactionQuery)
const { mutateAsync: recharge, isPending } = useRechargeCreditMutation()

const accounts = computed(() => accountData.value?.data?.records ?? [])
const accountTotal = computed(() => accountData.value?.data?.totalRow ?? 0)
const accountTotalPages = computed(() => Math.max(1, Math.ceil(accountTotal.value / accountQuery.pageSize)))
const transactions = computed(() => transactionData.value?.data?.records ?? [])
const transactionTotal = computed(() => transactionData.value?.data?.totalRow ?? 0)
const transactionTotalPages = computed(() => Math.max(1, Math.ceil(transactionTotal.value / transactionQuery.pageSize)))

function formatCredits(value?: number) {
  return Number(value ?? 0).toFixed(4)
}

function formatTime(value?: string) {
  return value ? new Date(value).toLocaleString() : '-'
}

async function submitRecharge() {
  try {
    await recharge({ ...form })
    toast.success('积分已充值')
    form.userId = undefined
    form.amount = undefined
    form.description = ''
    refetchAccounts()
    refetchTransactions()
  }
  catch (error: any) {
    toast.error(error?.data?.message ?? error?.message ?? '充值失败')
  }
}

function searchAccounts() {
  accountQuery.page = 1
  refetchAccounts()
}

function searchTransactions() {
  transactionQuery.page = 1
  refetchTransactions()
}

function refreshAll() {
  refetchAccounts()
  refetchTransactions()
}
</script>

<template>
  <BasicPage title="积分管理" description="管理员手动充值积分，并查看全站积分流水。" sticky>
    <template #actions>
      <UiButton variant="outline" :disabled="isFetchingAccounts || isFetchingTransactions" @click="refreshAll">
        <RefreshCwIcon class="mr-1 size-4" :class="{ 'animate-spin': isFetchingAccounts || isFetchingTransactions }" />
        刷新
      </UiButton>
    </template>

    <div class="grid gap-4 xl:grid-cols-[360px_minmax(0,1fr)]">
      <UiCard class="border-border/70">
        <UiCardHeader class="border-b bg-muted/30">
          <UiCardTitle class="flex items-center gap-2 text-base">
            <PlusIcon class="size-4" />手动充值
          </UiCardTitle>
          <UiCardDescription>第一版充值只做后台入账，不创建第三方支付订单。</UiCardDescription>
        </UiCardHeader>
        <UiCardContent class="space-y-4 pt-5">
          <div class="space-y-2">
            <UiLabel>用户 ID</UiLabel>
            <UiInput v-model="form.userId" inputmode="numeric" placeholder="输入用户 ID" />
          </div>
          <div class="space-y-2">
            <UiLabel>充值积分</UiLabel>
            <UiInput v-model.number="form.amount" type="number" min="0" step="0.0001" placeholder="例如 100" />
          </div>
          <div class="space-y-2">
            <UiLabel>备注</UiLabel>
            <UiTextarea v-model="form.description" placeholder="充值原因、线下付款记录或管理员说明" />
          </div>
          <UiButton class="w-full" :disabled="isPending || !form.userId || !form.amount" @click="submitRecharge">
            确认充值
          </UiButton>
        </UiCardContent>
      </UiCard>

      <UiCard class="border-border/70">
        <UiCardHeader class="border-b bg-muted/30">
          <UiCardTitle class="flex items-center gap-2 text-base">
            <WalletCardsIcon class="size-4" />用户余额
          </UiCardTitle>
        </UiCardHeader>
        <UiCardContent>
          <div class="grid gap-3 py-5 md:grid-cols-[220px_auto]">
            <UiInput v-model="accountQuery.userId" inputmode="numeric" placeholder="按用户 ID 查询余额" />
            <div class="flex gap-2">
              <UiButton @click="searchAccounts">
                查询
              </UiButton>
              <UiButton variant="outline" @click="accountQuery.userId = undefined; searchAccounts()">
                重置
              </UiButton>
            </div>
          </div>

          <div v-if="isFetchingAccounts" class="flex items-center justify-center py-12 text-sm text-muted-foreground">
            <LoaderCircleIcon class="mr-2 size-4 animate-spin" />正在加载用户余额...
          </div>
          <div v-else class="overflow-x-auto rounded-md border">
            <table class="w-full text-sm">
              <thead class="bg-muted/50 text-left">
                <tr class="border-b">
                  <th class="px-4 py-3 font-medium">
                    用户
                  </th>
                  <th class="px-4 py-3 font-medium">
                    当前余额
                  </th>
                  <th class="px-4 py-3 font-medium">
                    累计充值
                  </th>
                  <th class="px-4 py-3 font-medium">
                    累计使用
                  </th>
                  <th class="px-4 py-3 font-medium">
                    更新时间
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in accounts" :key="item.id" class="border-b last:border-b-0">
                  <td class="px-4 py-3">
                    {{ item.userId }}
                  </td>
                  <td class="px-4 py-3 font-medium">
                    {{ formatCredits(item.balance) }}
                  </td>
                  <td class="px-4 py-3 text-muted-foreground">
                    {{ formatCredits(item.totalRecharge) }}
                  </td>
                  <td class="px-4 py-3 text-muted-foreground">
                    {{ formatCredits(item.totalConsume) }}
                  </td>
                  <td class="px-4 py-3 text-muted-foreground">
                    {{ formatTime(item.updateTime) }}
                  </td>
                </tr>
                <tr v-if="accounts.length === 0">
                  <td colspan="5" class="px-4 py-10 text-center text-muted-foreground">
                    暂无用户余额
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="mt-4 flex items-center justify-between text-sm text-muted-foreground">
            <span>第 {{ accountQuery.page }} / {{ accountTotalPages }} 页</span>
            <div class="flex gap-2">
              <UiButton variant="outline" size="sm" :disabled="accountQuery.page <= 1" @click="accountQuery.page--; refetchAccounts()">
                上一页
              </UiButton>
              <UiButton variant="outline" size="sm" :disabled="accountQuery.page >= accountTotalPages" @click="accountQuery.page++; refetchAccounts()">
                下一页
              </UiButton>
            </div>
          </div>
        </UiCardContent>
      </UiCard>

      <UiCard class="border-border/70 xl:col-span-2">
        <UiCardHeader class="border-b bg-muted/30">
          <UiCardTitle class="text-base">
            积分流水
          </UiCardTitle>
        </UiCardHeader>
        <UiCardContent>
          <div class="grid gap-3 py-5 md:grid-cols-[160px_180px_auto]">
            <UiInput v-model="transactionQuery.userId" inputmode="numeric" placeholder="用户 ID" />
            <UiInput v-model="transactionQuery.transactionType" placeholder="流水类型" />
            <div class="flex gap-2">
              <UiButton @click="searchTransactions">
                查询
              </UiButton>
              <UiButton variant="outline" @click="transactionQuery.userId = undefined; transactionQuery.transactionType = ''; searchTransactions()">
                重置
              </UiButton>
            </div>
          </div>

          <div v-if="isFetchingTransactions" class="flex items-center justify-center py-12 text-sm text-muted-foreground">
            <LoaderCircleIcon class="mr-2 size-4 animate-spin" />正在加载积分流水...
          </div>
          <div v-else class="overflow-x-auto rounded-md border">
            <table class="w-full text-sm">
              <thead class="bg-muted/50 text-left">
                <tr class="border-b">
                  <th class="px-4 py-3 font-medium">
                    用户
                  </th>
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
                    {{ item.userId }}
                  </td>
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
                  <td class="max-w-[320px] px-4 py-3 text-muted-foreground">
                    <span class="line-clamp-1">{{ item.description || '-' }}</span>
                  </td>
                  <td class="px-4 py-3 text-muted-foreground">
                    {{ formatTime(item.createTime) }}
                  </td>
                </tr>
                <tr v-if="transactions.length === 0">
                  <td colspan="7" class="px-4 py-10 text-center text-muted-foreground">
                    暂无积分流水
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="mt-4 flex items-center justify-between text-sm text-muted-foreground">
            <span>第 {{ transactionQuery.page }} / {{ transactionTotalPages }} 页</span>
            <div class="flex gap-2">
              <UiButton variant="outline" size="sm" :disabled="transactionQuery.page <= 1" @click="transactionQuery.page--; refetchTransactions()">
                上一页
              </UiButton>
              <UiButton variant="outline" size="sm" :disabled="transactionQuery.page >= transactionTotalPages" @click="transactionQuery.page++; refetchTransactions()">
                下一页
              </UiButton>
            </div>
          </div>
        </UiCardContent>
      </UiCard>
    </div>
  </BasicPage>
</template>
