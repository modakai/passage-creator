<script setup lang="ts">
import { BookMarkedIcon, LoaderCircleIcon, RefreshCwIcon, SquarePenIcon, Trash2Icon } from '@lucide/vue'
import { useI18n } from 'vue-i18n'
import { toast } from 'vue-sonner'

import type {
  DictEntityId,
  DictItemItem,
  DictItemQuery,
  DictTypeItem,
  DictTypeQuery,
} from '@/services/types/dict.type'

import { BasicPage } from '@/components/global-layout'
import {
  useDeleteDictItemMutation,
  useDeleteDictTypeMutation,
  useGetDictItemPageQuery,
  useGetDictTypePageQuery,
} from '@/services/api/dict.api'

import DictItemFormDialog from './components/dict-item-form-dialog.vue'
import DictTypeFormDialog from './components/dict-type-form-dialog.vue'

const { t } = useI18n()
const typeQuery = reactive<DictTypeQuery>({
  page: 1,
  pageSize: 8,
  dictCode: '',
  dictName: '',
  status: '',
})
const itemQuery = reactive<DictItemQuery>({
  page: 1,
  pageSize: 10,
  dictTypeId: undefined,
  dictLabel: '',
  dictValue: '',
  status: '',
})

const { data: typeData, isFetching: isFetchingTypes, refetch: refetchTypes } = useGetDictTypePageQuery(typeQuery)
const { data: itemData, isFetching: isFetchingItems, refetch: refetchItems } = useGetDictItemPageQuery(itemQuery)
const { mutateAsync: deleteDictType, isPending: isDeletingType } = useDeleteDictTypeMutation()
const { mutateAsync: deleteDictItem, isPending: isDeletingItem } = useDeleteDictItemMutation()

const typeList = computed(() => typeData.value?.data?.records ?? [])
const itemList = computed(() => itemData.value?.data?.records ?? [])
const selectedTypeId = ref<DictEntityId | null>(null)
const selectedType = computed(() => typeList.value.find(item => item.id === selectedTypeId.value) ?? null)
const typeTotal = computed(() => typeData.value?.data?.totalRow ?? 0)
const itemTotal = computed(() => itemData.value?.data?.totalRow ?? 0)
const typeTotalPages = computed(() => Math.max(1, Math.ceil(typeTotal.value / typeQuery.pageSize)))
const itemTotalPages = computed(() => Math.max(1, Math.ceil(itemTotal.value / itemQuery.pageSize)))
const typeStatusFilter = computed({
  get: () => typeQuery.status === '' ? 'all' : String(typeQuery.status),
  set: value => typeQuery.status = value === 'all' ? '' : Number(value),
})
const itemStatusFilter = computed({
  get: () => itemQuery.status === '' ? 'all' : String(itemQuery.status),
  set: value => itemQuery.status = value === 'all' ? '' : Number(value),
})

const deletingType = ref<DictTypeItem | null>(null)
const deletingItem = ref<DictItemItem | null>(null)
const isDeleteTypeDialogOpen = ref(false)
const isDeleteItemDialogOpen = ref(false)
const editingTypeId = ref<DictEntityId | null>(null)
const editingItemId = ref<DictEntityId | null>(null)
const isEditTypeDialogOpen = ref(false)
const isEditItemDialogOpen = ref(false)

watch(typeList, (list) => {
  if (list.length === 0) {
    selectedTypeId.value = null
    itemQuery.dictTypeId = undefined
    return
  }
  if (!selectedTypeId.value || !list.some(item => item.id === selectedTypeId.value)) {
    selectedTypeId.value = list[0].id
  }
}, { immediate: true })

watch(selectedTypeId, (value) => {
  itemQuery.page = 1
  itemQuery.dictTypeId = value ?? undefined
  if (value) {
    refetchItems()
  }
})

watch(isEditTypeDialogOpen, (value) => {
  if (!value) {
    editingTypeId.value = null
  }
})

