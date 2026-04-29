<script setup lang="ts">
import { KeyRoundIcon, LoaderCircleIcon, RefreshCwIcon, ShieldBanIcon, ShieldCheckIcon, Trash2Icon } from '@lucide/vue'
import { useI18n } from 'vue-i18n'
import { toast } from 'vue-sonner'

import type { UserItem, UserQuery } from '@/services/types/user.type'

import { BasicPage } from '@/components/global-layout'
import {
  useDeleteUserMutation,
  useGetUserPageQuery,
  useResetUserPasswordMutation,
  useUpdateUserMutation,
} from '@/services/api/user.api'

import { deleteSelectedUser } from './components/user-delete-action'
import UserFormDialog from './components/user-form-dialog.vue'
import { canDeleteUser, canToggleUserStatus } from './components/user-protection'

const { t } = useI18n()
const query = reactive<UserQuery>({
  page: 1,
  pageSize: 10,
  userName: '',
  userRole: '',
  status: '',
})

const { data, isFetching, refetch } = useGetUserPageQuery(query)
const { mutateAsync: deleteUser, isPending: isDeleting } = useDeleteUserMutation()
const { mutateAsync: updateUser, isPending: isUpdating } = useUpdateUserMutation()
const { mutateAsync: resetPassword, isPending: isResetting } = useResetUserPasswordMutation()

/**
 * 当前列表数据。
 */
const userList = computed(() => data.value?.data?.records ?? [])
const total = computed(() => data.value?.data?.totalRow ?? 0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / query.pageSize!)))
const roleFilter = computed({
  get: () => query.userRole || 'all',
  set: value => query.userRole = value === 'all' ? '' : value,
})
const statusFilter = computed({
  get: () => query.status === '' ? 'all' : String(query.status),
  set: value => query.status = value === 'all' ? '' : Number(value),
})
const roleOptions = computed(() => [
  { label: t('pages.users.roles.user'), value: 'user' },
  { label: t('pages.users.roles.admin'), value: 'admin' },
])

const deletingUser = ref<UserItem | null>(null)
const resettingUser = ref<UserItem | null>(null)
const togglingUser = ref<UserItem | null>(null)

/**
 * 格式化时间，避免页面上出现无意义的空值。
 */
function formatTime(value?: string) {
  if (!value) {
    return t('common.emptyDash')
  }
  return new Date(value).toLocaleString()
}

/**
 * 根据状态映射标签样式。
 */
function getStatusVariant(status?: number) {
  return status === 1 ? 'default' : 'secondary'
}

/**
 * 根据角色返回国际化文案。
 */
function getRoleText(role?: string) {
  if (role === 'admin') {
    return t('pages.users.roles.admin')
  }
  return t('pages.users.roles.user')
}

/**
 * 提交查询前回到第一页。
 */
function handleSearch() {
  query.page = 1
  refetch()
}

/**
 * 重置筛选条件。
 */
function handleReset() {
  query.page = 1
  query.pageSize = 10
  query.userName = ''
  query.userRole = ''
  query.status = ''
  refetch()
}

/**
 * 切换分页。
 */
function changePage(nextPage: number) {
  query.page = Math.min(Math.max(nextPage, 1), totalPages.value)
  refetch()
}

/**
 * 删除用户。
 */
async function handleDelete() {
  const targetUser = deletingUser.value

  if (!targetUser) {
    return
  }
  if (!canDeleteUser(targetUser)) {
    toast.error(t('pages.users.protectedAdminCannotDelete'))
    deletingUser.value = null
    return
  }
  try {
    const isDeleted = await deleteSelectedUser({
      user: targetUser,
      deleteUser,
    })

    if (!isDeleted) {
      return
    }

    toast.success(t('pages.users.deleteSuccess'))
    deletingUser.value = null
    refetch()
  }
  catch (error: any) {
    const message = error?.data?.message ?? error?.message ?? t('pages.users.deleteFailed')
    toast.error(message)
  }
}

