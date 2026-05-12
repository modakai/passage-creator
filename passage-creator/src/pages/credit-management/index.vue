<script setup lang="ts">
import { CheckIcon, LoaderCircleIcon, PlusIcon, RefreshCwIcon, SearchIcon, WalletCardsIcon, XIcon } from '@lucide/vue'
import { toast } from 'vue-sonner'

import type { CreditAccountQuery, CreditRechargeForm, CreditTransactionQuery, ManualRechargeApplication, ManualRechargeQuery } from '@/services/types/credit.type'

import { BasicPage } from '@/components/global-layout'
import { useApproveManualRechargeApplicationMutation, useGetAdminCreditAccountsQuery, useGetAdminCreditTransactionsQuery, useGetAdminManualRechargeApplicationsQuery, useRechargeCreditMutation, useRejectManualRechargeApplicationMutation } from '@/services/api/credit.api'
import { getCreditTransactionStatusLabel, getCreditTransactionTypeLabel, getManualRechargePayMethodLabel, getManualRechargeStatusLabel } from '@/services/types/credit.type'

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

/**
 * 管理端人工充值申请查询条件。
 */
const applicationQuery = reactive<ManualRechargeQuery>({
  page: 1,
  pageSize: 10,
  userId: undefined,
  status: '',
  rechargeNo: '',
})

const applicationStatusFilter = ref('all')
const rejectingApplication = ref<ManualRechargeApplication | null>(null)
const rejectReason = ref('')

const form = reactive<CreditRechargeForm>({
  userId: undefined,
  amount: undefined,
  description: '',
})

const { data: accountData, isFetching: isFetchingAccounts, refetch: refetchAccounts } = useGetAdminCreditAccountsQuery(accountQuery)
const { data: transactionData, isFetching: isFetchingTransactions, refetch: refetchTransactions } = useGetAdminCreditTransactionsQuery(transactionQuery)
const { data: applicationData, isFetching: isFetchingApplications, refetch: refetchApplications } = useGetAdminManualRechargeApplicationsQuery(applicationQuery)
const { mutateAsync: recharge, isPending } = useRechargeCreditMutation()
const { mutateAsync: approveApplication, isPending: isApprovingApplication } = useApproveManualRechargeApplicationMutation()
const { mutateAsync: rejectApplication, isPending: isRejectingApplication } = useRejectManualRechargeApplicationMutation()

const accounts = computed(() => accountData.value?.data?.records ?? [])
const accountTotal = computed(() => accountData.value?.data?.totalRow ?? 0)
const accountTotalPages = computed(() => Math.max(1, Math.ceil(accountTotal.value / accountQuery.pageSize)))
const transactions = computed(() => transactionData.value?.data?.records ?? [])
const transactionTotal = computed(() => transactionData.value?.data?.totalRow ?? 0)
const transactionTotalPages = computed(() => Math.max(1, Math.ceil(transactionTotal.value / transactionQuery.pageSize)))
const applications = computed(() => applicationData.value?.data?.records ?? [])
const applicationTotal = computed(() => applicationData.value?.data?.totalRow ?? 0)
const applicationTotalPages = computed(() => Math.max(1, Math.ceil(applicationTotal.value / applicationQuery.pageSize)))

function formatCredits(value?: number) {
  return Number(value ?? 0).toFixed(4)
}