watch(isEditItemDialogOpen, (value) => {
  if (!value) {
    editingItemId.value = null
  }
})

/**
 * 根据状态映射展示样式。
 */
function getStatusVariant(status?: number) {
  return status === 1 ? 'default' : 'secondary'
}

/**
 * 格式化时间显示。
 */
function formatTime(value?: string) {
  if (!value) {
    return t('common.emptyDash')
  }
  return new Date(value).toLocaleString()
}

/**
 * 查询字典类型。
 */
function handleSearchType() {
  typeQuery.page = 1
  refetchTypes()
}

/**
 * 重置字典类型筛选条件。
 */
function handleResetType() {
  typeQuery.page = 1
  typeQuery.pageSize = 8
  typeQuery.dictCode = ''
  typeQuery.dictName = ''
  typeQuery.status = ''
  refetchTypes()
}

/**
 * 查询字典明细。
 */
function handleSearchItem() {
  itemQuery.page = 1
  refetchItems()
}

/**
 * 重置字典明细筛选条件。
 */
function handleResetItem() {
  itemQuery.page = 1
  itemQuery.pageSize = 10
  itemQuery.dictLabel = ''
  itemQuery.dictValue = ''
  itemQuery.status = ''
  refetchItems()
}

/**
 * 打开字典类型编辑弹窗，只在用户点击时加载详情。
 */
function openEditTypeDialog(id: DictEntityId) {
  editingTypeId.value = id
  isEditTypeDialogOpen.value = true
}

/**
 * 关闭字典类型编辑弹窗，并清理当前编辑上下文。
 */
function closeEditTypeDialog() {
  isEditTypeDialogOpen.value = false
  editingTypeId.value = null
}

/**
 * 打开字典明细编辑弹窗，只在用户点击时加载详情。
 */
function openEditItemDialog(id: DictEntityId) {
  editingItemId.value = id
  isEditItemDialogOpen.value = true
}

/**
 * 关闭字典明细编辑弹窗，并清理当前编辑上下文。
 */
function closeEditItemDialog() {
  isEditItemDialogOpen.value = false
  editingItemId.value = null
}

/**
 * 打开字典类型删除确认框，并保留当前待删对象。
 */
function openDeleteTypeDialog(item: DictTypeItem) {
  deletingType.value = item
  isDeleteTypeDialogOpen.value = true
}

/**
 * 关闭字典类型删除确认框。
 */
function closeDeleteTypeDialog() {
  isDeleteTypeDialogOpen.value = false
  deletingType.value = null
}

/**
 * 打开字典明细删除确认框，并保留当前待删对象。
 */
function openDeleteItemDialog(item: DictItemItem) {
  deletingItem.value = item
  isDeleteItemDialogOpen.value = true
}

/**
 * 关闭字典明细删除确认框。
 */
function closeDeleteItemDialog() {
  isDeleteItemDialogOpen.value = false
  deletingItem.value = null
}

/**
 * 删除字典类型。
 */
async function handleDeleteType() {
  const targetType = deletingType.value
  if (!targetType?.id) {
    return
  }
  try {
    await deleteDictType(targetType.id)
    toast.success(t('pages.dicts.typeDeleteSuccess'))
    closeDeleteTypeDialog()
    refetchTypes()
  }
  catch (error: any) {
    const message = error?.data?.message ?? error?.message ?? t('pages.dicts.typeDeleteFailed')
    toast.error(message)
  }
}

/**
 * 删除字典明细。
 */
async function handleDeleteItem() {
  const targetItem = deletingItem.value
  if (!targetItem?.id) {
    return
  }
  try {
    await deleteDictItem(targetItem.id)
    toast.success(t('pages.dicts.itemDeleteSuccess'))
    closeDeleteItemDialog()
    refetchItems()
  }
  catch (error: any) {
    const message = error?.data?.message ?? error?.message ?? t('pages.dicts.itemDeleteFailed')
    toast.error(message)
  }
}
</script>

