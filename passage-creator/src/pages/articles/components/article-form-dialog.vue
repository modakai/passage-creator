<script setup lang="ts">
import { LoaderCircleIcon, PlusIcon, SquarePenIcon } from '@lucide/vue'
import { toast } from 'vue-sonner'

import type { ArticleForm, ArticleStatus } from '@/services/types/article.type'

import {
  useCreateArticleMutation,
  useGetArticleDetailQuery,
  useUpdateArticleMutation,
} from '@/services/api/article.api'

/**
 * 文章表单弹窗属性。
 */
const props = defineProps<{
  articleId?: number
  hideTrigger?: boolean
}>()

/**
 * 提交成功后通知父组件刷新。
 */
const emit = defineEmits<{
  success: []
}>()

const open = defineModel<boolean>('open', { default: false })
const isEdit = computed(() => !!props.articleId)
const form = reactive<ArticleForm>({
  id: undefined,
  topic: '',
  mainTitle: '',
  subTitle: '',
  outline: '',
  content: '',
  fullContent: '',
  coverImage: '',
  images: '',
  status: 'PENDING',
  errorMessage: '',
})

const { data: detailData, isFetching: isFetchingDetail, refetch: refetchDetail } = useGetArticleDetailQuery(
  props.articleId,
  open,
)
const { mutateAsync: createArticle, isPending: isCreating } = useCreateArticleMutation()
const { mutateAsync: updateArticle, isPending: isUpdating } = useUpdateArticleMutation()
const isSubmitting = computed(() => isCreating.value || isUpdating.value)
const statusOptions: Array<{ label: string, value: ArticleStatus }> = [
  { label: '等待处理', value: 'PENDING' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '失败', value: 'FAILED' },
]

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
    // 编辑时主动拉详情，避免用列表字段覆盖长文本内容。
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
 * 使用文章详情填充表单。
 */
function fillForm(article: ArticleForm) {
  form.id = article.id
  form.topic = article.topic ?? ''
  form.mainTitle = article.mainTitle ?? ''
  form.subTitle = article.subTitle ?? ''
  form.outline = article.outline ?? ''
  form.content = article.content ?? ''
  form.fullContent = article.fullContent ?? ''
  form.coverImage = article.coverImage ?? ''
  form.images = article.images ?? ''
  form.status = article.status ?? 'PENDING'
  form.errorMessage = article.errorMessage ?? ''
}

/**
 * 重置表单，避免新增沿用上次编辑数据。
 */
function resetForm() {
  form.id = undefined
  form.topic = ''
  form.mainTitle = ''
  form.subTitle = ''
  form.outline = ''
  form.content = ''
  form.fullContent = ''
  form.coverImage = ''
  form.images = ''
  form.status = 'PENDING'
  form.errorMessage = ''
}

/**
 * 将空字符串统一转为 undefined，避免覆盖为无意义空值。
 */
function cleanOptional(value?: string) {
  return value?.trim() || undefined
}

/**
 * 提交新增或编辑文章。
 */
async function handleSubmit() {
  if (!form.topic.trim()) {
    toast.error('请输入文章选题')
    return
  }

  const payload: ArticleForm = {
    id: form.id,
    topic: form.topic.trim(),
    mainTitle: cleanOptional(form.mainTitle),
    subTitle: cleanOptional(form.subTitle),
    outline: cleanOptional(form.outline),
    content: cleanOptional(form.content),
    fullContent: cleanOptional(form.fullContent),
    coverImage: cleanOptional(form.coverImage),
    images: cleanOptional(form.images),
    status: form.status,
    errorMessage: cleanOptional(form.errorMessage),
  }

  try {
    if (isEdit.value && payload.id) {
      await updateArticle(payload)
      toast.success('文章更新成功')
    }
    else {
      await createArticle(payload)
      toast.success('文章创建成功')
    }
    open.value = false
    emit('success')
    resetForm()
  }
  catch (error: any) {
    const message = error?.data?.message ?? error?.message ?? '文章保存失败'
    toast.error(message)
  }
}
</script>

