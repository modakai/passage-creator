<script setup lang="ts">
import { VisArea, VisAxis, VisLine, VisXYContainer } from '@unovis/vue'

import type { ChartConfig } from '@/components/ui/chart'
import type { DashboardLoginTrend } from '@/services/types/dashboard.type'

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import {
  ChartContainer,
  ChartCrosshair,
  ChartTooltip,
  ChartTooltipContent,
  componentToString,
} from '@/components/ui/chart'

const props = defineProps<{
  data: DashboardLoginTrend[]
  loading: boolean
}>()

interface ChartPoint {
  date: Date
  label: string
  loginCount: number
}

const chartConfig = {
  loginCount: {
    label: '成功登录',
    color: 'var(--chart-1)',
  },
} satisfies ChartConfig

const svgDefs = `
  <linearGradient id="fillLoginCount" x1="0" y1="0" x2="0" y2="1">
    <stop offset="5%" stop-color="var(--color-loginCount)" stop-opacity="0.55" />
    <stop offset="95%" stop-color="var(--color-loginCount)" stop-opacity="0.08" />
  </linearGradient>
`

const chartData = computed<ChartPoint[]>(() => {
  return props.data.map(item => ({
    date: new Date(item.startTime),
    label: item.label,
    loginCount: item.loginCount ?? 0,
  }))
})

const yDomain = computed<[number, number]>(() => {
  const max = Math.max(...chartData.value.map(item => item.loginCount), 0)
  return [0, Math.max(5, Math.ceil(max * 1.2))]
})

const hasData = computed(() => chartData.value.some(item => item.loginCount > 0))
</script>

<template>
  <Card class="pt-0">
    <CardHeader class="flex items-center gap-2 space-y-0 border-b py-5 sm:flex-row">
      <div class="grid flex-1 gap-1">
        <CardTitle>登录趋势</CardTitle>
        <CardDescription>
          最近 7 天成功登录次数
        </CardDescription>
      </div>
    </CardHeader>
    <CardContent class="px-2 pt-4 sm:px-6 sm:pt-6 pb-4">
      <div v-if="loading" class="flex h-[250px] items-center justify-center text-sm text-muted-foreground">
        正在加载登录趋势
      </div>
      <div v-else-if="chartData.length === 0 || !hasData" class="flex h-[250px] items-center justify-center text-sm text-muted-foreground">
        暂无登录趋势数据
      </div>
      <ChartContainer v-else :config="chartConfig" class="aspect-auto h-[250px] w-full" :cursor="false">
        <VisXYContainer
          :data="chartData"
          :svg-defs="svgDefs"
          :margin="{ left: -30 }"
          :y-domain="yDomain"
        >
          <VisArea
            :x="(d: ChartPoint) => d.date"
            :y="(d: ChartPoint) => d.loginCount"
            color="url(#fillLoginCount)"
            :opacity="0.75"
          />
          <VisLine
            :x="(d: ChartPoint) => d.date"
            :y="(d: ChartPoint) => d.loginCount"
            :color="chartConfig.loginCount.color"
            :line-width="2"
          />
          <VisAxis
            type="x"
            :x="(d: ChartPoint) => d.date"
            :tick-line="false"
            :domain-line="false"
            :grid-line="false"
            :num-ticks="Math.min(chartData.length, 7)"
            :tick-format="(_d: number, index: number) => chartData[index]?.label ?? ''"
          />
          <VisAxis
            type="y"
            :num-ticks="3"
            :tick-line="false"
            :domain-line="false"
          />
          <ChartTooltip />
          <ChartCrosshair
            :template="componentToString(chartConfig, ChartTooltipContent, {
              labelFormatter: (d: number | Date) => new Date(d).toLocaleDateString('zh-CN', {
                month: '2-digit',
                day: '2-digit',
              }),
            })"
            :color="chartConfig.loginCount.color"
          />
        </VisXYContainer>
      </ChartContainer>
    </CardContent>
  </Card>
</template>
