<script lang="ts" setup>
import { useI18n } from 'vue-i18n'
import { toast } from 'vue-sonner'

import { ModalClose, ModalDescription, ModalFooter, ModalHeader, ModalTitle } from '@/components/prop-ui/modal'
import { useDeleteUserMutation } from '@/services/api/user.api'

import type { User } from '../data/schema'

import { deleteSelectedUser } from './user-delete-action'

const { user } = defineProps<{
  user: User
}>()

const emits = defineEmits<{
  (e: 'remove'): void
}>()
const { t } = useI18n()
const { mutateAsync: deleteUser, isPending } = useDeleteUserMutation()

/**
 * 确认删除后发起后端删除请求，并通知外层刷新或关闭。
 */
async function handleRemove() {
  try {
    const isDeleted = await deleteSelectedUser({
      user,
      deleteUser,
    })

    if (!isDeleted) {
      toast.error(t('pages.users.deleteFailed'))
      return
    }

    toast.success(t('pages.users.deleteSuccess'))
    emits('remove')
  }
  catch (error: any) {
    const message = error?.data?.message ?? error?.message ?? t('pages.users.deleteFailed')
    toast.error(message)
  }
}
</script>

<template>
  <div>
    <ModalHeader>
      <ModalTitle>
        {{ t('pages.users.deleteTitle', { username: user.username }) }}
      </ModalTitle>

      <ModalDescription>
        {{ t('pages.users.deleteDescription', { id: user.id }) }}
      </ModalDescription>
    </ModalHeader>

    <ModalFooter>
      <ModalClose as-child>
        <UiButton variant="outline">
          {{ t('actions.cancel') }}
        </UiButton>
      </ModalClose>

      <ModalClose as-child>
        <UiButton variant="destructive" :disabled="isPending" @click="handleRemove">
          {{ t('actions.delete') }}
        </UiButton>
      </ModalClose>
    </ModalFooter>
  </div>
</template>
