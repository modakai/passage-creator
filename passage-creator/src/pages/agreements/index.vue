<script setup lang="ts">
import { LoaderCircleIcon, RefreshCwIcon, Trash2Icon } from '@lucide/vue'
import { useI18n } from 'vue-i18n'
import { toast } from 'vue-sonner'

import type { AgreementQuery } from '@/services/types/agreement.type'

import { BasicPage } from '@/components/global-layout'
import {
  useDeleteAgreementMutation,
  useGetAgreementPageQuery,
  useGetAgreementTypeOptionsQuery,
} from '@/services/api/agreement.api'

import AgreementFormDialog from './components/agreement-form-dialog.vue'

/**
 * 协议列表查询条件。
 */
const query = reactive<AgreementQuery>({
  page: 1,
  pageSize: 10,
  agreementType: '',
  title: '',
  status: '',
})

const { data, isFetching, refetch } = useGetAgreementPageQuery(query)
const { data: typeOptionsData } = useGetAgreementTypeOptionsQuery()
const { mutateAsync: deleteAgreement, isPending: isDeleting } = useDeleteAgreementMutation()
const { t } = useI18n()

/**
 * 统一读取协议列表。
 */
const agreementList = computed(() => data.value?.data?.records ?? [])
const total = computed(() => data.value?.data?.totalRow ?? 0)
const typeOptions = computed(() => typeOptionsData.value?.data ?? [])
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / query.pageSize)))
const deletingId = ref<number | null>(null)

/**
 * 用字符串承接筛选状态，提交前转换为数字。
 */
const statusFilter = computed({
  get: () => query.status === '' ? 'all' : String(query.status),
  set: (value) => {
    query.status = value === 'all' ? '' : Number(value)
  },
})

/**
 * 用字符串承接全部类型选项。
 */
const agreementTypeFilter = computed({
  get: () => query.agreementType || 'all',
  set: value => query.agreementType = value === 'all' ? '' : value,
})

/**
 * 状态文案映射。
 */
function getStatusText(status: number) {
  return status === 1 ? t('common.status.enabled') : t('common.status.disabled')
}

/**
 * 状态样式映射。
 */
function getStatusVariant(status: number) {
  return status === 1 ? 'default' : 'secondary'
}

/**
 * 格式化时间展示。
 */
function formatTime(value?: string) {
  if (!value) {
    return t('common.emptyDash')
  }
  return new Date(value).toLocaleString()
}

/**
 * 提交查询前重置分页。
 */
function handleSearch() {
  query.page = 1
  refetch()
}

/**
 * 重置筛选条件。
 */
function handleReset() {
  query.page = 1
  query.pageSize = 10
  query.agreementType = ''
  query.title = ''
  query.status = ''
  refetch()
}

/**
 * 切换分页。
 */
function changePage(nextPage: number) {
  query.page = Math.min(Math.max(nextPage, 1), totalPages.value)
  refetch()
}

/**
 * 删除单条协议。
 */
async function handleDelete() {
  if (!deletingId.value) {
    return
  }
  try {
    await deleteAgreement(deletingId.value)
    toast.success(t('pages.agreements.deleteSuccess'))
    deletingId.value = null
    refetch()
  }
  catch (error: any) {
    const message = error?.data?.message ?? error?.message ?? t('pages.agreements.deleteFailed')
    toast.error(message)
  }
}
</script>

