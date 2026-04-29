<script setup lang="ts">
import { LoaderCircleIcon, PlusIcon, SquarePenIcon } from '@lucide/vue'
import { useI18n } from 'vue-i18n'
import { toast } from 'vue-sonner'

import type { DictEntityId, DictItemForm } from '@/services/types/dict.type'

import {
  useCreateDictItemMutation,
  useGetDictItemDetailQuery,
  useUpdateDictItemMutation,
} from '@/services/api/dict.api'

const props = defineProps<{
  dictTypeId: DictEntityId
  dictItemId?: DictEntityId
  disabled?: boolean
  hideTrigger?: boolean
}>()

const emit = defineEmits<{
  success: []
}>()

const { t } = useI18n()
const open = defineModel<boolean>('open', { default: false })
const isEdit = computed(() => !!props.dictItemId)
const form = reactive<DictItemForm>({
  id: undefined,
  dictTypeId: props.dictTypeId,
  dictLabel: '',
  dictValue: '',
  sortOrder: 0,
  status: 1,
  tagType: '',
  remark: '',
  extJson: '',
})

const { data: detailData, isFetching: isFetchingDetail, refetch: refetchDetail } = useGetDictItemDetailQuery(
  props.dictItemId,
  false,
)
const { mutateAsync: createDictItem, isPending: isCreating } = useCreateDictItemMutation()
const { mutateAsync: updateDictItem, isPending: isUpdating } = useUpdateDictItemMutation()

const statusValue = computed({
  get: () => String(form.status),
  set: value => form.status = Number(value),
})
const isSubmitting = computed(() => isCreating.value || isUpdating.value)
const previewTagVariant = computed<'default' | 'secondary' | 'destructive' | 'outline'>(() => {
  const value = form.tagType?.trim()
  if (value === 'secondary' || value === 'destructive' || value === 'outline') {
    return value
  }
  return 'default'
})
const tagTypeValue = computed({
  get: () => form.tagType?.trim() || 'none',
  set: value => form.tagType = value === 'none' ? '' : value,
})
const tagTypeOptions = computed(() => [
  { label: t('pages.dicts.itemForm.tagTypeNone'), value: 'none' },
  { label: 'default', value: 'default' },
  { label: 'secondary', value: 'secondary' },
  { label: 'destructive', value: 'destructive' },
  { label: 'outline', value: 'outline' },
])

watch(() => props.dictTypeId, (value) => {
  form.dictTypeId = value
})

watch(open, async (value) => {
  if (!value) {
    return
  }
  if (!isEdit.value) {
    resetForm()
    return
  }
  const detailResult = await refetchDetail()
  if (detailResult.data?.data) {
    // 初次挂载即打开时，主动请求后的结果需要立即写入表单。
    fillForm(detailResult.data.data)
  }
}, { immediate: true })

watch(detailData, (value) => {
  if (!open.value || !value?.data) {
    return
  }
  fillForm(value.data)
}, { immediate: true })

/**
 * 使用字典明细详情填充表单，保证编辑弹窗挂载即打开时也能回显。
 */
function fillForm(data: DictItemForm) {
  form.id = data.id
  form.dictTypeId = data.dictTypeId
  form.dictLabel = data.dictLabel
  form.dictValue = data.dictValue
  form.sortOrder = data.sortOrder ?? 0
  form.status = data.status
  form.tagType = data.tagType ?? ''
  form.remark = data.remark ?? ''
  form.extJson = data.extJson ?? ''
}

/**
 * 切换类型后要重置默认表单并继续绑定当前选中的类型 id。
 */
function resetForm() {
  form.id = undefined
  form.dictTypeId = props.dictTypeId
  form.dictLabel = ''
  form.dictValue = ''
  form.sortOrder = 0
  form.status = 1
  form.tagType = ''
  form.remark = ''
  form.extJson = ''
}

/**
 * 提交字典明细。
 */
async function handleSubmit() {
  if (!form.dictTypeId) {
    toast.error(t('pages.dicts.itemForm.typeRequired'))
    return
  }
  if (!form.dictLabel.trim()) {
    toast.error(t('pages.dicts.itemForm.labelRequired'))
    return
  }
  if (!form.dictValue.trim()) {
    toast.error(t('pages.dicts.itemForm.valueRequired'))
    return
  }

  try {
    const payload: DictItemForm = {
      id: form.id,
      dictTypeId: form.dictTypeId,
      dictLabel: form.dictLabel.trim(),
      dictValue: form.dictValue.trim(),
      sortOrder: Number(form.sortOrder ?? 0),
      status: form.status,
      tagType: form.tagType?.trim() || undefined,
      remark: form.remark?.trim() || undefined,
      extJson: form.extJson?.trim() || undefined,
    }

    if (isEdit.value && payload.id) {
      await updateDictItem(payload)
      toast.success(t('pages.dicts.itemUpdateSuccess'))
    }
    else {
      await createDictItem(payload)
      toast.success(t('pages.dicts.itemCreateSuccess'))
    }
    open.value = false
    emit('success')
    resetForm()
  }
  catch (error: any) {
    const message = error?.data?.message ?? error?.message ?? t('pages.dicts.itemSaveFailed')
    toast.error(message)
  }
}
</script>