function formatMoney(value?: number) {
  return Number(value ?? 0).toFixed(2)
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

/**
 * 根据筛选条件查询人工充值申请。
 */
function searchApplications() {
  applicationQuery.page = 1
  applicationQuery.status = applicationStatusFilter.value === 'all' ? '' : applicationStatusFilter.value
  refetchApplications()
}

function resetApplications() {
  applicationQuery.userId = undefined
  applicationQuery.rechargeNo = ''
  applicationStatusFilter.value = 'all'
  searchApplications()
}

function refreshAll() {
  refetchAccounts()
  refetchTransactions()
  refetchApplications()
}

/**
 * 审核通过人工充值申请，最终入账幂等性由后端事务保证。
 */
async function handleApproveApplication(item: ManualRechargeApplication) {
  try {
    await approveApplication({ id: item.id, adminRemark: '收款已人工核对' })
    toast.success('申请已通过，积分已入账')
    refreshAll()
  }
  catch (error: any) {
    toast.error(error?.data?.message ?? error?.message ?? '审核通过失败')
  }
}

function openRejectDialog(item: ManualRechargeApplication) {
  rejectingApplication.value = item
  rejectReason.value = item.adminRemark ?? ''
}

/**
 * 拒绝人工充值申请，必须写入管理员原因。
 */
async function handleRejectApplication() {
  if (!rejectingApplication.value) {
    return
  }
  if (!rejectReason.value.trim()) {
    toast.error('请填写拒绝原因')
    return
  }
  try {
    await rejectApplication({ id: rejectingApplication.value.id, adminRemark: rejectReason.value.trim() })
    toast.success('申请已拒绝')
    rejectingApplication.value = null
    rejectReason.value = ''
    refetchApplications()
  }
  catch (error: any) {
    toast.error(error?.data?.message ?? error?.message ?? '拒绝申请失败')
  }
}
</script>

<template>
  <BasicPage title="积分管理" description="管理员手动充值积分，并查看全站积分流水。" sticky>
    <template #actions>
      <UiButton variant="outline" :disabled="isFetchingAccounts || isFetchingTransactions || isFetchingApplications" @click="refreshAll">
        <RefreshCwIcon class="mr-1 size-4" :class="{ 'animate-spin': isFetchingAccounts || isFetchingTransactions || isFetchingApplications }" />
        刷新
      </UiButton>
    </template>

    <div class="grid gap-4 xl:grid-cols-[360px_minmax(0,1fr)]">
      <UiCard class="border-border/70">
        <UiCardHeader class="border-b bg-muted/30">
          <UiCardTitle class="flex items-center gap-2 text-base">
            <PlusIcon class="size-4" />手动充值
          </UiCardTitle>
          <UiCardDescription>运营补账入口；用户扫码申请请在下方人工充值申请中审核。</UiCardDescription>
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
            人工充值申请
          </UiCardTitle>
          <UiCardDescription>核对用户付款备注中的充值申请号后，再执行通过或拒绝。</UiCardDescription>
        </UiCardHeader>
        <UiCardContent>
          <div class="grid gap-3 py-5 lg:grid-cols-[160px_220px_180px_auto]">
            <UiInput v-model="applicationQuery.userId" inputmode="numeric" placeholder="用户 ID" />
            <UiInput v-model="applicationQuery.rechargeNo" placeholder="充值申请号" />
            <UiSelect v-model="applicationStatusFilter">
              <UiSelectTrigger>
                <UiSelectValue placeholder="申请状态" />
              </UiSelectTrigger>
              <UiSelectContent>
                <UiSelectItem value="all">
                  全部状态
                </UiSelectItem>
                <UiSelectItem value="PENDING">
                  待审核
                </UiSelectItem>
                <UiSelectItem value="APPROVED">
                  已到账
                </UiSelectItem>
                <UiSelectItem value="REJECTED">
                  已拒绝
                </UiSelectItem>
              </UiSelectContent>
            </UiSelect>
            <div class="flex gap-2">
              <UiButton @click="searchApplications">
                <SearchIcon class="mr-1 size-4" />
                查询
              </UiButton>
              <UiButton variant="outline" @click="resetApplications">
                重置
              </UiButton>
            </div>
          </div>

          <div v-if="isFetchingApplications" class="flex items-center justify-center py-12 text-sm text-muted-foreground">
            <LoaderCircleIcon class="mr-2 size-4 animate-spin" />正在加载充值申请...
          </div>
          <div v-else class="overflow-x-auto rounded-md border">
            <table class="w-full text-sm">
              <thead class="bg-muted/50 text-left">
                <tr class="border-b">
                  <th class="px-4 py-3 font-medium">
                    用户
                  </th>
                  <th class="px-4 py-3 font-medium">
                    申请号
                  </th>
                  <th class="px-4 py-3 font-medium">
                    金额
                  </th>
                  <th class="px-4 py-3 font-medium">
                    积分
                  </th>
                  <th class="px-4 py-3 font-medium">
                    方式
                  </th>
                  <th class="px-4 py-3 font-medium">
                    状态
                  </th>
                  <th class="px-4 py-3 font-medium">
                    备注
                  </th>
                  <th class="px-4 py-3 font-medium">
                    时间
                  </th>
                  <th class="px-4 py-3 font-medium">
                    操作
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in applications" :key="item.id" class="border-b last:border-b-0">
                  <td class="px-4 py-3">
                    {{ item.userId }}
                  </td>
                  <td class="px-4 py-3 font-medium">
                    {{ item.rechargeNo }}
                  </td>
                  <td class="px-4 py-3">
                    ¥{{ formatMoney(item.amount) }}
                  </td>
                  <td class="px-4 py-3">
                    {{ formatCredits(item.credits) }}
                  </td>
                  <td class="px-4 py-3 text-muted-foreground">
                    {{ getManualRechargePayMethodLabel(item.payMethod) }}
                  </td>
                  <td class="px-4 py-3">
                    <UiBadge :variant="item.status === 'REJECTED' ? 'destructive' : item.status === 'APPROVED' ? 'default' : 'secondary'">
                      {{ getManualRechargeStatusLabel(item.status) }}
                    </UiBadge>
                  </td>
                  <td class="max-w-[340px] px-4 py-3 text-muted-foreground">
                    <span class="line-clamp-1">{{ item.adminRemark || item.userRemark || '-' }}</span>
                  </td>
                  <td class="px-4 py-3 text-muted-foreground">
                    {{ formatTime(item.createTime) }}
                  </td>
                  <td class="px-4 py-3">
                    <div v-if="item.status === 'PENDING'" class="flex gap-2">
                      <UiButton size="sm" :disabled="isApprovingApplication" @click="handleApproveApplication(item)">
                        <CheckIcon class="mr-1 size-4" />
                        通过
                      </UiButton>
                      <UiButton size="sm" variant="outline" :disabled="isRejectingApplication" @click="openRejectDialog(item)">
                        <XIcon class="mr-1 size-4" />
                        拒绝
                      </UiButton>
                    </div>
                    <span v-else class="text-muted-foreground">已处理</span>
                  </td>
                </tr>
                <tr v-if="applications.length === 0">
                  <td colspan="9" class="px-4 py-10 text-center text-muted-foreground">
                    暂无人工充值申请
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="mt-4 flex items-center justify-between text-sm text-muted-foreground">
            <span>第 {{ applicationQuery.page }} / {{ applicationTotalPages }} 页</span>
            <div class="flex gap-2">
              <UiButton variant="outline" size="sm" :disabled="applicationQuery.page <= 1" @click="applicationQuery.page--; refetchApplications()">
                上一页
              </UiButton>
              <UiButton variant="outline" size="sm" :disabled="applicationQuery.page >= applicationTotalPages" @click="applicationQuery.page++; refetchApplications()">
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

    <UiDialog :open="rejectingApplication !== null" @update:open="value => !value ? rejectingApplication = null : undefined">
      <UiDialogContent>
        <UiDialogHeader>
          <UiDialogTitle>拒绝充值申请</UiDialogTitle>
          <UiDialogDescription>
            {{ rejectingApplication?.rechargeNo }} 将标记为已拒绝，拒绝原因会展示给用户。
          </UiDialogDescription>
        </UiDialogHeader>
        <div class="space-y-2 py-3">
          <UiLabel>拒绝原因</UiLabel>
          <UiTextarea v-model="rejectReason" placeholder="例如：未找到对应收款记录" />
        </div>
        <UiDialogFooter>
          <UiButton variant="outline" @click="rejectingApplication = null">
            取消
          </UiButton>
          <UiButton variant="destructive" :disabled="isRejectingApplication" @click="handleRejectApplication">
            确认拒绝
          </UiButton>
        </UiDialogFooter>
      </UiDialogContent>
    </UiDialog>
  </BasicPage>
</template>
