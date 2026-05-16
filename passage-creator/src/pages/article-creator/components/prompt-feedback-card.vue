<script setup lang="ts">
import { CheckIcon, FrownIcon, HeartIcon, LoaderCircleIcon, MehIcon, MessageSquareTextIcon, SmileIcon, XIcon } from '@lucide/vue'
import { toast } from 'vue-sonner'

import type { PromptFeedbackRating, PromptFeedbackStage } from '@/services/types/prompt-template.type'

import { submitPromptFeedback } from '@/services/api/prompt-template.api'

/**
 * 组件入参，绑定当前任务和反馈环节。
 */
const props = defineProps<{
  taskId: string
  stage: PromptFeedbackStage
  title: string
  description?: string
}>()

/**
 * 当前用户选择的满意度评价。
 */
const selectedRating = ref<PromptFeedbackRating | null>(null)

/**
 * 四档满意度选项，图标和文案保持上下结构展示。
 */
const feedbackOptions = [
  {
    value: 'VERY_SATISFIED',
    label: '非常满意',
    icon: HeartIcon,
    activeClass: 'border-rose-200 bg-rose-50 text-rose-700 ring-1 ring-rose-200',
    iconClass: 'text-rose-500',
  },
  {
    value: 'SATISFIED',
    label: '满意',
    icon: SmileIcon,
    activeClass: 'border-emerald-200 bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200',
    iconClass: 'text-emerald-600',
  },
  {
    value: 'NEUTRAL',
    label: '一般',
    icon: MehIcon,
    activeClass: 'border-amber-200 bg-amber-50 text-amber-700 ring-1 ring-amber-200',
    iconClass: 'text-amber-600',
  },
  {
    value: 'UNSATISFIED',
    label: '不满意',
    icon: FrownIcon,
    activeClass: 'border-red-200 bg-red-50 text-red-700 ring-1 ring-red-200',
    iconClass: 'text-red-600',
  },
] as const satisfies readonly {
  value: PromptFeedbackRating
  label: string
  icon: typeof HeartIcon
  activeClass: string
  iconClass: string
}[]

/**
 * 控制弱提示是否在当前页面会话内隐藏。
 */
const isDismissed = ref(false)

/**
 * 控制提交中的按钮状态。
 */
const isSubmitting = ref(false)

/**
 * 控制提交成功后的轻量确认态。
 */
const isSubmitted = ref(false)

watch(() => [props.taskId, props.stage] as const, () => {
  // 切换任务或反馈环节时，重新展示当前环节自己的弱提示。
  selectedRating.value = null
  isDismissed.value = false
  isSubmitting.value = false
  isSubmitted.value = false
})

/**
 * 用户选择评价后立即提交，不再要求手写 rating 或备注。
 */
async function selectRating(rating: PromptFeedbackRating) {
  if (isSubmitting.value || !props.taskId) {
    return
  }
  selectedRating.value = rating
  await handleSubmit()
}

/**
 * 关闭当前反馈提示，不影响创作流程。
 */
function dismissFeedback() {
  isDismissed.value = true
}

/**
 * 提交反馈；失败只提示，不改变创作流程状态。
 */
async function handleSubmit() {
  if (!props.taskId || !selectedRating.value) {
    return
  }
  isSubmitting.value = true
  try {
    await submitPromptFeedback({
      taskId: props.taskId,
      feedbackStage: props.stage,
      rating: selectedRating.value,
    })
    isSubmitted.value = true
    toast.success('反馈已记录')
  }
  catch (error: any) {
    toast.error(error?.data?.message ?? error?.message ?? '反馈提交失败')
  }
  finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div
    v-if="!isDismissed"
    class="fixed inset-x-4 bottom-4 z-50 rounded-xl border border-emerald-100 bg-white/95 p-4 shadow-xl shadow-emerald-950/10 backdrop-blur sm:inset-x-auto sm:right-6 sm:bottom-6 sm:w-[420px]"
  >
    <div v-if="isSubmitted" class="flex items-center justify-between gap-3">
      <div class="flex items-center gap-2 text-sm text-emerald-700">
        <span class="grid size-7 place-items-center rounded-full bg-emerald-50">
          <CheckIcon class="size-4" />
        </span>
        <span>反馈已记录</span>
      </div>
      <UiButton variant="ghost" size="icon" @click="dismissFeedback">
        <XIcon class="size-4" />
      </UiButton>
    </div>

    <div v-else class="space-y-3">
      <div class="flex items-start justify-between gap-3">
        <div class="min-w-0">
          <div class="flex items-center gap-2 text-sm font-medium">
            <MessageSquareTextIcon class="size-4 text-emerald-600" />
            {{ title }}
          </div>
          <p v-if="description" class="mt-1 text-xs leading-5 text-muted-foreground">
            {{ description }}
          </p>
        </div>
        <UiButton variant="ghost" size="icon" class="shrink-0 text-muted-foreground" @click="dismissFeedback">
          <XIcon class="size-4" />
        </UiButton>
      </div>

      <div class="grid grid-cols-2 gap-2">
        <button
          v-for="option in feedbackOptions"
          :key="option.value"
          type="button"
          class="flex min-h-20 flex-col items-center justify-center gap-2 rounded-lg border border-border bg-background px-2 py-3 text-sm font-medium text-foreground transition hover:border-primary/40 hover:bg-muted/60 disabled:cursor-not-allowed disabled:opacity-60"
          :class="selectedRating === option.value ? option.activeClass : ''"
          :disabled="isSubmitting"
          @click="selectRating(option.value)"
        >
          <LoaderCircleIcon
            v-if="isSubmitting && selectedRating === option.value"
            class="size-5 animate-spin"
          />
          <component
            :is="option.icon"
            v-else
            class="size-5"
            :class="option.iconClass"
          />
          <span>{{ option.label }}</span>
        </button>
      </div>
    </div>
  </div>
</template>