<template>
  <UiDialog v-model:open="open">
    <UiDialogTrigger v-if="!props.hideTrigger" as-child>
      <UiButton :variant="isEdit ? 'outline' : 'default'" size="sm" :disabled="disabled">
        <component :is="isEdit ? SquarePenIcon : PlusIcon" class="mr-1 size-4" />
        {{ isEdit ? t('actions.edit') : t('pages.dicts.createItem') }}
      </UiButton>
    </UiDialogTrigger>

    <UiDialogContent class="max-w-2xl">
      <UiDialogHeader>
        <UiDialogTitle>
          {{ isEdit ? t('pages.dicts.editItemTitle') : t('pages.dicts.createItemTitle') }}
        </UiDialogTitle>
        <UiDialogDescription>
          {{ t('pages.dicts.itemForm.description') }}
        </UiDialogDescription>
      </UiDialogHeader>

      <div v-if="isEdit && isFetchingDetail" class="flex items-center justify-center py-10 text-sm text-muted-foreground">
        <LoaderCircleIcon class="mr-2 size-4 animate-spin" />
        {{ t('pages.dicts.loadingItemDetail') }}
      </div>

      <div v-else class="grid gap-4 py-2 md:grid-cols-2">
        <div class="space-y-2">
          <UiLabel>{{ t('pages.dicts.columns.dictLabel') }}</UiLabel>
          <UiInput v-model="form.dictLabel" :placeholder="t('pages.dicts.itemForm.labelPlaceholder')" />
        </div>

        <div class="space-y-2">
          <UiLabel>{{ t('pages.dicts.columns.dictValue') }}</UiLabel>
          <UiInput v-model="form.dictValue" :placeholder="t('pages.dicts.itemForm.valuePlaceholder')" />
        </div>

        <div class="space-y-2">
          <UiLabel>{{ t('pages.dicts.columns.sortOrder') }}</UiLabel>
          <UiInput v-model.number="form.sortOrder" type="number" min="0" placeholder="0" />
        </div>

        <div class="space-y-2">
          <UiLabel>{{ t('pages.dicts.columns.status') }}</UiLabel>
          <UiSelect v-model="statusValue">
            <UiSelectTrigger class="w-full">
              <UiSelectValue :placeholder="t('pages.dicts.itemForm.statusPlaceholder')" />
            </UiSelectTrigger>
            <UiSelectContent>
              <UiSelectItem value="1">
                {{ t('common.status.enabled') }}
              </UiSelectItem>
              <UiSelectItem value="0">
                {{ t('common.status.disabled') }}
              </UiSelectItem>
            </UiSelectContent>
          </UiSelect>
        </div>

        <div class="space-y-2">
          <UiLabel>{{ t('pages.dicts.columns.tagType') }}</UiLabel>
          <UiSelect v-model="tagTypeValue">
            <UiSelectTrigger class="w-full">
              <UiSelectValue :placeholder="t('pages.dicts.itemForm.tagTypePlaceholder')" />
            </UiSelectTrigger>
            <UiSelectContent>
              <UiSelectItem v-for="item in tagTypeOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </UiSelectItem>
            </UiSelectContent>
          </UiSelect>
        </div>

        <div class="space-y-2">
          <UiLabel>{{ t('pages.dicts.columns.remark') }}</UiLabel>
          <UiInput v-model="form.remark" :placeholder="t('pages.dicts.itemForm.remarkPlaceholder')" />
        </div>

        <div class="space-y-2 md:col-span-2">
          <UiLabel>{{ t('pages.dicts.itemForm.tagPreview') }}</UiLabel>
          <div class="rounded-lg border border-dashed px-3 py-4">
            <UiBadge :variant="previewTagVariant">
              {{ form.dictLabel || t('pages.dicts.itemForm.tagPreviewText') }}
            </UiBadge>
          </div>
        </div>

        <div class="space-y-2 md:col-span-2">
          <UiLabel>{{ t('pages.dicts.columns.extJson') }}</UiLabel>
          <UiTextarea v-model="form.extJson" :placeholder="t('pages.dicts.itemForm.extJsonPlaceholder')" />
        </div>
      </div>

      <UiDialogFooter>
        <UiButton variant="outline" @click="open = false">
          {{ t('actions.cancel') }}
        </UiButton>
        <UiButton :disabled="isSubmitting" @click="handleSubmit">
          <LoaderCircleIcon v-if="isSubmitting" class="mr-2 size-4 animate-spin" />
          {{ t('actions.saveChanges') }}
        </UiButton>
      </UiDialogFooter>
    </UiDialogContent>
  </UiDialog>
</template>
