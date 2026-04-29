<script setup lang="ts">
import { DownloadIcon, LoaderCircleIcon, RefreshCwIcon, SearchIcon } from '@lucide/vue'
import { toast } from 'vue-sonner'

import type { AuditLogItem, AuditLogQuery } from '@/services/types/audit-log.type'

import { BasicPage } from '@/components/global-layout'
import {
  useExportAuditLogMutation,
  useGetAuditLogDetailQuery,
  useGetAuditLogPageQuery,
} from '@/services/api/audit-log.api'

const ALL_SELECT_VALUE = '__all__'

/**
 * 审计日志筛选条件。
 */
const query = reactive<AuditLogQuery>({
  page: 1,
  pageSize: 10,
  logType: '',
  userId: '',
  accountIdentifier: '',
  ipAddress: '',
  requestPath: '',
  httpMethod: '',
  result: '',
  operationDescription: '',
  businessModule: '',
  operationType: '',
  auditStartTime: '',
  auditEndTime: '',
})

const detailId = ref<number | null>(null)
const { data, isFetching, refetch } = useGetAuditLogPageQuery(query)
const { data: detailData, isFetching: isFetchingDetail } = useGetAuditLogDetailQuery(detailId)
const { mutateAsync: exportAuditLogs, isPending: isExporting } = useExportAuditLogMutation()

const auditLogs = computed(() => data.value?.data?.records ?? [])
const total = computed(() => data.value?.data?.totalRow ?? 0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / query.pageSize)))
const detail = computed(() => detailData.value?.data ?? null)

/**
 * Reka Select 不允许空字符串选项，这里用哨兵值表示全部。
 */
function createAllOptionModel<T extends string>(getValue: () => T | '', setValue: (value: T | '') => void) {
  return computed({
    get: () => getValue() || ALL_SELECT_VALUE,
    set: value => setValue(value === ALL_SELECT_VALUE ? '' : value as T),
  })
}

const logTypeModel = createAllOptionModel(() => query.logType ?? '', value => query.logType = value)
const resultModel = createAllOptionModel(() => query.result ?? '', value => query.result = value)
const methodModel = createAllOptionModel(() => query.httpMethod ?? '', value => query.httpMethod = value)

/**
 * 查询第一页审计日志。
 */
function handleSearch() {
  query.page = 1
  refetch()
}

/**
 * 重置审计日志筛选条件。
 */
function handleReset() {
  Object.assign(query, {
    page: 1,
    pageSize: 10,
    logType: '',
    userId: '',
    accountIdentifier: '',
    ipAddress: '',
    requestPath: '',
    httpMethod: '',
    result: '',
    operationDescription: '',
    businessModule: '',
    operationType: '',
    auditStartTime: '',
    auditEndTime: '',
  })
  refetch()
}

/**
 * 切换分页。
 */
function changePage(page: number) {
  query.page = Math.min(Math.max(page, 1), totalPages.value)
  refetch()
}

/**
 * 打开审计详情。
 */
function openDetail(item: AuditLogItem) {
  detailId.value = item.id
}

/**
 * 导出当前筛选结果。
 */
async function handleExport() {
  try {
    const blob = await exportAuditLogs({ ...query, exportLimit: 5000 })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `audit-logs-${Date.now()}.xlsx`
    link.click()
    URL.revokeObjectURL(url)
    toast.success('审计日志已开始导出')
  }
  catch (error: any) {
    toast.error(error?.data?.message ?? error?.message ?? '导出失败')
  }
}

/**
 * 格式化时间展示。
 */
function formatTime(value?: string) {
  return value ? new Date(value).toLocaleString() : '-'
}

/**
 * 获取日志类型文案。
 */
function getLogTypeLabel(value?: string) {
  return value === 'login' ? '登录日志' : '管理员操作'
}

/**
 * 获取结果标签样式。
 */
function getResultVariant(value?: string) {
  return value === 'success' ? 'default' : 'destructive'
}
</script>

