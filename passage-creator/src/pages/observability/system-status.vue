<script setup lang="ts">
import { ActivityIcon, CpuIcon, DatabaseIcon, HardDriveIcon, MemoryStickIcon, RefreshCwIcon, ServerIcon } from '@lucide/vue'

import type { DependencyStatus, MetricSnapshot, ObservabilityStatus } from '@/services/types/observability.type'

import { BasicPage } from '@/components/global-layout'
import { useGetSystemStatusQuery } from '@/services/api/observability.api'

const { data, isFetching, refetch } = useGetSystemStatusQuery()

const status = computed(() => data.value?.data ?? null)

/**
 * 状态文案映射。
 */
function getStatusLabel(value?: ObservabilityStatus) {
  const labels: Record<string, string> = {
    up: '正常',
    degraded: '降级',
    down: '异常',
    unknown: '未知',
  }
  return labels[value || 'unknown'] ?? '未知'
}

/**
 * 状态标签样式。
 */
function getStatusVariant(value?: ObservabilityStatus) {
  if (value === 'down') {
    return 'destructive'
  }
  if (value === 'degraded') {
    return 'secondary'
  }
  return 'default'
}

/**
 * 百分比格式化。
 */
function formatPercent(value?: number) {
  return `${(value ?? 0).toFixed(1)}%`
}

/**
 * 字节数格式化。
 */
function formatBytes(value?: number) {
  if (!value) {
    return '0 B'
  }
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let size = value
  let index = 0
  while (size >= 1024 && index < units.length - 1) {
    size /= 1024
    index += 1
  }
  return `${size.toFixed(1)} ${units[index]}`
}

/**
 * JVM 内存说明文案。
 */
function getJvmMemoryHint(item: MetricSnapshot) {
  if (item.name === '非堆内存' && item.status === 'up') {
    return `${formatBytes(item.used)} / 已提交 ${formatBytes(item.total)}`
  }
  return `${formatBytes(item.used)} / ${formatBytes(item.total)}`
}

/**
 * 时间格式化。
 */
function formatTime(value?: string) {
  return value ? new Date(value).toLocaleString() : '-'
}

/**
 * 依赖连接池描述。
 */
function getPoolText(item?: DependencyStatus) {
  if (!item?.totalConnections && !item?.activeConnections && !item?.idleConnections) {
    return '未暴露连接池指标'
  }
  return `活跃 ${item.activeConnections ?? 0} / 空闲 ${item.idleConnections ?? 0} / 总数 ${item.totalConnections ?? 0}`
}
</script>

