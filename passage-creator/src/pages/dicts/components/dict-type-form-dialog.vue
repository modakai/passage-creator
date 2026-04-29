<script setup lang="ts">
import { LoaderCircleIcon, PlusIcon, SquarePenIcon } from '@lucide/vue'
import { useI18n } from 'vue-i18n'
import { toast } from 'vue-sonner'

import type { DictEntityId, DictTypeForm } from '@/services/types/dict.type'

import {
  useCreateDictTypeMutation,
  useGetDictTypeDetailQuery,
  useUpdateDictTypeMutation,
} from '@/services/api/dict.api'

const props = defineProps<{
  dictTypeId?: DictEntityId
  hideTrigger?: boolean
}>()

const emit = defineEmits<{
  success: []
}>()

const { t } = useI18n()
const open = defineModel<boolean>('open', { default: false })
const isEdit = computed(() => !!props.dictTypeId)
const form = reactive<DictTypeForm>({
  id: undefined,
  dictCode: '',
  dictName: '',
  status: 1,
  remark: '',
})

const { data: detailData, isFetching: isFetchingDetail, refetch: refetchDetail } = useGetDictTypeDetailQuery(
  props.dictTypeId,
  false,
)
const { mutateAsync: createDictType, isPending: isCreating } = useCreateDictTypeMutation()
const { mutateAsync: updateDictType, isPending: isUpdating } = useUpdateDictTypeMutation()

const statusValue = computed({
  get: () => String(form.status),
  set: value => form.status = Number(value),
})
const isSubmitting = computed(() => isCreating.value || isUpdating.value)

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
 * 使用字典类型详情填充表单，保证编辑弹窗挂载即打开时也能回显。
 */
function fillForm(data: DictTypeForm) {
  form.id = data.id
  form.dictCode = data.dictCode
  form.dictName = data.dictName
  form.status = data.status
  form.remark = data.remark ?? ''
}

/**
 * 重置到默认表单值，避免新增沿用上一条编辑数据。
 */
function resetForm() {
  form.id = undefined
  form.dictCode = ''
  form.dictName = ''
  form.status = 1
  form.remark = ''
}

/**
 * 提交字典类型新增或更新请求。
 */
async function handleSubmit() {
  if (!form.dictCode.trim()) {
    toast.error(t('pages.dicts.typeForm.codeRequired'))
    return
  }
  if (!form.dictName.trim()) {
    toast.error(t('pages.dicts.typeForm.nameRequired'))
    return
  }

  try {
    const payload: DictTypeForm = {
      id: form.id,
      dictCode: form.dictCode.trim(),
      dictName: form.dictName.trim(),
      status: form.status,
      remark: form.remark?.trim() || undefined,
    }

    if (isEdit.value && payload.id) {
      await updateDictType(payload)
      toast.success(t('pages.dicts.typeUpdateSuccess'))
    }
    else {
      await createDictType(payload)
      toast.success(t('pages.dicts.typeCreateSuccess'))
    }
    open.value = false
    emit('success')
    resetForm()
  }
  catch (error: any) {
    const message = error?.data?.message ?? error?.message ?? t('pages.dicts.typeSaveFailed')
    toast.error(message)
  }
}
</script>

<template>
  <UiDialog v-model:open="open">
    <UiDialogTrigger v-if="!props.hideTrigger" as-child>
      <UiButton :variant="isEdit ? 'outline' : 'default'" size="sm">
        <component :is="isEdit ? SquarePenIcon : PlusIcon" class="mr-1 size-4" />
        {{ isEdit ? t('actions.edit') : t('pages.dicts.createType') }}
      </UiButton>
    </UiDialogTrigger>

    <UiDialogContent class="max-w-xl">
      <UiDialogHeader>
        <UiDialogTitle>
          {{ isEdit ? t('pages.dicts.editTypeTitle') : t('pages.dicts.createTypeTitle') }}
        </UiDialogTitle>
        <UiDialogDescription>
          {{ t('pages.dicts.typeForm.description') }}
        </UiDialogDescription>
      </UiDialogHeader>

      <div v-if="isEdit && isFetchingDetail" class="flex items-center justify-center py-10 text-sm text-muted-foreground">
        <LoaderCircleIcon class="mr-2 size-4 animate-spin" />
        {{ t('pages.dicts.loadingTypeDetail') }}
      </div>

      <div v-else class="grid gap-4 py-2">
        <div class="space-y-2">
          <UiLabel>{{ t('pages.dicts.columns.dictCode') }}</UiLabel>
          <!-- 字典编码创建后作为稳定标识，编辑时不允许修改。 -->
          <UiInput v-model="form.dictCode" :disabled="isEdit" :placeholder="t('pages.dicts.typeForm.codePlaceholder')" />
        </div>

        <div class="space-y-2">
          <UiLabel>{{ t('pages.dicts.columns.dictName') }}</UiLabel>
          <UiInput v-model="form.dictName" :placeholder="t('pages.dicts.typeForm.namePlaceholder')" />
        </div>

        <div class="space-y-2">
          <UiLabel>{{ t('pages.dicts.columns.status') }}</UiLabel>
          <UiSelect v-model="statusValue">
            <UiSelectTrigger class="w-full">
              <UiSelectValue :placeholder="t('pages.dicts.typeForm.statusPlaceholder')" />
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
          <UiLabel>{{ t('pages.dicts.columns.remark') }}</UiLabel>
          <UiTextarea v-model="form.remark" :placeholder="t('pages.dicts.typeForm.remarkPlaceholder')" />
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
