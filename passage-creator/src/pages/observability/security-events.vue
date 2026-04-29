<script setup lang="ts">
import { RefreshCwIcon, SearchIcon } from '@lucide/vue'

import type { ObservabilityEventQuery, ObservabilityEventType } from '@/services/types/observability.type'

import { BasicPage } from '@/components/global-layout'
import { useGetSecurityEventPageQuery } from '@/services/api/observability.api'

const ALL_SELECT_VALUE = '__all__'

const query = reactive<ObservabilityEventQuery>({
  page: 1,
  pageSize: 10,
  eventType: '',
  ipAddress: '',
  accountIdentifier: '',
  startTime: '',
  endTime: '',
})

const { data, isFetching, refetch } = useGetSecurityEventPageQuery(query)
const events = computed(() => data.value?.data?.records ?? [])
const total = computed(() => data.value?.data?.totalRow ?? 0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / query.pageSize)))

const eventTypeModel = computed({
  get: () => query.eventType || ALL_SELECT_VALUE,
  set: value => query.eventType = value === ALL_SELECT_VALUE ? '' : value as ObservabilityEventType,
})

/**
 * 查询第一页安全事件。
 */
function handleSearch() {
  query.page = 1
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
 * 事件类型文案。
 */
function getEventTypeLabel(value?: string) {
  const labels: Record<string, string> = {
    login_failure: '登录失败',
    abnormal_ip: '异常 IP',
    force_logout: '强制下线',
    security_alert: '安全告警',
  }
  return labels[value || ''] ?? value ?? '-'
}

/**
 * 事件级别样式。
 */
function getLevelVariant(value?: string) {
  return value === 'error' ? 'destructive' : 'secondary'
}

/**
 * 格式化时间。
 */
function formatTime(value?: string) {
  return value ? new Date(value).toLocaleString() : '-'
}
</script>

<template>
  <BasicPage title="安全事件" description="查看登录失败、异常 IP、强制下线和告警联动记录。" sticky>
    <template #actions>
      <UiButton variant="outline" :disabled="isFetching" @click="refetch()">
        <RefreshCwIcon class="mr-1 size-4" :class="{ 'animate-spin': isFetching }" />
        刷新
      </UiButton>
    </template>

    <div class="space-y-4">
      <UiCard class="border-border/70">
        <UiCardHeader>
          <UiCardTitle>筛选条件</UiCardTitle>
          <UiCardDescription>按事件类型、账号、IP 和时间范围定位安全风险。</UiCardDescription>
        </UiCardHeader>
        <UiCardContent class="grid gap-3 md:grid-cols-6">
          <UiSelect v-model="eventTypeModel">
            <UiSelectTrigger><UiSelectValue placeholder="全部事件" /></UiSelectTrigger>
            <UiSelectContent>
              <UiSelectItem :value="ALL_SELECT_VALUE">
                全部事件
              </UiSelectItem>
              <UiSelectItem value="login_failure">
                登录失败
              </UiSelectItem>
              <UiSelectItem value="abnormal_ip">
                异常 IP
              </UiSelectItem>
              <UiSelectItem value="force_logout">
                强制下线
              </UiSelectItem>
              <UiSelectItem value="security_alert">
                安全告警
              </UiSelectItem>
            </UiSelectContent>
          </UiSelect>
          <UiInput v-model="query.accountIdentifier" placeholder="账号" />
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
          <UiCardTitle>安全事件记录</UiCardTitle>
          <UiCardDescription>当前共 {{ total }} 条记录。</UiCardDescription>
        </UiCardHeader>
        <UiCardContent>
          <div class="overflow-x-auto rounded-lg border">
            <table class="w-full text-sm">
              <thead class="bg-muted/50">
                <tr class="border-b text-left">
                  <th class="px-4 py-3">
                    类型
                  </th>
                  <th class="px-4 py-3">
                    级别
                  </th>
                  <th class="px-4 py-3">
                    主体
                  </th>
                  <th class="px-4 py-3">
                    账号
                  </th>
                  <th class="px-4 py-3">
                    IP
                  </th>
                  <th class="px-4 py-3">
                    详情
                  </th>
                  <th class="px-4 py-3">
                    关联
                  </th>
                  <th class="px-4 py-3">
                    时间
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in events" :key="item.id" class="border-b last:border-b-0">
                  <td class="px-4 py-3">
                    {{ getEventTypeLabel(item.eventType) }}
                  </td>
                  <td class="px-4 py-3">
                    <UiBadge :variant="getLevelVariant(item.eventLevel)">
                      {{ item.eventLevel || 'warning' }}
                    </UiBadge>
                  </td>
                  <td class="px-4 py-3">
                    {{ item.subject || '-' }}
                  </td>
                  <td class="px-4 py-3">
                    {{ item.accountIdentifier || '-' }}
                  </td>
                  <td class="px-4 py-3">
                    {{ item.ipAddress || '-' }}
                  </td>
                  <td class="max-w-[420px] truncate px-4 py-3">
                    {{ item.detail || item.exceptionSummary || '-' }}
                  </td>
                  <td class="px-4 py-3 text-xs text-muted-foreground">
                    审计 {{ item.auditLogId || '-' }} / 通知 {{ item.notificationId || '-' }}
                  </td>
                  <td class="px-4 py-3 text-muted-foreground">
                    {{ formatTime(item.eventTime) }}
                  </td>
                </tr>
                <tr v-if="events.length === 0">
                  <td colspan="8" class="px-4 py-10 text-center text-muted-foreground">
                    暂无安全事件
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
