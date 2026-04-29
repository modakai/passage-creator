<script setup lang="ts">
import { MegaphoneIcon, XIcon } from '@lucide/vue'

import type { NotificationItem, NotificationReceiverType } from '@/services/types/notification.type'

import {
  useCloseAnnouncementMutation,
  useGetClientAnnouncementsQuery,
} from '@/services/api/notification.api'

const props = defineProps<{
  /**
   * 当前公告所属接收端。
   */
  receiverType: NotificationReceiverType
}>()

const activePopup = ref<NotificationItem | null>(null)
const { data, refetch } = useGetClientAnnouncementsQuery(props.receiverType)
const { mutateAsync: closeAnnouncement } = useCloseAnnouncementMutation(props.receiverType)
const announcements = computed(() => data.value?.data ?? [])

watch(announcements, (items) => {
  activePopup.value = items.find(item => item.popup === 1) ?? null
}, { immediate: true })

/**
 * 关闭公告并刷新服务端状态。
 */
async function handleClose(id: number) {
  await closeAnnouncement(id)
  activePopup.value = null
  refetch()
}
</script>

<template>
  <div v-if="announcements.length > 0" class="mb-4 rounded-lg border bg-amber-50/80 p-3 text-amber-950 dark:bg-amber-950/20 dark:text-amber-100">
    <div class="flex items-start gap-3">
      <MegaphoneIcon class="mt-0.5 size-4 shrink-0" />
      <div class="min-w-0 flex-1">
        <p class="font-medium">
          {{ announcements[0].title }}
        </p>
        <p class="mt-1 line-clamp-2 text-sm opacity-80">
          {{ announcements[0].summary || announcements[0].content }}
        </p>
      </div>
      <UiButton variant="ghost" size="icon" class="size-7" @click="handleClose(announcements[0].id)">
        <XIcon class="size-4" />
      </UiButton>
    </div>
  </div>

  <UiDialog :open="activePopup !== null" @update:open="value => !value && activePopup ? handleClose(activePopup.id) : undefined">
    <UiDialogContent>
      <UiDialogHeader>
        <UiDialogTitle>{{ activePopup?.title }}</UiDialogTitle>
        <UiDialogDescription>系统公告</UiDialogDescription>
      </UiDialogHeader>
      <div class="whitespace-pre-wrap text-sm leading-7">
        {{ activePopup?.content }}
      </div>
      <UiDialogFooter>
        <UiButton v-if="activePopup" @click="handleClose(activePopup.id)">
          我知道了
        </UiButton>
      </UiDialogFooter>
    </UiDialogContent>
  </UiDialog>
</template>