<template>
  <UiDialog v-model:open="open">
    <UiDialogTrigger v-if="!props.hideTrigger" as-child>
      <UiButton :variant="isEdit ? 'outline' : 'default'" size="sm">
        <component :is="isEdit ? SquarePenIcon : PlusIcon" class="mr-1 size-4" />
        {{ isEdit ? '编辑' : '新增文章' }}
      </UiButton>
    </UiDialogTrigger>

    <UiDialogContent class="max-h-[90vh] max-w-4xl overflow-y-auto">
      <UiDialogHeader>
        <UiDialogTitle>
          {{ isEdit ? '编辑文章' : '新增文章' }}
        </UiDialogTitle>
        <UiDialogDescription>
          维护文章选题、标题、正文、配图和状态字段。
        </UiDialogDescription>
      </UiDialogHeader>

      <div v-if="isEdit && isFetchingDetail" class="flex items-center justify-center py-10 text-sm text-muted-foreground">
        <LoaderCircleIcon class="mr-2 size-4 animate-spin" />
        正在加载文章详情...
      </div>

      <div v-else class="grid gap-4 py-2 md:grid-cols-2">
        <div class="space-y-2 md:col-span-2">
          <UiLabel>选题</UiLabel>
          <UiInput v-model="form.topic" placeholder="请输入文章选题" />
        </div>

        <div class="space-y-2">
          <UiLabel>主标题</UiLabel>
          <UiInput v-model="form.mainTitle" placeholder="请输入主标题" />
        </div>

        <div class="space-y-2">
          <UiLabel>副标题</UiLabel>
          <UiInput v-model="form.subTitle" placeholder="请输入副标题" />
        </div>

        <div class="space-y-2">
          <UiLabel>封面图 URL</UiLabel>
          <UiInput v-model="form.coverImage" placeholder="请输入封面图 URL" />
        </div>

        <div class="space-y-2">
          <UiLabel>状态</UiLabel>
          <UiSelect v-model="form.status">
            <UiSelectTrigger class="w-full">
              <UiSelectValue placeholder="请选择文章状态" />
            </UiSelectTrigger>
            <UiSelectContent>
              <UiSelectItem v-for="item in statusOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </UiSelectItem>
            </UiSelectContent>
          </UiSelect>
        </div>

        <div class="space-y-2 md:col-span-2">
          <UiLabel>大纲 JSON</UiLabel>
          <UiTextarea v-model="form.outline" class="min-h-24 font-mono text-xs" placeholder="请输入大纲 JSON" />
        </div>

        <div class="space-y-2 md:col-span-2">
          <UiLabel>正文 Markdown</UiLabel>
          <UiTextarea v-model="form.content" class="min-h-40 font-mono text-xs" placeholder="请输入正文 Markdown" />
        </div>

        <div class="space-y-2 md:col-span-2">
          <UiLabel>完整图文 Markdown</UiLabel>
          <UiTextarea v-model="form.fullContent" class="min-h-40 font-mono text-xs" placeholder="为空时后端会默认使用正文内容" />
        </div>

        <div class="space-y-2 md:col-span-2">
          <UiLabel>配图列表 JSON</UiLabel>
          <UiTextarea v-model="form.images" class="min-h-24 font-mono text-xs" placeholder="请输入配图列表 JSON" />
        </div>

        <div class="space-y-2 md:col-span-2">
          <UiLabel>错误信息</UiLabel>
          <UiTextarea v-model="form.errorMessage" placeholder="失败状态下可记录错误信息" />
        </div>
      </div>

      <UiDialogFooter>
        <UiButton variant="outline" @click="open = false">
          取消
        </UiButton>
        <UiButton :disabled="isSubmitting" @click="handleSubmit">
          <LoaderCircleIcon v-if="isSubmitting" class="mr-2 size-4 animate-spin" />
          保存修改
        </UiButton>
      </UiDialogFooter>
    </UiDialogContent>
  </UiDialog>
</template>
