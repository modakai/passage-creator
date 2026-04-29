<script setup lang="ts">
import type { HTMLAttributes } from 'vue'

import { ImagePlusIcon, LoaderCircleIcon, Trash2Icon } from '@lucide/vue'
import { toast } from 'vue-sonner'

import type { ImageUploadItem } from '@/services/types/file.type'

import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { useUploadFileMutation } from '@/services/api/file.api'

import {
  collectUploadedUrls,
  createImageUploadItem,
  getImageFileErrorMessage,
} from './helpers'

interface Props {
  modelValue?: string[]
  maxCount?: number
  disabled?: boolean
  accept?: string
  tips?: string
  variant?: 'grid' | 'avatar'
  class?: HTMLAttributes['class']
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => [],
  maxCount: 1,
  disabled: false,
  accept: '.jpeg,.jpg,.svg,.png,.webp',
  tips: '支持 jpeg、jpg、svg、png、webp 格式，单张不超过 1MB',
  variant: 'grid',
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string[]): void
}>()

const inputRef = ref<HTMLInputElement | null>(null)
const innerItems = ref<ImageUploadItem[]>([])
const objectUrlSet = new Set<string>()
const uploadMutation = useUploadFileMutation()

/**
 * 同步外部值到组件内部列表，并尽量保留上传中的占位项。
 */
watch(
  () => props.modelValue,
  (urls) => {
    const uploadingItems = innerItems.value.filter(item => item.status === 'uploading')
    const successItems = urls
      .slice(0, props.maxCount)
      .map(url => createImageUploadItem({ url, status: 'success' }))

    innerItems.value = [...successItems, ...uploadingItems]
  },
  { immediate: true, deep: true },
)

/**
 * 当前成功上传的图片数量。
 */
const successCount = computed(() => innerItems.value.filter(item => item.status === 'success').length)

/**
 * 是否存在上传中的图片。
 */
const isUploading = computed(() => innerItems.value.some(item => item.status === 'uploading'))

/**
 * 头像模式只关心第一张图，避免沿用通用网格导致个人资料页出现大块空白。
 */
const avatarItem = computed(() => innerItems.value[0])

/**
 * 是否还允许继续展示上传入口。
 */
const canAppendMore = computed(() => {
  if (props.disabled || isUploading.value) {
    return false
  }

  if (props.maxCount <= 1) {
    return true
  }

  return successCount.value < props.maxCount
})

/**
 * 打开系统文件选择器。
 */
function openFilePicker() {
  if (isUploading.value) {
    toast.warning('图片上传中，请稍候再试')
    return
  }

  if (!canAppendMore.value && props.maxCount > 1) {
    toast.error(`最多只能上传 ${props.maxCount} 张图片`)
    return
  }

  inputRef.value?.click()
}

/**
 * 清空 input，保证同一文件可重复选择。
 */
function resetInputValue() {
  if (inputRef.value) {
    inputRef.value.value = ''
  }
}

/**
 * 回收本地预览地址，避免内存泄漏。
 */
function revokeObjectUrl(url: string) {
  if (url.startsWith('blob:') && objectUrlSet.has(url)) {
    URL.revokeObjectURL(url)
    objectUrlSet.delete(url)
  }
}

/**
 * 将内部成功项同步回父组件。
 */
function emitUploadedUrls() {
  emit('update:modelValue', collectUploadedUrls(innerItems.value))
}

/**
 * 删除指定图片。
 */
function removeItem(uid: string) {
  const currentItem = innerItems.value.find(item => item.uid === uid)
  if (!currentItem || props.disabled) {
    return
  }

  revokeObjectUrl(currentItem.url)
  innerItems.value = innerItems.value.filter(item => item.uid !== uid)
  emitUploadedUrls()
}

/**
 * 处理文件选择并立即上传。
 */
async function onFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  const files = Array.from(target.files ?? [])
  if (!files.length || props.disabled) {
    resetInputValue()
    return
  }

  const availableCount = props.maxCount <= 1 ? 1 : Math.max(props.maxCount - successCount.value, 0)
  if (availableCount <= 0 && props.maxCount > 1) {
    toast.error(`最多只能上传 ${props.maxCount} 张图片`)
    resetInputValue()
    return
  }

  const selectedFiles = props.maxCount <= 1 ? [files[0]] : files.slice(0, availableCount)
  if (props.maxCount > 1 && files.length > availableCount) {
    toast.warning(`最多还能上传 ${availableCount} 张图片，已自动截取前 ${availableCount} 张`)
  }

  for (const file of selectedFiles) {
    const errorMessage = getImageFileErrorMessage(file)
    if (errorMessage) {
      toast.error(errorMessage)
      continue
    }

    const previewUrl = URL.createObjectURL(file)
    objectUrlSet.add(previewUrl)

    const uploadingItem = createImageUploadItem({
      url: previewUrl,
      status: 'uploading',
      name: file.name,
    })
    const previousSuccessItems = innerItems.value.filter(item => item.status === 'success')

    if (props.maxCount <= 1) {
      innerItems.value.forEach(item => revokeObjectUrl(item.url))
      innerItems.value = [uploadingItem]
    }
    else {
      innerItems.value = [...innerItems.value, uploadingItem]
    }

    try {
      const response = await uploadMutation.mutateAsync(file)
      const uploadedUrl = response.data

      innerItems.value = innerItems.value.map((item) => {
        if (item.uid !== uploadingItem.uid) {
          return item
        }

        revokeObjectUrl(item.url)
        return createImageUploadItem({
          uid: item.uid,
          url: uploadedUrl,
          status: 'success',
          name: file.name,
        })
      })
      emitUploadedUrls()
    }
    catch (error) {
      revokeObjectUrl(previewUrl)
      innerItems.value = props.maxCount <= 1
        ? previousSuccessItems
        : innerItems.value.filter(item => item.uid !== uploadingItem.uid)
      const message = error instanceof Error ? error.message : '图片上传失败，请稍后重试'
      toast.error(message)
    }
  }

  resetInputValue()
}

