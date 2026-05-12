<script setup lang="ts">
import { ClipboardIcon, CreditCardIcon, LoaderCircleIcon, QrCodeIcon, RefreshCwIcon, WalletCardsIcon } from '@lucide/vue'
import { toast } from 'vue-sonner'

import type { CreditTransactionQuery, ManualRechargeApplication, ManualRechargeCreateForm, ManualRechargeQuery } from '@/services/types/credit.type'

import { BasicPage } from '@/components/global-layout'
import { useCreateManualRechargeApplicationMutation, useGetCreditSummaryQuery, useGetManualRechargePackagesQuery, useGetMyCreditTransactionsQuery, useGetMyManualRechargeApplicationsQuery } from '@/services/api/credit.api'
import { getCreditTransactionStatusLabel, getCreditTransactionTypeLabel, getManualRechargePayMethodLabel, getManualRechargeStatusLabel } from '@/services/types/credit.type'

/**
 * 当前用户积分流水查询条件。
 */
const query = reactive<CreditTransactionQuery>({
  page: 1,
  pageSize: 10,
})

/**
 * 当前用户人工充值申请查询条件。
 */
const rechargeQuery = reactive<ManualRechargeQuery>({
  page: 1,
  pageSize: 6,
})

/**
 * 创建人工充值申请表单，只提交套餐和备注，金额积分以后端套餐配置为准。
 */
const rechargeForm = reactive<ManualRechargeCreateForm>({
  packageId: '',
  payMethod: 'UNKNOWN',
  userRemark: '',
})

const latestApplication = ref<ManualRechargeApplication | null>(null)

const { data: summaryData, isFetching: isFetchingSummary, refetch: refetchSummary } = useGetCreditSummaryQuery()
const { data: transactionData, isFetching: isFetchingTransactions, refetch: refetchTransactions } = useGetMyCreditTransactionsQuery(query)
const { data: packageData, isFetching: isFetchingPackages } = useGetManualRechargePackagesQuery()
const { data: applicationData, isFetching: isFetchingApplications, refetch: refetchApplications } = useGetMyManualRechargeApplicationsQuery(rechargeQuery)
const { mutateAsync: createRechargeApplication, isPending: isCreatingRechargeApplication } = useCreateManualRechargeApplicationMutation()

const summary = computed(() => summaryData.value?.data)
const transactions = computed(() => transactionData.value?.data?.records ?? [])
const total = computed(() => transactionData.value?.data?.totalRow ?? 0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / query.pageSize)))
const packages = computed(() => packageData.value?.data ?? [])
const applications = computed(() => applicationData.value?.data?.records ?? [])
const applicationTotal = computed(() => applicationData.value?.data?.totalRow ?? 0)
const applicationTotalPages = computed(() => Math.max(1, Math.ceil(applicationTotal.value / rechargeQuery.pageSize)))
const selectedPackage = computed(() => packages.value.find(item => item.packageId === rechargeForm.packageId))

watch(packages, (items) => {
  if (!rechargeForm.packageId && items.length > 0) {
    rechargeForm.packageId = items[0].packageId
  }
}, { immediate: true })

/**
 * 统一积分金额展示，避免空值和过长小数影响表格扫描。
 */
function formatCredits(value?: number) {
  return Number(value ?? 0).toFixed(4)
}

/**
 * 统一人民币金额展示。
 */
function formatMoney(value?: number) {
  return Number(value ?? 0).toFixed(2)
}

/**
 * 格式化流水时间。
 */
function formatTime(value?: string) {
  return value ? new Date(value).toLocaleString() : '-'
}

/**
 * 刷新用户积分账户、流水和人工充值申请。
 */
function refreshAll() {
  refetchSummary()
  refetchTransactions()
  refetchApplications()
}

/**
 * 创建人工充值申请，成功后展示付款所需信息。
 */
async function submitRechargeApplication() {
  if (!rechargeForm.packageId) {
    toast.error('请选择充值套餐')
    return
  }
  try {
    const response = await createRechargeApplication({ ...rechargeForm })
    latestApplication.value = response.data
    rechargeForm.userRemark = ''
    toast.success('充值申请已创建')
    refetchApplications()
  }
  catch (error: any) {
    toast.error(error?.data?.message ?? error?.message ?? '创建充值申请失败')
  }
}

