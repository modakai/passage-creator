<script setup lang="ts">
import { LoaderCircleIcon, PlusIcon, SquarePenIcon } from '@lucide/vue'
import { useI18n } from 'vue-i18n'
import { toast } from 'vue-sonner'

import type { UserAddForm, UserEntityId, UserUpdateForm } from '@/services/types/user.type'

import ImageUpload from '@/components/prop-ui/image-upload/ImageUpload.vue'
import {
  useCreateUserMutation,
  useGetUserDetailQuery,
  useUpdateUserMutation,
} from '@/services/api/user.api'

import { buildAvatarUploadModel, resolveAvatarFromUploadModel } from './user-form-avatar'

/**
 * 用户表单弹窗属性。
 */
const props = defineProps<{
  userId?: UserEntityId
}>()

/**
 * 提交成功后通知父组件刷新列表。
 */
const emit = defineEmits<{
  success: []
}>()

const { t } = useI18n()
const open = ref(false)
const isEdit = computed(() => !!props.userId)
const form = reactive<UserAddForm & Partial<UserUpdateForm> & { id?: UserEntityId }>({
  id: undefined,
  userAccount: '',
  userName: '',
  userAvatar: '',
  userProfile: '',
  userRole: 'user',
  status: 1,
})

const { data: detailData, isFetching: isFetchingDetail } = useGetUserDetailQuery(props.userId, open)
const { mutateAsync: createUser, isPending: isCreating } = useCreateUserMutation()
const { mutateAsync: updateUser, isPending: isUpdating } = useUpdateUserMutation()

/**
 * 使用字符串承接状态选择器的值。
 */
const statusValue = computed({
  get: () => String(form.status),
  set: value => form.status = Number(value),
})

/**
 * 用户角色选项。
 */
const roleOptions = computed(() => [
  { label: t('pages.users.roles.user'), value: 'user' },
  { label: t('pages.users.roles.admin'), value: 'admin' },
])

/**
 * 提交中的统一状态。
 */
const isSubmitting = computed(() => isCreating.value || isUpdating.value)
const avatarUploadModel = computed({
  get: () => buildAvatarUploadModel(form.userAvatar),
  set: urls => form.userAvatar = resolveAvatarFromUploadModel(urls),
})

watch(open, (value) => {
  if (!value) {
    return
  }
  if (!isEdit.value) {
    resetForm()
    return
  }
  if (detailData.value?.data) {
    // 编辑弹窗打开时优先回填缓存，后续查询完成会由 detailData watcher 覆盖为最新详情。
    fillForm(detailData.value.data)
  }
})

watch(detailData, (value) => {
  if (!open.value || !value?.data) {
    return
  }
  fillForm(value.data)
}, { immediate: true })

/**
 * 使用后端详情填充表单，避免列表字段不全导致误编辑。
 */
function fillForm(user: any) {
  form.id = user.id
  form.userAccount = user.userAccount ?? ''
  form.userName = user.userName ?? ''
  form.userAvatar = user.userAvatar ?? ''
  form.userProfile = user.userProfile ?? ''
  form.userRole = user.userRole ?? 'user'
  form.status = user.status ?? 1
}

/**
 * 将表单恢复到新增默认值。
 */
function resetForm() {
  form.id = undefined
  form.userAccount = ''
  form.userName = ''
  form.userAvatar = ''
  form.userProfile = ''
  form.userRole = 'user'
  form.status = 1
}

/**
 * 提交新增或编辑请求。
 */
async function handleSubmit() {
  if (!isEdit.value && !form.userAccount.trim()) {
    toast.error(t('pages.users.form.accountRequired'))
    return
  }
  if (!form.userRole?.trim()) {
    toast.error(t('pages.users.form.roleRequired'))
    return
  }

  try {
    if (isEdit.value && form.id) {
      const payload: UserUpdateForm = {
        id: form.id,
        userName: form.userName?.trim() || undefined,
        userAvatar: form.userAvatar?.trim() || undefined,
        userProfile: form.userProfile?.trim() || undefined,
        userRole: form.userRole,
        status: form.status,
      }
      await updateUser(payload)
      toast.success(t('pages.users.updateSuccess'))
    }
    else {
      const payload: UserAddForm = {
        userAccount: form.userAccount.trim(),
        userName: form.userName?.trim() || undefined,
        userAvatar: form.userAvatar?.trim() || undefined,
        userProfile: form.userProfile?.trim() || undefined,
        userRole: form.userRole,
        status: form.status,
      }
      await createUser(payload)
      toast.success(t('pages.users.createSuccess'))
    }

    open.value = false
    emit('success')
    resetForm()
  }
  catch (error: any) {
    const message = error?.data?.message ?? error?.message ?? t('pages.users.saveFailed')
    toast.error(message)
  }
}
</script>

