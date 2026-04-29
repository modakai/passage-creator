<script setup lang="ts">
import type { Table } from '@tanstack/vue-table'

import { XIcon } from '@lucide/vue'
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

import { DataTableFacetedFilter, DataTableViewOptions } from '@/components/data-table'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'

import type { User } from '../data/schema'

import { getCallTypes, getUserTypes } from '../data/data'

interface DataTableToolbarProps {
  table: Table<User>
}

const props = defineProps<DataTableToolbarProps>()

const isFiltered = computed(() => props.table.getState().columnFilters.length > 0)
const { t } = useI18n()
const callTypes = computed(() => getCallTypes())
const userTypes = computed(() => getUserTypes())
</script>

<template>
  <div class="flex items-center justify-between">
    <div class="flex items-center flex-1 space-x-2">
      <Input
        :placeholder="t('pages.users.filterPlaceholder')"
        :model-value="(table.getColumn('username')?.getFilterValue() as string) ?? ''"
        class="h-8 w-[150px] lg:w-[250px]"
        @input="table.getColumn('username')?.setFilterValue($event.target.value)"
      />
      <DataTableFacetedFilter
        v-if="table.getColumn('status')"
        :column="table.getColumn('status')"
        :title="t('pages.users.columns.status')"
        :options="callTypes"
      />
      <DataTableFacetedFilter
        v-if="table.getColumn('role')"
        :column="table.getColumn('role')"
        :title="t('pages.users.columns.role')"
        :options="userTypes"
      />

      <Button
        v-if="isFiltered"
        variant="ghost"
        class="h-8 px-2 lg:px-3"
        @click="table.resetColumnFilters()"
      >
        {{ t('actions.reset') }}
        <XIcon class="size-4 ml-2" />
      </Button>
    </div>
    <DataTableViewOptions :table="table" />
  </div>
</template>