/**
 * 复制充值申请号，减少用户填写付款备注时输错的概率。
 */
async function copyRechargeNo(rechargeNo?: string) {
  if (!rechargeNo) {
    return
  }
  await navigator.clipboard?.writeText(rechargeNo)
  toast.success('充值申请号已复制')
}
</script>

<template>
  <BasicPage title="积分中心" description="查看当前账户积分余额、累计使用和积分明细。" sticky>
    <template #actions>
      <UiButton variant="outline" :disabled="isFetchingSummary || isFetchingTransactions || isFetchingApplications" @click="refreshAll">
        <RefreshCwIcon class="mr-1 size-4" :class="{ 'animate-spin': isFetchingSummary || isFetchingTransactions || isFetchingApplications }" />
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

    <UiCard class="mt-4 border-border/70">
      <UiCardHeader class="border-b bg-muted/30">
        <UiCardTitle class="flex items-center gap-2 text-base">
          <QrCodeIcon class="size-4" />
          人工扫码充值
        </UiCardTitle>
        <UiCardDescription>该方式为人工审核充值，通常需要管理员确认后到账。</UiCardDescription>
      </UiCardHeader>
      <UiCardContent class="space-y-5 pt-5">
        <div v-if="isFetchingPackages" class="flex items-center justify-center py-8 text-sm text-muted-foreground">
          <LoaderCircleIcon class="mr-2 size-4 animate-spin" />
          正在加载充值套餐...
        </div>
        <template v-else>
          <div class="grid gap-3 md:grid-cols-3">
            <button
              v-for="item in packages"
              :key="item.packageId"
              type="button"
              class="rounded-lg border p-4 text-left transition hover:border-primary/60 hover:bg-primary/5"
              :class="rechargeForm.packageId === item.packageId ? 'border-primary bg-primary/5 ring-1 ring-primary/20' : 'border-border'"
              @click="rechargeForm.packageId = item.packageId"
            >
              <div class="text-sm font-medium">
                {{ item.name }}
              </div>
              <div class="mt-3 flex items-end justify-between gap-3">
                <span class="text-2xl font-semibold">¥{{ formatMoney(item.amount) }}</span>
                <span class="text-sm text-muted-foreground">{{ formatCredits(item.credits) }} 积分</span>
              </div>
            </button>
          </div>

          <div class="grid gap-4 lg:grid-cols-[minmax(0,1fr)_360px]">
            <div class="space-y-4">
              <div class="grid gap-4 md:grid-cols-2">
                <div class="space-y-2">
                  <UiLabel>付款方式</UiLabel>
                  <UiSelect v-model="rechargeForm.payMethod">
                    <UiSelectTrigger>
                      <UiSelectValue placeholder="选择付款方式" />
                    </UiSelectTrigger>
                    <UiSelectContent>
                      <UiSelectItem value="WECHAT">
                        微信
                      </UiSelectItem>
                      <UiSelectItem value="ALIPAY">
                        支付宝
                      </UiSelectItem>
                      <UiSelectItem value="UNKNOWN">
                        暂不确定
                      </UiSelectItem>
                    </UiSelectContent>
                  </UiSelect>
                </div>
                <div class="space-y-2">
                  <UiLabel>用户备注</UiLabel>
                  <UiInput v-model="rechargeForm.userRemark" placeholder="可填写付款账号后四位等信息" />
                </div>
              </div>
              <UiAlert class="border-amber-300 bg-amber-50 text-amber-950 dark:bg-amber-950/20 dark:text-amber-100">
                <WalletCardsIcon class="size-4" />
                <UiAlertTitle>付款备注必须填写充值申请号</UiAlertTitle>
                <UiAlertDescription>创建申请后再扫码付款；付款备注请完整填写页面生成的充值申请号，到账需人工审核，非实时到账。</UiAlertDescription>
              </UiAlert>
              <UiButton :disabled="isCreatingRechargeApplication || !selectedPackage" @click="submitRechargeApplication">
                <LoaderCircleIcon v-if="isCreatingRechargeApplication" class="mr-2 size-4 animate-spin" />
                生成充值申请
              </UiButton>
            </div>

            <div class="rounded-lg border bg-muted/20 p-4">
              <div class="text-sm text-muted-foreground">
                当前选择
              </div>
              <div class="mt-2 text-xl font-semibold">
                {{ selectedPackage ? `¥${formatMoney(selectedPackage.amount)} = ${formatCredits(selectedPackage.credits)} 积分` : '未选择套餐' }}
              </div>
              <div class="mt-4 text-sm text-muted-foreground">
                系统只按后端套餐配置创建申请，前端不会决定最终发放积分。
              </div>
            </div>
          </div>
        </template>

        <div v-if="latestApplication" class="rounded-lg border border-primary/30 bg-primary/5 p-4">
          <div class="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
            <div>
              <div class="text-sm text-muted-foreground">
                充值申请号
              </div>
              <div class="mt-1 flex flex-wrap items-center gap-2 text-2xl font-semibold">
                {{ latestApplication.rechargeNo }}
                <UiButton variant="outline" size="sm" @click="copyRechargeNo(latestApplication.rechargeNo)">
                  <ClipboardIcon class="mr-1 size-4" />
                  复制
                </UiButton>
              </div>
              <p class="mt-3 text-sm font-medium text-amber-700 dark:text-amber-300">
                扫码付款后请在付款备注中填写充值申请号：{{ latestApplication.rechargeNo }}
              </p>
              <p class="mt-1 text-sm text-muted-foreground">
                该方式为人工审核充值，通常需要管理员确认后到账。
              </p>
            </div>
            <UiBadge variant="secondary">
              {{ getManualRechargeStatusLabel(latestApplication.status) }}
            </UiBadge>
          </div>

          <div class="mt-5 grid gap-4 md:grid-cols-2">
            <div class="rounded-md border bg-background p-3">
              <div class="mb-2 text-sm font-medium">
                微信收款码
              </div>
              <img class="aspect-square w-full rounded-md border object-contain" :src="latestApplication.payment?.wechatQrCodeUrl" alt="微信收款码">
            </div>
            <div class="rounded-md border bg-background p-3">
              <div class="mb-2 text-sm font-medium">
                支付宝收款码
              </div>
              <img class="aspect-square w-full rounded-md border object-contain" :src="latestApplication.payment?.alipayQrCodeUrl" alt="支付宝收款码">
            </div>
          </div>
        </div>
      </UiCardContent>
    </UiCard>

    <UiCard class="mt-4 border-border/70">
      <UiCardHeader class="border-b bg-muted/30">
        <UiCardTitle class="text-base">
          充值申请
        </UiCardTitle>
        <UiCardDescription>扫码付款后等待管理员人工核对，状态会在这里更新。</UiCardDescription>
      </UiCardHeader>
      <UiCardContent>
        <div v-if="isFetchingApplications" class="flex items-center justify-center py-10 text-sm text-muted-foreground">
          <LoaderCircleIcon class="mr-2 size-4 animate-spin" />
          正在加载充值申请...
        </div>
        <div v-else class="overflow-x-auto rounded-md border">
          <table class="w-full text-sm">
            <thead class="bg-muted/50 text-left">
              <tr class="border-b">
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
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in applications" :key="item.id" class="border-b last:border-b-0">
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
                <td class="max-w-[320px] px-4 py-3 text-muted-foreground">
                  <span class="line-clamp-1">{{ item.status === 'REJECTED' ? item.adminRemark || '未填写拒绝原因' : item.userRemark || '-' }}</span>
                </td>
                <td class="px-4 py-3 text-muted-foreground">
                  {{ formatTime(item.createTime) }}
                </td>
              </tr>
              <tr v-if="applications.length === 0">
                <td colspan="7" class="px-4 py-10 text-center text-muted-foreground">
                  暂无充值申请
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="mt-4 flex items-center justify-between text-sm text-muted-foreground">
          <span>第 {{ rechargeQuery.page }} / {{ applicationTotalPages }} 页</span>
          <div class="flex gap-2">
            <UiButton variant="outline" size="sm" :disabled="rechargeQuery.page <= 1" @click="rechargeQuery.page--; refetchApplications()">
              上一页
            </UiButton>
            <UiButton variant="outline" size="sm" :disabled="rechargeQuery.page >= applicationTotalPages" @click="rechargeQuery.page++; refetchApplications()">
              下一页
            </UiButton>
          </div>
        </div>
      </UiCardContent>
    </UiCard>

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
