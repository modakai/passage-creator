<script setup lang="ts">
import { BellIcon, CheckCheckIcon } from '@lucide/vue'
import { toast } from 'vue-sonner'

import type { NotificationItem, NotificationReceiverType } from '@/services/types/notification.type'

import {
  useGetClientMessagesQuery,
  useGetUnreadCountQuery,
  useMarkAllNotificationsReadMutation,
  useMarkNotificationReadMutation,
} from '@/services/api/notification.api'

const props = defineProps<{
  /**
   * 当前通知入口所属接收端。
   */
  receiverType: NotificationReceiverType
}>()

const open = ref(false)
const selected = ref<NotificationItem | null>(null)
const { data: messagesData, refetch } = useGetClientMessagesQuery(props.receiverType)
const { data: countData } = useGetUnreadCountQuery(props.receiverType)
const { mutateAsync: markRead } = useMarkNotificationReadMutation(props.receiverType)
const { mutateAsync: markAllRead } = useMarkAllNotificationsReadMutation(props.receiverType)

const messages = computed(() => messagesData.value?.data ?? [])
const unreadCount = computed(() => countData.value?.data ?? 0)

/**
 * 打开通知详情并同步标记已读。
 */
async function openMessage(message: NotificationItem) {
  selected.value = message
  if (!message.read) {
    await markRead(message.id)
    refetch()
  }
}

/**
 * 批量标记当前端消息已读。
 */
async function handleReadAll() {
  await markAllRead()
  toast.success('已将当前端消息全部标记为已读')
  refetch()
}
</script>

<template>
  <UiPopover v-model:open="open">
    <UiPopoverTrigger as-child>
      <UiButton variant="outline" size="icon" class="relative">
        <BellIcon class="size-4" />
        <span
          v-if="unreadCount > 0"
          class="absolute -right-1 -top-1 grid min-w-5 place-items-center rounded-full bg-red-600 px-1 text-[10px] font-medium text-white"
        >
          {{ unreadCount > 99 ? '99+' : unreadCount }}
        </span>
      </UiButton>
    </UiPopoverTrigger>
    <UiPopoverContent align="end" class="w-96 p-0">
      <div class="flex items-center justify-between border-b px-4 py-3">
        <div>
          <p class="font-medium">
            通知消息
          </p>
          <p class="text-xs text-muted-foreground">
            {{ unreadCount }} 条未读
          </p>
        </div>
        <UiButton variant="ghost" size="sm" :disabled="messages.length === 0" @click="handleReadAll">
          <CheckCheckIcon class="mr-1 size-4" />
          全部已读
        </UiButton>
      </div>
      <UiScrollArea class="h-80">
        <button
          v-for="message in messages"
          :key="message.id"
          class="w-full border-b px-4 py-3 text-left transition-colors hover:bg-muted/70"
          @click="openMessage(message)"
        >
          <div class="flex items-start gap-3">
            <span class="mt-1 size-2 rounded-full" :class="message.read ? 'bg-muted' : 'bg-red-600'" />
            <span class="min-w-0 flex-1">
              <span class="block truncate font-medium">{{ message.title }}</span>
              <span class="mt-1 line-clamp-2 text-xs text-muted-foreground">{{ message.summary || message.content }}</span>
            </span>
          </div>
        </button>
        <div v-if="messages.length === 0" class="px-4 py-12 text-center text-sm text-muted-foreground">
          暂无通知消息
        </div>
      </UiScrollArea>
    </UiPopoverContent>
  </UiPopover>

  <UiDialog :open="selected !== null" @update:open="value => !value ? selected = null : undefined">
    <UiDialogContent>
      <UiDialogHeader>
        <UiDialogTitle>{{ selected?.title }}</UiDialogTitle>
        <UiDialogDescription>{{ selected?.publishTime ? new Date(selected.publishTime).toLocaleString() : '未发布' }}</UiDialogDescription>
      </UiDialogHeader>
      <div class="whitespace-pre-wrap text-sm leading-7">
        {{ selected?.content }}
      </div>
    </UiDialogContent>
  </UiDialog>
</template>
