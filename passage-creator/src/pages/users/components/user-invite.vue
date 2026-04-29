<script setup lang="ts">
import { MailPlusIcon } from '@lucide/vue'
import { useI18n } from 'vue-i18n'

import { Modal, ModalClose, ModalContent, ModalDescription, ModalFooter, ModalHeader, ModalTitle, ModalTrigger, useModal } from '@/components/prop-ui/modal'
import { Button } from '@/components/ui/button'

import UserInviteForm from './user-invite-form.vue'

const { isDesktop } = useModal()
const isOpen = ref(false)
const { t } = useI18n()
</script>

<template>
  <Modal v-model:open="isOpen">
    <ModalTrigger as-child>
      <Button variant="outline">
        <MailPlusIcon />
        {{ t('pages.users.inviteUser') }}
      </Button>
    </ModalTrigger>

    <ModalContent>
      <ModalHeader>
        <ModalTitle as-child>
          <div class="flex items-center gap-2">
            <MailPlusIcon />
            <span>{{ t('pages.users.inviteUser') }}</span>
          </div>
        </ModalTitle>
        <ModalDescription>
          {{ t('pages.users.inviteDescription') }}
        </ModalDescription>
      </ModalHeader>

      <UserInviteForm />

      <ModalFooter v-if="!isDesktop" class="pt-2">
        <ModalClose as-child>
          <Button variant="outline">
            {{ t('actions.cancel') }}
          </Button>
        </ModalClose>
      </ModalFooter>
    </ModalContent>
  </Modal>
</template>