<template>
  <BasicPage title="审计日志" description="查询登录行为、管理员操作、异常结果和请求轨迹。" sticky>
    <template #actions>
      <UiButton variant="outline" :disabled="isFetching" @click="refetch()">
        <RefreshCwIcon class="mr-1 size-4" :class="{ 'animate-spin': isFetching }" />
        刷新
      </UiButton>
      <UiButton :disabled="isExporting" @click="handleExport">
        <LoaderCircleIcon v-if="isExporting" class="mr-1 size-4 animate-spin" />
        <DownloadIcon v-else class="mr-1 size-4" />
        {{ isExporting ? '导出中' : '导出' }}
      </UiButton>
    </template>

    <div class="space-y-4">
      <UiCard class="border-border/70">
        <UiCardHeader>
          <UiCardTitle>筛选条件</UiCardTitle>
          <UiCardDescription>按日志类型、用户、IP、路径、结果和时间范围定位审计记录。</UiCardDescription>
        </UiCardHeader>
        <UiCardContent class="grid gap-3 md:grid-cols-4 xl:grid-cols-6">
          <UiSelect v-model="logTypeModel">
            <UiSelectTrigger><UiSelectValue placeholder="全部类型" /></UiSelectTrigger>
            <UiSelectContent>
              <UiSelectItem :value="ALL_SELECT_VALUE">
                全部类型
              </UiSelectItem>
              <UiSelectItem value="login">
                登录日志
              </UiSelectItem>
              <UiSelectItem value="admin_operation">
                管理员操作
              </UiSelectItem>
            </UiSelectContent>
          </UiSelect>
          <UiInput v-model="query.userId" placeholder="用户 ID" />
          <UiInput v-model="query.accountIdentifier" placeholder="账号" />
          <UiInput v-model="query.ipAddress" placeholder="IP 地址" />
          <UiInput v-model="query.requestPath" placeholder="请求路径" />
          <UiSelect v-model="methodModel">
            <UiSelectTrigger><UiSelectValue placeholder="请求方法" /></UiSelectTrigger>
            <UiSelectContent>
              <UiSelectItem :value="ALL_SELECT_VALUE">
                全部方法
              </UiSelectItem>
              <UiSelectItem value="GET">
                GET
              </UiSelectItem>
              <UiSelectItem value="POST">
                POST
              </UiSelectItem>
              <UiSelectItem value="PUT">
                PUT
              </UiSelectItem>
              <UiSelectItem value="DELETE">
                DELETE
              </UiSelectItem>
            </UiSelectContent>
          </UiSelect>
          <UiSelect v-model="resultModel">
            <UiSelectTrigger><UiSelectValue placeholder="执行结果" /></UiSelectTrigger>
            <UiSelectContent>
              <UiSelectItem :value="ALL_SELECT_VALUE">
                全部结果
              </UiSelectItem>
              <UiSelectItem value="success">
                成功
              </UiSelectItem>
              <UiSelectItem value="failure">
                失败
              </UiSelectItem>
            </UiSelectContent>
          </UiSelect>
          <UiInput v-model="query.operationDescription" placeholder="操作描述" />
          <UiInput v-model="query.businessModule" placeholder="业务模块" />
          <UiInput v-model="query.operationType" placeholder="操作类型" />
          <UiInput v-model="query.auditStartTime" type="datetime-local" />
          <UiInput v-model="query.auditEndTime" type="datetime-local" />
          <div class="flex gap-2 md:col-span-2 xl:col-span-6">
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
        <UiCardHeader>
          <UiCardTitle>审计记录</UiCardTitle>
          <UiCardDescription>当前共 {{ total }} 条记录。</UiCardDescription>
        </UiCardHeader>
        <UiCardContent>
          <div v-if="isFetching" class="flex items-center justify-center py-12 text-sm text-muted-foreground">
            <LoaderCircleIcon class="mr-2 size-4 animate-spin" />
            正在加载审计日志...
          </div>
          <div v-else class="overflow-x-auto rounded-lg border">
            <table class="w-full text-sm">
              <thead class="bg-muted/50">
                <tr class="border-b text-left">
                  <th class="px-4 py-3">
                    类型
                  </th>
                  <th class="px-4 py-3">
                    账号
                  </th>
                  <th class="px-4 py-3">
                    IP
                  </th>
                  <th class="px-4 py-3">
                    请求
                  </th>
                  <th class="px-4 py-3">
                    描述
                  </th>
                  <th class="px-4 py-3">
                    结果
                  </th>
                  <th class="px-4 py-3">
                    耗时
                  </th>
                  <th class="px-4 py-3">
                    时间
                  </th>
                  <th class="px-4 py-3 text-right">
                    操作
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in auditLogs" :key="item.id" class="border-b last:border-b-0">
                  <td class="px-4 py-3">
                    {{ getLogTypeLabel(item.logType) }}
                  </td>
                  <td class="px-4 py-3">
                    <div class="font-medium">
                      {{ item.accountIdentifier || '-' }}
                    </div>
                    <div class="text-xs text-muted-foreground">
                      {{ item.userId || '-' }}
                    </div>
                  </td>
                  <td class="px-4 py-3">
                    {{ item.ipAddress || '-' }}
                  </td>
                  <td class="px-4 py-3">
                    <div class="font-medium">
                      {{ item.httpMethod || '-' }}
                    </div>
                    <div class="max-w-[220px] truncate text-xs text-muted-foreground">
                      {{ item.requestPath || '-' }}
                    </div>
                  </td>
                  <td class="px-4 py-3">
                    <div>{{ item.operationDescription || '-' }}</div>
                    <div class="text-xs text-muted-foreground">
                      {{ item.businessModule || '-' }} / {{ item.operationType || '-' }}
                    </div>
                  </td>
                  <td class="px-4 py-3">
                    <UiBadge :variant="getResultVariant(item.result)">
                      {{ item.result === 'success' ? '成功' : '失败' }}
                    </UiBadge>
                  </td>
                  <td class="px-4 py-3">
                    {{ item.costMillis ?? '-' }} ms
                  </td>
                  <td class="px-4 py-3 text-muted-foreground">
                    {{ formatTime(item.auditTime) }}
                  </td>
                  <td class="px-4 py-3 text-right">
                    <UiButton size="sm" variant="outline" @click="openDetail(item)">
                      详情
                    </UiButton>
                  </td>
                </tr>
                <tr v-if="auditLogs.length === 0">
                  <td colspan="9" class="px-4 py-10 text-center text-muted-foreground">
                    暂无审计日志
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

    <UiDialog :open="detailId !== null" @update:open="value => !value ? detailId = null : undefined">
      <UiDialogContent class="max-w-3xl">
        <UiDialogHeader>
          <UiDialogTitle>审计详情</UiDialogTitle>
          <UiDialogDescription>{{ detail?.traceId || '未记录追踪 ID' }}</UiDialogDescription>
        </UiDialogHeader>
        <div v-if="isFetchingDetail" class="flex items-center justify-center py-10 text-sm text-muted-foreground">
          <LoaderCircleIcon class="mr-2 size-4 animate-spin" />
          正在加载详情...
        </div>
        <div v-else class="grid gap-4 text-sm">
          <div class="grid gap-3 md:grid-cols-3">
            <div><span class="text-muted-foreground">账号：</span>{{ detail?.accountIdentifier || '-' }}</div>
            <div><span class="text-muted-foreground">用户 ID：</span>{{ detail?.userId || '-' }}</div>
            <div><span class="text-muted-foreground">状态码：</span>{{ detail?.statusCode ?? '-' }}</div>
          </div>
          <div class="rounded-md border bg-muted/30 p-3">
            <div class="mb-2 font-medium">
              请求摘要
            </div>
            <pre class="max-h-40 overflow-auto whitespace-pre-wrap text-xs">{{ detail?.requestSummary || '-' }}</pre>
          </div>
          <div class="rounded-md border bg-muted/30 p-3">
            <div class="mb-2 font-medium">
              响应摘要
            </div>
            <pre class="max-h-40 overflow-auto whitespace-pre-wrap text-xs">{{ detail?.responseSummary || '-' }}</pre>
          </div>
          <div class="rounded-md border bg-muted/30 p-3">
            <div class="mb-2 font-medium">
              异常摘要
            </div>
            <pre class="max-h-40 overflow-auto whitespace-pre-wrap text-xs">{{ detail?.exceptionSummary || detail?.failureReason || '-' }}</pre>
          </div>
        </div>
      </UiDialogContent>
    </UiDialog>
  </BasicPage>
</template>

<route lang="yaml">
meta:
  auth: true
</route>
