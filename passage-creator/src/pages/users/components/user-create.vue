<script lang="ts" setup>
import { UserRoundPlusIcon } from '@lucide/vue'
import { useI18n } from 'vue-i18n'

import { Modal, ModalContent, ModalTrigger } from '@/components/prop-ui/modal'

import UserResource from './user-resource.vue'

const isOpen = ref(false)
const { t } = useI18n()
</script>

<template>
  <Modal v-model:open="isOpen">
    <ModalTrigger as-child>
      <UiButton>
        <UserRoundPlusIcon />
        {{ t('pages.users.createUser') }}
      </UiButton>
    </ModalTrigger>

    <ModalContent>
      <UserResource @close="isOpen = false" />
    </ModalContent>
  </Modal>
</template>
