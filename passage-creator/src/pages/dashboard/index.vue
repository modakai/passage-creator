<script lang="ts" setup>
import { BasicPage } from '@/components/global-layout'
import { Button } from '@/components/ui/button'
import { useGetDashboardStatisticsQuery } from '@/services/api/dashboard.api'

import OverviewContent from './components/overview-content.vue'

const tabs = ref([
  { name: 'Overview', value: 'overview' },
  { name: 'Analytics', value: 'analytics', disabled: true },
  { name: 'Reports', value: 'reports', disabled: true },
  { name: 'Notifications', value: 'notifications', disabled: true },
])

const activeTab = ref(tabs.value[0].value)
const dashboardQuery = useGetDashboardStatisticsQuery()

const statistics = computed(() => dashboardQuery.data.value?.data)
</script>

<template>
  <BasicPage
    title="workspace"
    description="workspace description"
    sticky
  >
    <template #actions>
      <Button
        :disabled="dashboardQuery.isFetching.value"
        @click="() => dashboardQuery.refetch()"
      >
        刷新数据
      </Button>
    </template>

    <UiTabs :default-value="activeTab" class="w-full">
      <UiTabsList>
        <UiTabsTrigger
          v-for="tab in tabs" :key="tab.value"
          :value="tab.value"
          :disabled="tab.disabled"
        >
          {{ tab.name }}
        </UiTabsTrigger>
      </UiTabsList>
      <UiTabsContent value="overview" class="space-y-4">
        <OverviewContent
          :statistics="statistics"
          :loading="dashboardQuery.isLoading.value"
          :fetching="dashboardQuery.isFetching.value"
          :error="dashboardQuery.error.value"
          @retry="dashboardQuery.refetch"
        />
      </UiTabsContent>
    </UiTabs>
  </BasicPage>
</template>