<template>
  <UiDialog v-model:open="open">
    <UiDialogTrigger as-child>
      <UiButton :variant="isEdit ? 'outline' : 'default'" size="sm">
        <component :is="isEdit ? SquarePenIcon : PlusIcon" class="mr-1 size-4" />
        {{ isEdit ? t('actions.edit') : t('pages.users.createUser') }}
      </UiButton>
    </UiDialogTrigger>

    <UiDialogContent class="max-w-2xl">
      <UiDialogHeader>
        <UiDialogTitle>
          {{ isEdit ? t('pages.users.editTitle') : t('pages.users.createTitle') }}
        </UiDialogTitle>
        <UiDialogDescription>
          {{ t('pages.users.formDescription') }}
        </UiDialogDescription>
      </UiDialogHeader>

      <div v-if="isEdit && isFetchingDetail" class="flex items-center justify-center py-10 text-sm text-muted-foreground">
        <LoaderCircleIcon class="mr-2 size-4 animate-spin" />
        {{ t('pages.users.loadingDetail') }}
      </div>

      <div v-else class="grid gap-4 py-2 md:grid-cols-2">
        <div class="space-y-2">
          <UiLabel>{{ t('pages.users.columns.userAccount') }}</UiLabel>
          <UiInput
            v-model="form.userAccount"
            :disabled="isEdit"
            :placeholder="t('pages.users.form.accountPlaceholder')"
          />
        </div>

        <div class="space-y-2">
          <UiLabel>{{ t('pages.users.columns.userName') }}</UiLabel>
          <UiInput v-model="form.userName" :placeholder="t('pages.users.form.namePlaceholder')" />
        </div>

        <div class="space-y-2">
          <UiLabel>{{ t('pages.users.columns.userRole') }}</UiLabel>
          <UiSelect v-model="form.userRole">
            <UiSelectTrigger class="w-full">
              <UiSelectValue :placeholder="t('pages.users.form.rolePlaceholder')" />
            </UiSelectTrigger>
            <UiSelectContent>
              <UiSelectItem v-for="item in roleOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </UiSelectItem>
            </UiSelectContent>
          </UiSelect>
        </div>

        <div class="space-y-2">
          <UiLabel>{{ t('pages.users.columns.status') }}</UiLabel>
          <UiSelect v-model="statusValue">
            <UiSelectTrigger class="w-full">
              <UiSelectValue :placeholder="t('pages.users.form.statusPlaceholder')" />
            </UiSelectTrigger>
            <UiSelectContent>
              <UiSelectItem value="1">
                {{ t('common.status.enabled') }}
              </UiSelectItem>
              <UiSelectItem value="0">
                {{ t('common.status.disabled') }}
              </UiSelectItem>
            </UiSelectContent>
          </UiSelect>
        </div>

        <div class="space-y-2 md:col-span-2">
          <UiLabel>{{ t('pages.users.columns.userAvatar') }}</UiLabel>
          <ImageUpload
            v-model="avatarUploadModel"
            variant="avatar"
            :tips="t('pages.users.form.avatarPlaceholder')"
          />
        </div>

        <div class="space-y-2 md:col-span-2">
          <UiLabel>{{ t('pages.users.columns.userProfile') }}</UiLabel>
          <UiTextarea v-model="form.userProfile" :placeholder="t('pages.users.form.profilePlaceholder')" />
        </div>
      </div>

      <UiDialogFooter>
        <UiButton variant="outline" @click="open = false">
          {{ t('actions.cancel') }}
        </UiButton>
        <UiButton :disabled="isSubmitting" @click="handleSubmit">
          <LoaderCircleIcon v-if="isSubmitting" class="mr-2 size-4 animate-spin" />
          {{ t('actions.saveChanges') }}
        </UiButton>
      </UiDialogFooter>
    </UiDialogContent>
  </UiDialog>
</template>