<template>
  <BasicPage :title="t('pages.dicts.title')" :description="t('pages.dicts.description')" sticky>
    <template #actions>
      <UiButton variant="outline" :disabled="isFetchingTypes || isFetchingItems" @click="() => { refetchTypes(); if (selectedTypeId) refetchItems() }">
        <RefreshCwIcon class="mr-1 size-4" :class="{ 'animate-spin': isFetchingTypes || isFetchingItems }" />
        {{ t('actions.refresh') }}
      </UiButton>
    </template>

    <div class="grid gap-5 xl:grid-cols-[360px_minmax(0,1fr)]">
      <UiCard class="overflow-hidden border-border/70 bg-[radial-gradient(circle_at_top,_hsl(var(--muted))_0,_transparent_55%)] xl:sticky xl:top-6">
        <UiCardHeader class="space-y-4 border-b bg-gradient-to-br from-background via-background to-muted/40">
          <div class="flex items-center justify-between gap-3">
            <div>
              <UiCardTitle class="flex items-center gap-2 text-base">
                <BookMarkedIcon class="size-5 text-primary" />
                {{ t('pages.dicts.typeTitle') }}
              </UiCardTitle>
              <UiCardDescription class="mt-1">
                {{ t('pages.dicts.typeTotal', { total: typeTotal }) }}
              </UiCardDescription>
            </div>
            <DictTypeFormDialog @success="refetchTypes()" />
          </div>

          <div class="grid gap-3">
            <UiInput v-model="typeQuery.dictCode" :placeholder="t('pages.dicts.typeForm.codePlaceholder')" />
            <UiInput v-model="typeQuery.dictName" :placeholder="t('pages.dicts.typeForm.namePlaceholder')" />
            <UiSelect v-model="typeStatusFilter">
              <UiSelectTrigger class="w-full">
                <UiSelectValue :placeholder="t('pages.dicts.allStatus')" />
              </UiSelectTrigger>
              <UiSelectContent>
                <UiSelectItem value="all">
                  {{ t('pages.dicts.allStatus') }}
                </UiSelectItem>
                <UiSelectItem value="1">
                  {{ t('common.status.enabled') }}
                </UiSelectItem>
                <UiSelectItem value="0">
                  {{ t('common.status.disabled') }}
                </UiSelectItem>
              </UiSelectContent>
            </UiSelect>
            <div class="flex gap-2">
              <UiButton class="flex-1" @click="handleSearchType">
                {{ t('actions.search') }}
              </UiButton>
              <UiButton variant="outline" class="flex-1" @click="handleResetType">
                {{ t('actions.reset') }}
              </UiButton>
            </div>
          </div>
        </UiCardHeader>

        <UiCardContent class="p-0">
          <div v-if="isFetchingTypes" class="flex items-center justify-center py-12 text-sm text-muted-foreground">
            <LoaderCircleIcon class="mr-2 size-4 animate-spin" />
            {{ t('pages.dicts.loadingTypes') }}
          </div>

          <div v-else class="divide-y">
            <div
              v-for="item in typeList"
              :key="item.id"
              role="button"
              tabindex="0"
              class="w-full px-4 py-4 text-left transition duration-200 hover:bg-muted/30"
              :class="selectedTypeId === item.id
                ? 'bg-primary/5 shadow-[inset_3px_0_0_hsl(var(--primary))]'
                : 'bg-transparent'"
              @click="selectedTypeId = item.id"
              @keydown.enter.prevent="selectedTypeId = item.id"
              @keydown.space.prevent="selectedTypeId = item.id"
            >
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <div class="flex items-center gap-2">
                    <span class="font-medium tracking-[0.02em]">{{ item.dictName }}</span>
                    <UiBadge :variant="getStatusVariant(item.status)">
                      {{ item.status === 1 ? t('common.status.enabled') : t('common.status.disabled') }}
                    </UiBadge>
                  </div>
                  <div class="mt-1 text-xs text-muted-foreground">
                    {{ item.dictCode }}
                  </div>
                  <div v-if="item.remark" class="mt-2 line-clamp-2 text-xs text-muted-foreground/90">
                    {{ item.remark }}
                  </div>
                </div>
                <div class="flex shrink-0 gap-2">
                  <UiButton variant="outline" size="sm" @click.stop="openEditTypeDialog(item.id)">
                    <SquarePenIcon class="mr-1 size-4" />
                    {{ t('actions.edit') }}
                  </UiButton>
                  <UiButton variant="outline" size="sm" :disabled="isDeletingType" @click.stop="openDeleteTypeDialog(item)">
                    <Trash2Icon class="mr-1 size-4" />
                    {{ t('actions.delete') }}
                  </UiButton>
                </div>
              </div>
            </div>
            <div v-if="typeList.length === 0" class="px-4 py-10 text-center text-sm text-muted-foreground">
              {{ t('pages.dicts.emptyTypes') }}
            </div>
          </div>

          <div class="flex items-center justify-between border-t px-4 py-3 text-sm text-muted-foreground">
            <span>{{ t('pages.dicts.page', { page: typeQuery.page, totalPages: typeTotalPages }) }}</span>
            <div class="flex gap-2">
              <UiButton variant="outline" size="sm" :disabled="typeQuery.page <= 1" @click="typeQuery.page--; refetchTypes()">
                {{ t('common.previousPage') }}
              </UiButton>
              <UiButton variant="outline" size="sm" :disabled="typeQuery.page >= typeTotalPages" @click="typeQuery.page++; refetchTypes()">
                {{ t('common.nextPage') }}
              </UiButton>
            </div>
          </div>
        </UiCardContent>
      </UiCard>

      <div class="space-y-4">
        <UiCard class="overflow-hidden border-border/70 bg-gradient-to-r from-background via-background to-muted/30">
          <UiCardContent class="flex flex-col gap-4 p-5 md:flex-row md:items-end md:justify-between">
            <div class="space-y-2">
              <div class="text-xs font-medium uppercase tracking-[0.24em] text-muted-foreground">
                {{ t('pages.dicts.itemTitle') }}
              </div>
              <div v-if="selectedType" class="flex flex-wrap items-center gap-3">
                <div class="text-2xl font-semibold tracking-tight">
                  {{ selectedType.dictName }}
                </div>
                <UiBadge :variant="getStatusVariant(selectedType.status)">
                  {{ selectedType.status === 1 ? t('common.status.enabled') : t('common.status.disabled') }}
                </UiBadge>
                <code class="rounded bg-muted px-2 py-1 text-xs text-muted-foreground">
                  {{ selectedType.dictCode }}
                </code>
              </div>
              <div class="text-sm text-muted-foreground">
                {{
                  selectedType
                    ? t('pages.dicts.itemTotal', { total: itemTotal, name: selectedType.dictName })
                    : t('pages.dicts.selectTypeFirst')
                }}
              </div>
              <div v-if="selectedType?.remark" class="max-w-2xl text-sm text-muted-foreground">
                {{ selectedType.remark }}
              </div>
            </div>
            <div class="flex flex-wrap gap-2">
              <UiButton v-if="selectedTypeId" variant="outline" size="sm" @click="openEditTypeDialog(selectedTypeId)">
                <SquarePenIcon class="mr-1 size-4" />
                {{ t('actions.edit') }}
              </UiButton>
              <DictItemFormDialog :dict-type-id="selectedTypeId ?? 0" :disabled="!selectedTypeId" @success="refetchItems()" />
            </div>
          </UiCardContent>
        </UiCard>

        <UiCard class="overflow-hidden border-border/70">
          <UiCardHeader class="space-y-3 border-b bg-gradient-to-br from-background to-muted/40">
            <div class="flex items-center justify-between gap-3">
              <div>
                <UiCardTitle>{{ t('pages.dicts.itemTitle') }}</UiCardTitle>
                <UiCardDescription>
                  {{
                    selectedType
                      ? t('pages.dicts.itemTotal', { total: itemTotal, name: selectedType.dictName })
                      : t('pages.dicts.selectTypeFirst')
                  }}
                </UiCardDescription>
              </div>
            </div>

            <div class="grid gap-3 md:grid-cols-[1.2fr_1.2fr_0.8fr_auto]">
              <UiInput v-model="itemQuery.dictLabel" :placeholder="t('pages.dicts.itemForm.labelPlaceholder')" :disabled="!selectedTypeId" />
              <UiInput v-model="itemQuery.dictValue" :placeholder="t('pages.dicts.itemForm.valuePlaceholder')" :disabled="!selectedTypeId" />
              <UiSelect v-model="itemStatusFilter" :disabled="!selectedTypeId">
                <UiSelectTrigger class="w-full">
                  <UiSelectValue :placeholder="t('pages.dicts.allStatus')" />
                </UiSelectTrigger>
                <UiSelectContent>
                  <UiSelectItem value="all">
                    {{ t('pages.dicts.allStatus') }}
                  </UiSelectItem>
                  <UiSelectItem value="1">
                    {{ t('common.status.enabled') }}
                  </UiSelectItem>
                  <UiSelectItem value="0">
                    {{ t('common.status.disabled') }}
                  </UiSelectItem>
                </UiSelectContent>
              </UiSelect>
              <div class="flex gap-2">
                <UiButton class="flex-1" :disabled="!selectedTypeId" @click="handleSearchItem">
                  {{ t('actions.search') }}
                </UiButton>
                <UiButton variant="outline" class="flex-1" :disabled="!selectedTypeId" @click="handleResetItem">
                  {{ t('actions.reset') }}
                </UiButton>
              </div>
            </div>
          </UiCardHeader>

          <UiCardContent>
            <div v-if="!selectedTypeId" class="py-12 text-center text-sm text-muted-foreground">
              {{ t('pages.dicts.selectTypeFirst') }}
            </div>

            <div v-else-if="isFetchingItems" class="flex items-center justify-center py-12 text-sm text-muted-foreground">
              <LoaderCircleIcon class="mr-2 size-4 animate-spin" />
              {{ t('pages.dicts.loadingItems') }}
            </div>

            <div v-else class="overflow-x-auto rounded-xl border border-border/70">
              <table class="w-full text-sm">
                <thead class="bg-muted/50">
                  <tr class="border-b text-left">
                    <th class="px-4 py-3 font-medium">
                      {{ t('pages.dicts.columns.dictLabel') }}
                    </th>
                    <th class="px-4 py-3 font-medium">
                      {{ t('pages.dicts.columns.dictValue') }}
                    </th>
                    <th class="px-4 py-3 font-medium">
                      {{ t('pages.dicts.columns.status') }}
                    </th>
                    <th class="px-4 py-3 font-medium">
                      {{ t('pages.dicts.columns.sortOrder') }}
                    </th>
                    <th class="px-4 py-3 font-medium">
                      {{ t('pages.dicts.columns.updateTime') }}
                    </th>
                    <th class="px-4 py-3 font-medium text-right">
                      {{ t('actions.action') }}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in itemList" :key="item.id" class="border-b last:border-b-0">
                    <td class="px-4 py-3 align-top">
                      <div class="font-medium">
                        {{ item.dictLabel }}
                      </div>
                      <div v-if="item.remark" class="mt-1 line-clamp-2 text-xs text-muted-foreground">
                        {{ item.remark }}
                      </div>
                    </td>
                    <td class="px-4 py-3 align-top">
                      <div class="font-medium">
                        {{ item.dictValue }}
                      </div>
                      <div v-if="item.tagType" class="mt-1 text-xs text-muted-foreground">
                        {{ item.tagType }}
                      </div>
                    </td>
                    <td class="px-4 py-3">
                      <UiBadge :variant="getStatusVariant(item.status)">
                        {{ item.status === 1 ? t('common.status.enabled') : t('common.status.disabled') }}
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
                        <UiButton variant="outline" size="sm" @click="openEditItemDialog(item.id)">
                          <SquarePenIcon class="mr-1 size-4" />
                          {{ t('actions.edit') }}
                        </UiButton>
                        <UiButton variant="outline" size="sm" :disabled="isDeletingItem" @click.stop="openDeleteItemDialog(item)">
                          <Trash2Icon class="mr-1 size-4" />
                          {{ t('actions.delete') }}
                        </UiButton>
                      </div>
                    </td>
                  </tr>
                  <tr v-if="itemList.length === 0">
                    <td colspan="6" class="px-4 py-10 text-center text-muted-foreground">
                      {{ t('pages.dicts.emptyItems') }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div v-if="selectedTypeId" class="mt-4 flex items-center justify-between text-sm text-muted-foreground">
              <span>{{ t('pages.dicts.page', { page: itemQuery.page, totalPages: itemTotalPages }) }}</span>
              <div class="flex gap-2">
                <UiButton variant="outline" size="sm" :disabled="itemQuery.page <= 1" @click="itemQuery.page--; refetchItems()">
                  {{ t('common.previousPage') }}
                </UiButton>
                <UiButton variant="outline" size="sm" :disabled="itemQuery.page >= itemTotalPages" @click="itemQuery.page++; refetchItems()">
                  {{ t('common.nextPage') }}
                </UiButton>
              </div>
            </div>
          </UiCardContent>
        </UiCard>
      </div>
    </div>

    <DictTypeFormDialog
      v-if="editingTypeId"
      v-model:open="isEditTypeDialogOpen"
      :dict-type-id="editingTypeId"
      :hide-trigger="true"
      @success="() => { closeEditTypeDialog(); refetchTypes() }"
    />
    <DictItemFormDialog
      v-if="editingItemId && selectedTypeId"
      v-model:open="isEditItemDialogOpen"
      :dict-type-id="selectedTypeId"
      :dict-item-id="editingItemId"
      :hide-trigger="true"
      @success="() => { closeEditItemDialog(); refetchItems() }"
    />

    <UiAlertDialog :open="isDeleteTypeDialogOpen" @update:open="value => isDeleteTypeDialogOpen = value">
      <UiAlertDialogContent>
        <UiAlertDialogHeader>
          <UiAlertDialogTitle>{{ t('pages.dicts.deleteTypeTitle') }}</UiAlertDialogTitle>
          <UiAlertDialogDescription>{{ t('pages.dicts.deleteTypeDescription') }}</UiAlertDialogDescription>
        </UiAlertDialogHeader>
        <UiAlertDialogFooter>
          <UiAlertDialogCancel @click="closeDeleteTypeDialog">
            {{ t('actions.cancel') }}
          </UiAlertDialogCancel>
          <UiAlertDialogAction :disabled="isDeletingType" @click="handleDeleteType">
            {{ t('pages.dicts.confirmDeleteType') }}
          </UiAlertDialogAction>
        </UiAlertDialogFooter>
      </UiAlertDialogContent>
    </UiAlertDialog>

    <UiAlertDialog :open="isDeleteItemDialogOpen" @update:open="value => isDeleteItemDialogOpen = value">
      <UiAlertDialogContent>
        <UiAlertDialogHeader>
          <UiAlertDialogTitle>{{ t('pages.dicts.deleteItemTitle') }}</UiAlertDialogTitle>
          <UiAlertDialogDescription>{{ t('pages.dicts.deleteItemDescription') }}</UiAlertDialogDescription>
        </UiAlertDialogHeader>
        <UiAlertDialogFooter>
          <UiAlertDialogCancel @click="closeDeleteItemDialog">
            {{ t('actions.cancel') }}
          </UiAlertDialogCancel>
          <UiAlertDialogAction :disabled="isDeletingItem" @click="handleDeleteItem">
            {{ t('pages.dicts.confirmDeleteItem') }}
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
