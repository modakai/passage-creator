<script setup lang="ts">
import { LoaderCircleIcon, PlusIcon, SquarePenIcon } from '@lucide/vue'
import { toast } from 'vue-sonner'

import type { AgreementForm, AgreementItem } from '@/services/types/agreement.type'

import {
  useCreateAgreementMutation,
  useGetAgreementDetailQuery,
  useGetAgreementTypeOptionsQuery,
  useUpdateAgreementMutation,
} from '@/services/api/agreement.api'

import AgreementRichEditor from './agreement-rich-editor.vue'

/**
 * 弹窗属性。
 */
const props = defineProps<{
  agreementId?: number
}>()

/**
 * 对外通知刷新列表。
 */
const emit = defineEmits<{
  success: []
}>()

/**
 * 控制弹窗开关。
 */
const open = ref(false)

/**
 * 通过是否存在 id 区分新增和编辑。
 */
const isEdit = computed(() => !!props.agreementId)

/**
 * 富文本表单数据。
 */
const form = reactive<AgreementForm>({
  id: undefined,
  agreementType: '',
  title: '',
  content: '',
  status: 1,
  sortOrder: 0,
  remark: '',
})

const { data: detailData, isFetching: isFetchingDetail, refetch: refetchDetail } = useGetAgreementDetailQuery(props.agreementId)
const { data: typeOptionsData } = useGetAgreementTypeOptionsQuery()
const { mutateAsync: createAgreement, isPending: isCreating } = useCreateAgreementMutation()
const { mutateAsync: updateAgreement, isPending: isUpdating } = useUpdateAgreementMutation()

/**
 * 保存操作进行中状态。
 */
const isSubmitting = computed(() => isCreating.value || isUpdating.value)

/**
 * 协议类型选项。
 */
const typeOptions = computed(() => typeOptionsData.value?.data ?? [])

/**
 * 用字符串承接选择器值，再在提交时转成数字。
 */
const statusValue = computed({
  get: () => String(form.status),
  set: value => form.status = Number(value),
})

watch(open, async (value) => {
  if (!value) {
    return
  }

  if (!isEdit.value) {
    resetForm()
    return
  }

  await refetchDetail()
})

watch(detailData, (value) => {
  if (!value?.data || !open.value) {
    return
  }
  fillForm(value.data)
}, { immediate: true })

/**
 * 将详情数据填充到表单。
 */
function fillForm(data: AgreementItem) {
  form.id = data.id
  form.agreementType = data.agreementType
  form.title = data.title
  form.content = data.content
  form.status = data.status
  form.sortOrder = data.sortOrder ?? 0
  form.remark = data.remark ?? ''
}

/**
 * 重置表单到新增默认值。
 */
function resetForm() {
  form.id = undefined
  form.agreementType = ''
  form.title = ''
  form.content = ''
  form.status = 1
  form.sortOrder = 0
  form.remark = ''
}

/**
 * 提交新增或更新请求。
 */
async function handleSubmit() {
  if (!form.agreementType.trim()) {
    toast.error('请选择协议类型')
    return
  }
  if (!form.title.trim()) {
    toast.error('请输入协议标题')
    return
  }
  if (!form.content.trim() || form.content === '<p><br></p>') {
    toast.error('请输入协议内容')
    return
  }

  try {
    if (isEdit.value && form.id) {
      await updateAgreement({ ...form, id: form.id })
      toast.success('协议更新成功')
    }
    else {
      await createAgreement({ ...form })
      toast.success('协议创建成功')
    }
    open.value = false
    emit('success')
    if (!isEdit.value) {
      resetForm()
    }
  }
  catch (error: any) {
    const message = error?.data?.message ?? error?.message ?? '保存失败'
    toast.error(message)
  }
}
</script>

<template>
  <UiDialog v-model:open="open">
    <UiDialogTrigger as-child>
      <UiButton :variant="isEdit ? 'outline' : 'default'" size="sm">
        <component :is="isEdit ? SquarePenIcon : PlusIcon" class="mr-1 size-4" />
        {{ isEdit ? '编辑' : '新增协议' }}
      </UiButton>
    </UiDialogTrigger>

    <UiDialogContent class="max-h-[92vh] w-[calc(100vw-2rem)] max-w-6xl overflow-y-auto sm:max-w-6xl">
      <UiDialogHeader>
        <UiDialogTitle>{{ isEdit ? '编辑协议' : '新增协议' }}</UiDialogTitle>
        <UiDialogDescription>
          维护当前协议的标题、类型、启用状态和富文本内容。
        </UiDialogDescription>
      </UiDialogHeader>

      <div v-if="isEdit && isFetchingDetail" class="flex items-center justify-center py-10 text-sm text-muted-foreground">
        <LoaderCircleIcon class="mr-2 size-4 animate-spin" />
        正在加载协议详情...
      </div>

      <div v-else class="space-y-5">
        <div class="grid gap-4 md:grid-cols-2">
          <div class="space-y-2">
            <UiLabel>协议类型</UiLabel>
            <UiSelect v-model="form.agreementType">
              <UiSelectTrigger class="w-full">
                <UiSelectValue placeholder="请选择协议类型" />
              </UiSelectTrigger>
              <UiSelectContent>
                <UiSelectItem v-for="item in typeOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </UiSelectItem>
              </UiSelectContent>
            </UiSelect>
          </div>

          <div class="space-y-2">
            <UiLabel>协议标题</UiLabel>
            <UiInput v-model="form.title" placeholder="请输入协议标题" />
          </div>
        </div>

        <div class="grid gap-4 md:grid-cols-3">
          <div class="space-y-2">
            <UiLabel>状态</UiLabel>
            <UiSelect v-model="statusValue">
              <UiSelectTrigger class="w-full">
                <UiSelectValue placeholder="请选择状态" />
              </UiSelectTrigger>
              <UiSelectContent>
                <UiSelectItem value="1">
                  启用
                </UiSelectItem>
                <UiSelectItem value="0">
                  禁用
                </UiSelectItem>
              </UiSelectContent>
            </UiSelect>
          </div>

          <div class="space-y-2">
            <UiLabel>排序值</UiLabel>
            <UiInput v-model.number="form.sortOrder" type="number" min="0" placeholder="0" />
          </div>

          <div class="space-y-2">
            <UiLabel>备注</UiLabel>
            <UiInput v-model="form.remark" placeholder="可选备注" />
          </div>
        </div>

        <div class="space-y-2">
          <UiLabel>协议内容</UiLabel>
          <!-- 为长协议正文预留更大的编辑区域，减少频繁滚动。 -->
          <AgreementRichEditor v-model="form.content" :height="520" />
        </div>
      </div>

      <UiDialogFooter>
        <UiButton variant="outline" @click="open = false">
          取消
        </UiButton>
        <UiButton :disabled="isSubmitting" @click="handleSubmit">
          <LoaderCircleIcon v-if="isSubmitting" class="mr-2 size-4 animate-spin" />
          保存
        </UiButton>
      </UiDialogFooter>
    </UiDialogContent>
  </UiDialog>
</template>