/**
 * 重置密码。
 */
async function handleResetPassword() {
  if (!resettingUser.value?.id) {
    return
  }
  try {
    await resetPassword(resettingUser.value.id)
    toast.success(t('pages.users.resetPasswordSuccess'))
    resettingUser.value = null
  }
  catch (error: any) {
    const message = error?.data?.message ?? error?.message ?? t('pages.users.resetPasswordFailed')
    toast.error(message)
  }
}

/**
 * 启用或禁用用户。
 */
async function handleToggleStatus() {
  if (!togglingUser.value?.id) {
    return
  }
  if (!canToggleUserStatus(togglingUser.value)) {
    toast.error(t('pages.users.protectedAdminCannotDisable'))
    togglingUser.value = null
    return
  }
  try {
    await updateUser({
      id: togglingUser.value.id,
      userName: togglingUser.value.userName,
      userAvatar: togglingUser.value.userAvatar,
      userProfile: togglingUser.value.userProfile,
      userRole: togglingUser.value.userRole,
      status: togglingUser.value.status === 1 ? 0 : 1,
    })
    toast.success(t('pages.users.toggleStatusSuccess'))
    togglingUser.value = null
    refetch()
  }
  catch (error: any) {
    const message = error?.data?.message ?? error?.message ?? t('pages.users.toggleStatusFailed')
    toast.error(message)
  }
}
</script>

