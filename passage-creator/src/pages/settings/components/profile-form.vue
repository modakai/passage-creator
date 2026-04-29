<script setup lang="ts">
import { ImageIcon, LoaderCircleIcon, SaveIcon, UserRoundIcon } from '@lucide/vue'
import { toTypedSchema } from '@vee-validate/zod'
import { useForm } from 'vee-validate'
import { useI18n } from 'vue-i18n'
import { toast } from 'vue-sonner'

import ImageUpload from '@/components/prop-ui/image-upload/ImageUpload.vue'
import { FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { useAuth } from '@/composables/use-auth'
import { useUpdateMyUserMutation } from '@/services/api/user.api'

import { settingsProfileValidator } from '../validators/profile.validator'

const { t } = useI18n()
const { refreshLoginUser, session } = useAuth()
const profileFormSchema = toTypedSchema(settingsProfileValidator)

/**
 * 当前登录账号资料。
 */
const currentUser = computed(() => session.value.user)

const {
  handleSubmit,
  resetForm,
  setFieldValue,
  values,
} = useForm({
  validationSchema: profileFormSchema,
  initialValues: {
    userName: currentUser.value?.name ?? '',
    userAvatar: currentUser.value?.avatar ?? '',
  },
})

const updateMyUserMutation = useUpdateMyUserMutation()
const isUpdating = computed(() => updateMyUserMutation.isPending.value)

/**
 * 后台个人资料页打开时主动同步后端最新资料，避免本地持久化登录态过旧导致回显不准。
 */
onMounted(async () => {
  try {
    await refreshLoginUser()
  }
  catch {
    // 登录态失效会由全局请求拦截和路由守卫处理，这里只避免阻断页面渲染。
  }
})

/**
 * 登录态刷新后保持设置表单和当前用户资料一致。
 */
watch(
  currentUser,
  (user) => {
    resetForm({
      values: {
        userName: user?.name ?? '',
        userAvatar: user?.avatar ?? '',
      },
    })
  },
  { immediate: true },
)

/**
 * 头像上传组件输出数组，这里只取第一张作为用户头像。
 */
function handleAvatarChange(urls: string[]) {
  setFieldValue('userAvatar', urls[0] ?? '')
}

/**
 * 当前用户角色只允许查看，角色变更应在用户管理中由管理员处理。
 */
function getRoleText(role?: string) {
  if (role === 'admin') {
    return t('pages.users.roles.admin')
  }

  return t('pages.users.roles.user')
}

const onSubmit = handleSubmit(async (formValues) => {
  try {
    await updateMyUserMutation.mutateAsync({
      userName: formValues.userName.trim(),
      userAvatar: formValues.userAvatar || '',
    })
    await refreshLoginUser()
    toast.success(t('pages.settings.profile.saveSuccess'))
  }
  catch (error: any) {
    const message = error?.data?.message ?? error?.message ?? t('pages.settings.profile.saveFailed')
    toast.error(message)
  }
})
</script>

<template>
  <div class="space-y-8">
    <div>
      <h3 class="text-lg font-medium">
        {{ t('pages.settings.profile.title') }}
      </h3>
      <p class="text-sm text-muted-foreground">
        {{ t('pages.settings.profile.description') }}
      </p>
    </div>

    <UiSeparator class="my-4" />

    <UiCard class="border-border/70">
      <UiCardHeader>
        <UiCardTitle class="flex items-center gap-2 text-base">
          <UserRoundIcon class="size-4 text-primary" />
          {{ t('pages.settings.profile.basicTitle') }}
        </UiCardTitle>
        <UiCardDescription>
          {{ t('pages.settings.profile.basicDescription') }}
        </UiCardDescription>
      </UiCardHeader>
      <UiCardContent>
        <form class="space-y-6" @submit="onSubmit">
          <div class="grid gap-6 lg:grid-cols-[240px_minmax(0,1fr)]">
            <div class="space-y-3">
              <div class="flex items-center gap-2 text-sm font-medium">
                <ImageIcon class="size-4 text-primary" />
                {{ t('pages.settings.profile.avatar') }}
              </div>
              <ImageUpload
                :model-value="values.userAvatar ? [values.userAvatar] : []"
                variant="avatar"
                :tips="t('pages.settings.profile.avatarTips')"
                @update:model-value="handleAvatarChange"
              />
            </div>

            <div class="space-y-6">
              <FormField v-slot="{ componentField }" name="userName">
                <FormItem>
                  <FormLabel>{{ t('pages.settings.profile.userName') }}</FormLabel>
                  <FormControl>
                    <Input
                      type="text"
                      maxlength="20"
                      :placeholder="t('pages.settings.profile.userNamePlaceholder')"
                      v-bind="componentField"
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              </FormField>

              <div class="grid gap-4 md:grid-cols-2">
                <div class="space-y-2">
                  <UiLabel>{{ t('pages.settings.profile.account') }}</UiLabel>
                  <UiInput :model-value="currentUser?.email || ''" disabled />
                </div>
                <div class="space-y-2">
                  <UiLabel>{{ t('pages.settings.profile.role') }}</UiLabel>
                  <UiInput :model-value="getRoleText(currentUser?.role)" disabled />
                </div>
              </div>
            </div>
          </div>

          <div class="flex justify-end">
            <UiButton type="submit" :disabled="isUpdating">
              <LoaderCircleIcon v-if="isUpdating" class="mr-1 size-4 animate-spin" />
              <SaveIcon v-else class="mr-1 size-4" />
              {{ t('pages.settings.profile.save') }}
            </UiButton>
          </div>
        </form>
      </UiCardContent>
    </UiCard>
  </div>
</template>
