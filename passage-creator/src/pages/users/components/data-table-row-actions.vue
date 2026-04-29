<script setup lang="ts">
import type { Row } from '@tanstack/vue-table'
import type { Component } from 'vue'

import { EllipsisIcon } from '@lucide/vue'
import { useI18n } from 'vue-i18n'

import { Modal, ModalContent } from '@/components/prop-ui/modal'

import type { User } from '../data/schema'

interface DataTableRowActionsProps {
  row: Row<User>
}
const props = defineProps<DataTableRowActionsProps>()
const user = computed(() => props.row.original)
const isOpen = ref(false)

const showComponent = shallowRef<Component | null>(null)
type TCommand = 'edit' | 'delete'
const { t } = useI18n()

const componentLoader: Record<TCommand, () => Promise<{ default: Component }>> = {
  edit: () => import('./user-resource.vue'),
  delete: () => import('./user-delete.vue'),
}

async function handleSelect(command: TCommand) {
  try {
    const { default: component } = await componentLoader[command]()
    showComponent.value = component
    isOpen.value = true
  }
  catch (e) {
    console.error(`Failed to load component for "${command}"`, e)
  }
}
</script>

<template>
  <Modal v-model:open="isOpen">
    <UiDropdownMenu>
      <UiDropdownMenuTrigger as-child>
        <UiButton
          variant="ghost"
          class="flex h-8 w-8 p-0 data-[state=open]:bg-muted"
        >
          <EllipsisIcon class="size-4" />
          <span class="sr-only">{{ t('actions.openMenu') }}</span>
        </UiButton>
      </UiDropdownMenuTrigger>
      <UiDropdownMenuContent align="end" class="w-[160px]">
        <UiDropdownMenuItem @click.stop="handleSelect('edit')">
          {{ t('actions.edit') }}
        </UiDropdownMenuItem>

        <UiDropdownMenuItem @click.stop="handleSelect('delete')">
          {{ t('actions.delete') }}
          <UiDropdownMenuShortcut>⌘⌫</UiDropdownMenuShortcut>
        </UiDropdownMenuItem>
      </UiDropdownMenuContent>
    </UiDropdownMenu>

    <ModalContent>
      <component :is="showComponent" :user="user" @close="isOpen = false" />
    </ModalContent>
  </Modal>
</template>