<template>
  <BasicPage :title="t('pages.users.title')" :description="t('pages.users.description')" sticky>
    <template #actions>
      <UserFormDialog @success="refetch()" />
      <UiButton variant="outline" :disabled="isFetching" @click="refetch()">
        <RefreshCwIcon class="mr-1 size-4" :class="{ 'animate-spin': isFetching }" />
        {{ t('actions.refresh') }}
      </UiButton>
    </template>

    <div class="space-y-4">
      <UiCard class="border-border/70 bg-gradient-to-br from-background to-muted/30">
        <UiCardHeader>
          <UiCardTitle>{{ t('pages.users.filterTitle') }}</UiCardTitle>
          <UiCardDescription>{{ t('pages.users.filterDescription') }}</UiCardDescription>
        </UiCardHeader>
        <UiCardContent class="grid gap-4 md:grid-cols-4">
          <div class="space-y-2">
            <UiLabel>{{ t('pages.users.columns.userName') }}</UiLabel>
            <UiInput v-model="query.userName" :placeholder="t('pages.users.form.searchNamePlaceholder')" />
          </div>

          <div class="space-y-2">
            <UiLabel>{{ t('pages.users.columns.userRole') }}</UiLabel>
            <UiSelect v-model="roleFilter">
              <UiSelectTrigger class="w-full">
                <UiSelectValue :placeholder="t('pages.users.allRoles')" />
              </UiSelectTrigger>
              <UiSelectContent>
                <UiSelectItem value="all">
                  {{ t('pages.users.allRoles') }}
                </UiSelectItem>
                <UiSelectItem v-for="item in roleOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </UiSelectItem>
              </UiSelectContent>
            </UiSelect>
          </div>

          <div class="space-y-2">
            <UiLabel>{{ t('pages.users.columns.status') }}</UiLabel>
            <UiSelect v-model="statusFilter">
              <UiSelectTrigger class="w-full">
                <UiSelectValue :placeholder="t('pages.users.allStatus')" />
              </UiSelectTrigger>
              <UiSelectContent>
                <UiSelectItem value="all">
                  {{ t('pages.users.allStatus') }}
                </UiSelectItem>
                <UiSelectItem value="1">
                  {{ t('common.status.enabled') }}
                </UiSelectItem>
                <UiSelectItem value="0">
                  {{ t('common.status.disabled') }}
                </UiSelectItem>
              </UiSelectContent>
            </UiSelect>
          </div>

          <div class="flex items-end gap-2">
            <UiButton class="flex-1" @click="handleSearch">
              {{ t('actions.search') }}
            </UiButton>
            <UiButton variant="outline" class="flex-1" @click="handleReset">
              {{ t('actions.reset') }}
            </UiButton>
          </div>
        </UiCardContent>
      </UiCard>

      <UiCard class="overflow-hidden border-border/70">
        <UiCardHeader class="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
          <div>
            <UiCardTitle>{{ t('pages.users.listTitle') }}</UiCardTitle>
            <UiCardDescription>{{ t('pages.users.total', { total }) }}</UiCardDescription>
          </div>
        </UiCardHeader>
        <UiCardContent>
          <div v-if="isFetching" class="flex items-center justify-center py-12 text-sm text-muted-foreground">
            <LoaderCircleIcon class="mr-2 size-4 animate-spin" />
            {{ t('pages.users.loading') }}
          </div>

          <div v-else class="overflow-x-auto rounded-xl border border-border/70">
            <table class="w-full text-sm">
              <thead class="bg-muted/50">
                <tr class="border-b text-left">
                  <th class="px-4 py-3 font-medium">
                    {{ t('pages.users.columns.userAccount') }}
                  </th>
                  <th class="px-4 py-3 font-medium">
                    {{ t('pages.users.columns.userName') }}
                  </th>
                  <th class="px-4 py-3 font-medium">
                    {{ t('pages.users.columns.userRole') }}
                  </th>
                  <th class="px-4 py-3 font-medium">
                    {{ t('pages.users.columns.status') }}
                  </th>
                  <th class="px-4 py-3 font-medium">
                    {{ t('pages.users.columns.updateTime') }}
                  </th>
                  <th class="px-4 py-3 font-medium text-right">
                    {{ t('actions.action') }}
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in userList" :key="item.id" class="border-b last:border-b-0">
                  <td class="px-4 py-3 align-top">
                    <div class="font-medium">
                      {{ item.userAccount || t('common.emptyDash') }}
                    </div>
                  </td>
                  <td class="px-4 py-3 align-top">
                    <div class="font-medium">
                      {{ item.userName || t('common.emptyDash') }}
                    </div>
                    <div v-if="item.userProfile" class="mt-1 line-clamp-2 max-w-xs text-xs text-muted-foreground">
                      {{ item.userProfile }}
                    </div>
                  </td>
                  <td class="px-4 py-3 text-muted-foreground">
                    {{ getRoleText(item.userRole) }}
                  </td>
                  <td class="px-4 py-3">
                    <UiBadge :variant="getStatusVariant(item.status)">
                      {{ item.status === 1 ? t('common.status.enabled') : t('common.status.disabled') }}
                    </UiBadge>
                  </td>
                  <td class="px-4 py-3 text-muted-foreground">
                    {{ formatTime(item.updateTime) }}
                  </td>
                  <td class="px-4 py-3">
                    <div class="flex justify-end gap-2">
                      <UserFormDialog :user-id="item.id" @success="refetch()" />
                      <UiButton v-if="canToggleUserStatus(item)" variant="outline" size="sm" :disabled="isUpdating" @click="togglingUser = item">
                        <component :is="item.status === 1 ? ShieldBanIcon : ShieldCheckIcon" class="mr-1 size-4" />
                        {{ item.status === 1 ? t('pages.users.disableUser') : t('pages.users.enableUser') }}
                      </UiButton>
                      <UiButton variant="outline" size="sm" :disabled="isResetting" @click="resettingUser = item">
                        <KeyRoundIcon class="mr-1 size-4" />
                        {{ t('pages.users.resetPassword') }}
                      </UiButton>
                      <UiButton v-if="canDeleteUser(item)" variant="outline" size="sm" :disabled="isDeleting" @click="deletingUser = item">
                        <Trash2Icon class="mr-1 size-4" />
                        {{ t('actions.delete') }}
                      </UiButton>
                    </div>
                  </td>
                </tr>
                <tr v-if="userList.length === 0">
                  <td colspan="6" class="px-4 py-10 text-center text-muted-foreground">
                    {{ t('pages.users.empty') }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="mt-4 flex items-center justify-between text-sm text-muted-foreground">
            <span>{{ t('pages.users.page', { page: query.page, totalPages }) }}</span>
            <div class="flex gap-2">
              <UiButton variant="outline" size="sm" :disabled="query.page! <= 1" @click="changePage((query.page ?? 1) - 1)">
                {{ t('common.previousPage') }}
              </UiButton>
              <UiButton variant="outline" size="sm" :disabled="(query.page ?? 1) >= totalPages" @click="changePage((query.page ?? 1) + 1)">
                {{ t('common.nextPage') }}
              </UiButton>
            </div>
          </div>
        </UiCardContent>
      </UiCard>
    </div>

    <UiAlertDialog :open="deletingUser !== null" @update:open="value => !value ? deletingUser = null : undefined">
      <UiAlertDialogContent>
        <UiAlertDialogHeader>
          <UiAlertDialogTitle>{{ t('pages.users.deleteTitle') }}</UiAlertDialogTitle>
          <UiAlertDialogDescription>{{ t('pages.users.deleteDescription') }}</UiAlertDialogDescription>
        </UiAlertDialogHeader>
        <UiAlertDialogFooter>
          <UiAlertDialogCancel @click="deletingUser = null">
            {{ t('actions.cancel') }}
          </UiAlertDialogCancel>
          <UiButton variant="destructive" :disabled="isDeleting" @click="handleDelete">
            {{ t('pages.users.confirmDelete') }}
          </UiButton>
        </UiAlertDialogFooter>
      </UiAlertDialogContent>
    </UiAlertDialog>

    <UiAlertDialog :open="resettingUser !== null" @update:open="value => !value ? resettingUser = null : undefined">
      <UiAlertDialogContent>
        <UiAlertDialogHeader>
          <UiAlertDialogTitle>{{ t('pages.users.resetPasswordTitle') }}</UiAlertDialogTitle>
          <UiAlertDialogDescription>{{ t('pages.users.resetPasswordDescription') }}</UiAlertDialogDescription>
        </UiAlertDialogHeader>
        <UiAlertDialogFooter>
          <UiAlertDialogCancel @click="resettingUser = null">
            {{ t('actions.cancel') }}
          </UiAlertDialogCancel>
          <UiButton :disabled="isResetting" @click="handleResetPassword">
            {{ t('pages.users.confirmResetPassword') }}
          </UiButton>
        </UiAlertDialogFooter>
      </UiAlertDialogContent>
    </UiAlertDialog>

    <UiAlertDialog :open="togglingUser !== null" @update:open="value => !value ? togglingUser = null : undefined">
      <UiAlertDialogContent>
        <UiAlertDialogHeader>
          <UiAlertDialogTitle>{{ t('pages.users.toggleStatusTitle') }}</UiAlertDialogTitle>
          <UiAlertDialogDescription>
            {{
              togglingUser?.status === 1
                ? t('pages.users.disableDescription')
                : t('pages.users.enableDescription')
            }}
          </UiAlertDialogDescription>
        </UiAlertDialogHeader>
        <UiAlertDialogFooter>
          <UiAlertDialogCancel @click="togglingUser = null">
            {{ t('actions.cancel') }}
          </UiAlertDialogCancel>
          <UiButton :disabled="isUpdating" @click="handleToggleStatus">
            {{ togglingUser?.status === 1 ? t('pages.users.confirmDisable') : t('pages.users.confirmEnable') }}
          </UiButton>
        </UiAlertDialogFooter>
      </UiAlertDialogContent>
    </UiAlertDialog>
  </BasicPage>
</template>

<route lang="yaml">
meta:
  auth: true
</route>