<template>
  <BasicPage :title="t('pages.agreements.title')" :description="t('pages.agreements.description')" sticky>
    <template #actions>
      <AgreementFormDialog @success="refetch()" />
      <UiButton variant="outline" :disabled="isFetching" @click="refetch()">
        <RefreshCwIcon class="mr-1 size-4" :class="{ 'animate-spin': isFetching }" />
        {{ t('actions.refresh') }}
      </UiButton>
    </template>

    <div class="space-y-4">
      <UiCard>
        <UiCardHeader>
          <UiCardTitle>{{ t('pages.agreements.filterTitle') }}</UiCardTitle>
        </UiCardHeader>
        <UiCardContent class="grid gap-4 md:grid-cols-4">
          <div class="space-y-2">
            <UiLabel>{{ t('pages.agreements.type') }}</UiLabel>
            <UiSelect v-model="agreementTypeFilter">
              <UiSelectTrigger class="w-full">
                <UiSelectValue :placeholder="t('pages.agreements.allTypes')" />
              </UiSelectTrigger>
              <UiSelectContent>
                <UiSelectItem value="all">
                  {{ t('pages.agreements.allTypes') }}
                </UiSelectItem>
                <UiSelectItem v-for="item in typeOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </UiSelectItem>
              </UiSelectContent>
            </UiSelect>
          </div>

          <div class="space-y-2">
            <UiLabel>{{ t('pages.agreements.titleLabel') }}</UiLabel>
            <UiInput v-model="query.title" :placeholder="t('pages.agreements.titlePlaceholder')" />
          </div>

          <div class="space-y-2">
            <UiLabel>{{ t('pages.agreements.status') }}</UiLabel>
            <UiSelect v-model="statusFilter">
              <UiSelectTrigger class="w-full">
                <UiSelectValue :placeholder="t('pages.agreements.allStatus')" />
              </UiSelectTrigger>
              <UiSelectContent>
                <UiSelectItem value="all">
                  {{ t('pages.agreements.allStatus') }}
                </UiSelectItem>
                <UiSelectItem value="1">
                  {{ t('common.status.enabled') }}
                </UiSelectItem>
                <UiSelectItem value="0">
                  {{ t('common.status.disabled') }}
                </UiSelectItem>
              </UiSelectContent>
            </UiSelect>
          </div>

          <div class="flex items-end gap-2">
            <UiButton class="flex-1" @click="handleSearch">
              {{ t('actions.search') }}
            </UiButton>
            <UiButton variant="outline" class="flex-1" @click="handleReset">
              {{ t('actions.reset') }}
            </UiButton>
          </div>
        </UiCardContent>
      </UiCard>

      <UiCard>
        <UiCardHeader class="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
          <div>
            <UiCardTitle>{{ t('pages.agreements.listTitle') }}</UiCardTitle>
            <UiCardDescription>
              {{ t('pages.agreements.total', { total }) }}
            </UiCardDescription>
          </div>
        </UiCardHeader>
        <UiCardContent>
          <div v-if="isFetching" class="flex items-center justify-center py-12 text-sm text-muted-foreground">
            <LoaderCircleIcon class="mr-2 size-4 animate-spin" />
            {{ t('pages.agreements.loading') }}
          </div>

          <div v-else class="overflow-x-auto rounded-lg border">
            <table class="w-full text-sm">
              <thead class="bg-muted/50">
                <tr class="border-b text-left">
                  <th class="px-4 py-3 font-medium">
                    {{ t('pages.agreements.titleLabel') }}
                  </th>
                  <th class="px-4 py-3 font-medium">
                    {{ t('pages.agreements.type') }}
                  </th>
                  <th class="px-4 py-3 font-medium">
                    {{ t('pages.agreements.status') }}
                  </th>
                  <th class="px-4 py-3 font-medium">
                    {{ t('pages.agreements.sortOrder') }}
                  </th>
                  <th class="px-4 py-3 font-medium">
                    {{ t('pages.agreements.updatedAt') }}
                  </th>
                  <th class="px-4 py-3 font-medium text-right">
                    {{ t('actions.action') }}
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in agreementList" :key="item.id" class="border-b last:border-b-0">
                  <td class="px-4 py-3">
                    <div class="font-medium">
                      {{ item.title }}
                    </div>
                    <div v-if="item.remark" class="mt-1 text-xs text-muted-foreground">
                      {{ item.remark }}
                    </div>
                  </td>
                  <td class="px-4 py-3 text-muted-foreground">
                    {{ item.agreementType }}
                  </td>
                  <td class="px-4 py-3">
                    <UiBadge :variant="getStatusVariant(item.status)">
                      {{ getStatusText(item.status) }}
                    </UiBadge>
                  </td>
                  <td class="px-4 py-3 text-muted-foreground">
                    {{ item.sortOrder }}
                  </td>
                  <td class="px-4 py-3 text-muted-foreground">
                    {{ formatTime(item.updateTime) }}
                  </td>
                  <td class="px-4 py-3">
                    <div class="flex justify-end gap-2">
                      <AgreementFormDialog :agreement-id="item.id" @success="refetch()" />
                      <UiButton variant="outline" size="sm" :disabled="isDeleting" @click="deletingId = item.id">
                        <Trash2Icon class="mr-1 size-4" />
                        {{ t('actions.delete') }}
                      </UiButton>
                    </div>
                  </td>
                </tr>
                <tr v-if="agreementList.length === 0">
                  <td colspan="6" class="px-4 py-10 text-center text-muted-foreground">
                    {{ t('pages.agreements.empty') }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="mt-4 flex items-center justify-between text-sm text-muted-foreground">
            <span>{{ t('pages.agreements.page', { page: query.page, totalPages }) }}</span>
            <div class="flex gap-2">
              <UiButton variant="outline" size="sm" :disabled="query.page <= 1" @click="changePage(query.page - 1)">
                {{ t('common.previousPage') }}
              </UiButton>
              <UiButton variant="outline" size="sm" :disabled="query.page >= totalPages" @click="changePage(query.page + 1)">
                {{ t('common.nextPage') }}
              </UiButton>
            </div>
          </div>
        </UiCardContent>
      </UiCard>
    </div>

    <UiAlertDialog :open="deletingId !== null" @update:open="value => !value ? deletingId = null : undefined">
      <UiAlertDialogContent>
        <UiAlertDialogHeader>
          <UiAlertDialogTitle>{{ t('pages.agreements.deleteTitle') }}</UiAlertDialogTitle>
          <UiAlertDialogDescription>
            {{ t('pages.agreements.deleteDesc') }}
          </UiAlertDialogDescription>
        </UiAlertDialogHeader>
        <UiAlertDialogFooter>
          <UiAlertDialogCancel @click="deletingId = null">
            {{ t('actions.cancel') }}
          </UiAlertDialogCancel>
          <UiAlertDialogAction :disabled="isDeleting" @click="handleDelete">
            {{ t('pages.agreements.confirmDelete') }}
          </UiAlertDialogAction>
        </UiAlertDialogFooter>
      </UiAlertDialogContent>
    </UiAlertDialog>
  </BasicPage>
</template>

<route lang="yaml">
meta:
  auth: true
</route>
