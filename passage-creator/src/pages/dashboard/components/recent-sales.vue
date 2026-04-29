<script lang="ts" setup>
import type { DashboardRecentOperation } from '@/services/types/dashboard.type'

import { Avatar, AvatarFallback } from '@/components/ui/avatar'

const props = defineProps<{
  list: DashboardRecentOperation[]
  loading: boolean
}>()

function fallbackName(item: DashboardRecentOperation) {
  return (item.operator || item.module || '?').slice(0, 1).toUpperCase()
}

function operationTimeText(value?: string) {
  if (!value) {
    return '未知时间'
  }
  return new Date(value).toLocaleString()
}
</script>

<template>
  <div v-if="loading" class="flex min-h-[220px] items-center justify-center text-sm text-muted-foreground">
    正在加载最近操作
  </div>
  <div v-else-if="props.list.length === 0" class="flex min-h-[220px] items-center justify-center text-sm text-muted-foreground">
    暂无最近操作日志
  </div>
  <div v-else class="space-y-6">
    <div v-for="item in props.list" :key="item.id" class="flex items-start gap-4">
      <Avatar class-name="h-9 w-9">
        <AvatarFallback>{{ fallbackName(item) }}</AvatarFallback>
      </Avatar>
      <div class="min-w-0 flex-1">
        <div class="flex flex-wrap items-center justify-between gap-2">
          <p class="truncate text-sm font-medium leading-none">
            {{ item.operator || '未知操作人' }}
          </p>
          <span class="rounded-sm border px-2 py-0.5 text-xs text-muted-foreground">
            {{ item.result || 'unknown' }}
          </span>
        </div>
        <p class="mt-2 line-clamp-2 text-sm text-muted-foreground">
          {{ item.action || '未记录操作描述' }}
        </p>
        <div class="mt-2 flex flex-wrap gap-x-3 gap-y-1 text-xs text-muted-foreground">
          <span>{{ item.module || '未分类模块' }}</span>
          <span>{{ item.operationType || 'other' }}</span>
          <span>{{ item.ipAddress || '未知 IP' }}</span>
          <span>{{ operationTimeText(item.operationTime) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>
