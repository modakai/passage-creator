<script setup lang="ts">
import { ArrowLeftIcon, LoaderCircleIcon } from '@lucide/vue'

import AgreementRichEditor from '@/pages/agreements/components/agreement-rich-editor.vue'
import { useGetPublicAgreementQuery } from '@/services/api/agreement.api'

/**
 * 从路由参数中读取协议类型编码。
 */
const route = useRoute()
const router = useRouter()
const agreementType = computed(() => String((route.params as Record<string, string | undefined>).agreementType ?? ''))

const { data, isFetching, error } = useGetPublicAgreementQuery(agreementType)

/**
 * 协议详情数据。
 */
const agreement = computed(() => data.value?.data)

/**
 * 格式化更新时间。
 */
function formatTime(value?: string) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}
</script>

<template>
  <div class="min-h-screen bg-muted/30 px-4 py-10">
    <div class="mx-auto max-w-4xl space-y-6">
      <UiButton variant="ghost" class="px-0" @click="router.back()">
        <ArrowLeftIcon class="mr-1 size-4" />
        返回
      </UiButton>

      <UiCard>
        <UiCardHeader>
          <UiCardTitle class="text-3xl">
            {{ agreement?.title ?? '协议详情' }}
          </UiCardTitle>
          <UiCardDescription>
            协议类型：{{ agreement?.agreementType ?? agreementType }} | 更新时间：{{ formatTime(agreement?.updateTime) }}
          </UiCardDescription>
        </UiCardHeader>
        <UiCardContent>
          <div v-if="isFetching" class="flex items-center justify-center py-12 text-sm text-muted-foreground">
            <LoaderCircleIcon class="mr-2 size-4 animate-spin" />
            正在加载协议内容...
          </div>

          <UiAlert v-else-if="error" variant="destructive">
            <UiAlertTitle>加载失败</UiAlertTitle>
            <UiAlertDescription>
              {{ (error as Error).message || '协议内容获取失败，请稍后再试。' }}
            </UiAlertDescription>
          </UiAlert>

          <AgreementRichEditor v-else :model-value="agreement?.content ?? ''" readonly :height="420" />
        </UiCardContent>
      </UiCard>
    </div>
  </div>
</template>

<route lang="yaml">
meta:
  layout: blank
</route>