onBeforeUnmount(() => {
  innerItems.value.forEach(item => revokeObjectUrl(item.url))
})
</script>

<template>
  <div :class="cn('space-y-3', props.class)">
    <input
      ref="inputRef"
      type="file"
      class="hidden"
      :accept="props.accept"
      :multiple="props.maxCount > 1"
      :disabled="props.disabled"
      @change="onFileChange"
    >

    <div v-if="props.variant === 'avatar'" class="space-y-3">
      <div class="flex items-center justify-center gap-4">
        <button
          type="button"
          class="group relative size-28 shrink-0 overflow-hidden rounded-full border bg-background shadow-sm ring-4 ring-muted transition hover:ring-primary/30 disabled:pointer-events-none disabled:opacity-60"
          :disabled="props.disabled || isUploading"
          @click="openFilePicker"
        >
          <span class="sr-only">{{ avatarItem?.url ? '更换头像' : '上传头像' }}</span>
          <div class="size-full">
            <img
              v-if="avatarItem?.url"
              :src="avatarItem.url"
              :alt="avatarItem.name"
              class="size-full object-cover transition-transform duration-300 group-hover:scale-105"
            >
            <div
              v-else
              class="flex size-full items-center justify-center bg-muted text-muted-foreground"
            >
              <ImagePlusIcon class="size-8" />
            </div>
          </div>

          <div
            class="absolute inset-0 flex items-center justify-center rounded-full bg-black/0 text-white opacity-0 transition group-hover:bg-black/35 group-hover:opacity-100"
          >
            <div class="flex flex-col items-center gap-1 text-xs font-medium">
              <ImagePlusIcon class="size-5" />
              <span>{{ avatarItem?.url ? '更换' : '上传' }}</span>
            </div>
          </div>

          <div
            v-if="avatarItem?.status === 'uploading'"
            class="absolute inset-0 flex items-center justify-center rounded-full bg-background/70 backdrop-blur-sm"
          >
            <LoaderCircleIcon class="size-6 animate-spin text-primary" />
          </div>
        </button>

        <Button
          v-if="avatarItem?.url"
          type="button"
          size="icon-sm"
          variant="outline"
          class="rounded-full"
          :disabled="props.disabled || avatarItem.status === 'uploading'"
          @click="removeItem(avatarItem.uid)"
        >
          <Trash2Icon class="size-4" />
        </Button>
      </div>

      <div class="rounded-lg border bg-muted/20 px-3 py-2 text-xs leading-5 text-muted-foreground">
        <div class="flex items-start gap-2">
          <ImagePlusIcon class="mt-0.5 size-4 shrink-0" />
          <span>{{ props.tips }}</span>
        </div>
      </div>
    </div>

    <div v-else class="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
      <div
        v-for="item in innerItems"
        :key="item.uid"
        class="group relative overflow-hidden rounded-xl border bg-muted/30"
      >
        <div class="aspect-square overflow-hidden bg-muted">
          <img
            v-if="item.url"
            :src="item.url"
            :alt="item.name"
            class="size-full object-cover transition-transform duration-300 group-hover:scale-105"
          >
        </div>

        <div
          v-if="item.status === 'uploading'"
          class="absolute inset-0 flex flex-col items-center justify-center gap-2 bg-background/75 backdrop-blur-sm"
        >
          <LoaderCircleIcon class="size-6 animate-spin text-primary" />
          <span class="text-sm text-muted-foreground">上传中...</span>
        </div>

        <div class="absolute right-2 top-2">
          <Button
            type="button"
            size="icon-sm"
            variant="secondary"
            class="rounded-full shadow-sm"
            :disabled="props.disabled || item.status === 'uploading'"
            @click="removeItem(item.uid)"
          >
            <Trash2Icon class="size-4" />
          </Button>
        </div>

        <div class="border-t bg-background/95 px-3 py-2 text-xs text-muted-foreground">
          <p class="truncate">
            {{ item.name }}
          </p>
        </div>
      </div>

      <button
        v-if="canAppendMore"
        type="button"
        class="flex aspect-square flex-col items-center justify-center gap-3 rounded-xl border border-dashed bg-muted/30 px-4 text-center transition-colors hover:border-primary/60 hover:bg-primary/5 disabled:pointer-events-none disabled:opacity-50"
        :disabled="props.disabled"
        @click="openFilePicker"
      >
        <div class="flex size-12 items-center justify-center rounded-full bg-primary/10 text-primary">
          <ImagePlusIcon class="size-6" />
        </div>
        <div class="space-y-1">
          <p class="text-sm font-medium text-foreground">
            {{ props.maxCount <= 1 && successCount > 0 ? '重新上传图片' : '点击上传图片' }}
          </p>
          <p class="text-xs text-muted-foreground">
            {{ props.maxCount <= 1 ? '单图模式' : `最多上传 ${props.maxCount} 张` }}
          </p>
        </div>
      </button>
    </div>

    <div v-if="props.variant === 'grid'" class="rounded-lg border bg-muted/20 px-3 py-2 text-xs text-muted-foreground">
      <div class="flex items-center gap-2">
        <ImagePlusIcon class="size-4" />
        <span>{{ props.tips }}</span>
      </div>
    </div>
  </div>
</template>