<template>
  <BasicPage title="系统状态" description="查看 JVM、主机资源、数据库和 Redis 的当前运行状态。" sticky>
    <template #actions>
      <UiButton variant="outline" :disabled="isFetching" @click="refetch()">
        <RefreshCwIcon class="mr-1 size-4" :class="{ 'animate-spin': isFetching }" />
        刷新
      </UiButton>
    </template>

    <div class="space-y-4">
      <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <UiCard class="border-border/70">
          <UiCardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
            <UiCardTitle class="text-sm font-medium">
              综合状态
            </UiCardTitle>
            <ServerIcon class="size-4 text-muted-foreground" />
          </UiCardHeader>
          <UiCardContent>
            <UiBadge :variant="getStatusVariant(status?.overallStatus)">
              {{ getStatusLabel(status?.overallStatus) }}
            </UiBadge>
            <p class="mt-3 text-xs text-muted-foreground">
              采样时间：{{ formatTime(status?.sampleTime) }}
            </p>
          </UiCardContent>
        </UiCard>
        <UiCard class="border-border/70">
          <UiCardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
            <UiCardTitle class="text-sm font-medium">
              系统 CPU
            </UiCardTitle>
            <CpuIcon class="size-4 text-muted-foreground" />
          </UiCardHeader>
          <UiCardContent>
            <div class="text-2xl font-semibold">
              {{ formatPercent(status?.os.systemCpu.usagePercent) }}
            </div>
            <UiProgress class="mt-3" :model-value="status?.os.systemCpu.usagePercent ?? 0" />
          </UiCardContent>
        </UiCard>
        <UiCard class="border-border/70">
          <UiCardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
            <UiCardTitle class="text-sm font-medium">
              系统内存
            </UiCardTitle>
            <MemoryStickIcon class="size-4 text-muted-foreground" />
          </UiCardHeader>
          <UiCardContent>
            <div class="text-2xl font-semibold">
              {{ formatPercent(status?.os.memory.usagePercent) }}
            </div>
            <p class="mt-1 text-xs text-muted-foreground">
              {{ formatBytes(status?.os.memory.used) }} / {{ formatBytes(status?.os.memory.total) }}
            </p>
            <UiProgress class="mt-3" :model-value="status?.os.memory.usagePercent ?? 0" />
          </UiCardContent>
        </UiCard>
        <UiCard class="border-border/70">
          <UiCardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
            <UiCardTitle class="text-sm font-medium">
              磁盘
            </UiCardTitle>
            <HardDriveIcon class="size-4 text-muted-foreground" />
          </UiCardHeader>
          <UiCardContent>
            <div class="text-2xl font-semibold">
              {{ formatPercent(status?.os.disk.usagePercent) }}
            </div>
            <p class="mt-1 text-xs text-muted-foreground">
              {{ formatBytes(status?.os.disk.used) }} / {{ formatBytes(status?.os.disk.total) }}
            </p>
            <UiProgress class="mt-3" :model-value="status?.os.disk.usagePercent ?? 0" />
          </UiCardContent>
        </UiCard>
      </div>

      <div class="grid gap-4 xl:grid-cols-2">
        <UiCard class="border-border/70">
          <UiCardHeader>
            <UiCardTitle>JVM</UiCardTitle>
            <UiCardDescription>堆内存、非堆内存、线程和 GC 快照。</UiCardDescription>
          </UiCardHeader>
          <UiCardContent class="space-y-4">
            <div v-for="item in [status?.jvm.heapMemory, status?.jvm.nonHeapMemory].filter(Boolean) as MetricSnapshot[]" :key="item.name">
              <div class="mb-2 flex items-center justify-between text-sm">
                <span>{{ item.name }}</span>
                <span class="text-muted-foreground">{{ item.name === '非堆内存' ? '动态扩展' : formatPercent(item.usagePercent) }}</span>
              </div>
              <UiProgress :model-value="item.name === '非堆内存' ? 0 : item.usagePercent ?? 0" />
              <p class="mt-1 text-xs text-muted-foreground">
                {{ getJvmMemoryHint(item) }}
              </p>
            </div>
            <div class="grid gap-3 pt-2 md:grid-cols-3">
              <div class="rounded-md border p-3">
                <div class="text-xs text-muted-foreground">
                  线程
                </div>
                <div class="mt-1 text-lg font-semibold">
                  {{ status?.jvm.threadCount ?? 0 }}
                </div>
              </div>
              <div class="rounded-md border p-3">
                <div class="text-xs text-muted-foreground">
                  守护线程
                </div>
                <div class="mt-1 text-lg font-semibold">
                  {{ status?.jvm.daemonThreadCount ?? 0 }}
                </div>
              </div>
              <div class="rounded-md border p-3">
                <div class="text-xs text-muted-foreground">
                  GC 耗时
                </div>
                <div class="mt-1 text-lg font-semibold">
                  {{ status?.jvm.gcTimeMillis ?? 0 }} ms
                </div>
              </div>
            </div>
          </UiCardContent>
        </UiCard>

        <UiCard class="border-border/70">
          <UiCardHeader>
            <UiCardTitle>依赖状态</UiCardTitle>
            <UiCardDescription>数据库和 Redis 的可用性与响应耗时。</UiCardDescription>
          </UiCardHeader>
          <UiCardContent class="space-y-3">
            <div v-for="item in [status?.database, status?.redis].filter(Boolean) as DependencyStatus[]" :key="item.name" class="rounded-md border p-4">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2 font-medium">
                  <DatabaseIcon v-if="item.name === '数据库'" class="size-4" />
                  <ActivityIcon v-else class="size-4" />
                  {{ item.name }}
                </div>
                <UiBadge :variant="getStatusVariant(item.status)">
                  {{ getStatusLabel(item.status) }}
                </UiBadge>
              </div>
              <p class="mt-2 text-sm text-muted-foreground">
                {{ item.message || '-' }}
              </p>
              <p class="mt-1 text-xs text-muted-foreground">
                耗时：{{ item.latencyMillis ?? 0 }} ms
              </p>
              <p v-if="item.name === '数据库'" class="mt-1 text-xs text-muted-foreground">
                {{ getPoolText(item) }}
              </p>
            </div>
          </UiCardContent>
        </UiCard>
      </div>
    </div>
  </BasicPage>
</template>

<route lang="yaml">
meta:
  auth: true
</route>
