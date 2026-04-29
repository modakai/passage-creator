<script setup lang="ts">
import { ImageIcon, LoaderCircleIcon, LockKeyholeIcon, SaveIcon, ShieldCheckIcon, UserRoundIcon } from '@lucide/vue'
import { toTypedSchema } from '@vee-validate/zod'
import { useForm } from 'vee-validate'
import { toast } from 'vue-sonner'

import ImageUpload from '@/components/prop-ui/image-upload/ImageUpload.vue'
import { FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { useAuth } from '@/composables/use-auth'
import { useUpdateMyPasswordMutation, useUpdateMyUserMutation } from '@/services/api/user.api'

import { profilePasswordValidator, profileUpdateValidator } from './validators/profile-center.validator'

const { hasAdminAccess, refreshLoginUser, session } = useAuth()

const profileFormSchema = toTypedSchema(profileUpdateValidator)
const passwordFormSchema = toTypedSchema(profilePasswordValidator)

/**
 * 当前登录用户的便捷映射，避免模板中重复判空。
 */
const currentUser = computed(() => session.value.user)

const {
  handleSubmit: handleProfileSubmit,
  resetForm: resetProfileForm,
  setFieldValue: setProfileFieldValue,
  values: profileValues,
} = useForm({
  validationSchema: profileFormSchema,
  initialValues: {
    userName: currentUser.value?.name ?? '',
    userAvatar: currentUser.value?.avatar ?? '',
  },
})

const {
  handleSubmit: handlePasswordSubmit,
  resetForm: resetPasswordForm,
} = useForm({
  validationSchema: passwordFormSchema,
  initialValues: {
    oldPassword: '',
    newPassword: '',
    checkPassword: '',
  },
})

const updateMyUserMutation = useUpdateMyUserMutation()
const updateMyPasswordMutation = useUpdateMyPasswordMutation()
const isUpdatingProfile = computed(() => updateMyUserMutation.isPending.value)
const isUpdatingPassword = computed(() => updateMyPasswordMutation.isPending.value)

/**
 * 登录态变更后同步回表单，保证页面刷新后展示最新资料。
 */
watch(
  currentUser,
  (user) => {
    resetProfileForm({
      values: {
        userName: user?.name ?? '',
        userAvatar: user?.avatar ?? '',
      },
    })
  },
  { immediate: true },
)

/**
 * 将图片上传组件返回值同步为单个头像地址。
 */
function handleAvatarChange(urls: string[]) {
  setProfileFieldValue('userAvatar', urls[0] ?? '')
}

/**
 * 获取角色展示文案。
 */
function getRoleText(role: string) {
  return role === 'admin' ? '管理员' : '普通用户'
}

const onSubmitProfile = handleProfileSubmit(async (values) => {
  try {
    await updateMyUserMutation.mutateAsync({
      userName: values.userName.trim(),
      userAvatar: values.userAvatar || '',
    })
    await refreshLoginUser()
    toast.success('个人资料已更新')
  }
  catch (error: any) {
    const message = error?.data?.message ?? error?.message ?? '个人资料更新失败'
    toast.error(message)
  }
})

const onSubmitPassword = handlePasswordSubmit(async (values) => {
  try {
    await updateMyPasswordMutation.mutateAsync(values)
    resetPasswordForm({
      values: {
        oldPassword: '',
        newPassword: '',
        checkPassword: '',
      },
    })
    toast.success('密码修改成功')
  }
  catch (error: any) {
    const message = error?.data?.message ?? error?.message ?? '密码修改失败'
    toast.error(message)
  }
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
      <div class="space-y-2">
        <h1 class="text-3xl font-semibold tracking-tight">
          个人中心
        </h1>
        <p class="text-muted-foreground">
          维护当前平台账号的头像、昵称与登录密码。
        </p>
      </div>

      <UiButton
        v-if="hasAdminAccess"
        variant="outline"
        @click="$router.push('/dashboard')"
      >
        <ShieldCheckIcon class="mr-1 size-4" />
        进入后台管理
      </UiButton>
    </div>

    <div class="grid gap-6 xl:grid-cols-[360px_minmax(0,1fr)]">
      <UiCard class="border-border/70 bg-gradient-to-br from-background via-background to-muted/40">
        <UiCardHeader>
          <UiCardTitle>账号概览</UiCardTitle>
          <UiCardDescription>当前平台登录信息和资料预览</UiCardDescription>
        </UiCardHeader>
        <UiCardContent class="space-y-5">
          <div class="flex items-center gap-4">
            <UiAvatar class="size-18 border shadow-sm">
              <UiAvatarImage :src="currentUser?.avatar || ''" :alt="currentUser?.name || '当前用户头像'" />
              <UiAvatarFallback class="text-lg font-semibold">
                {{ currentUser?.name?.slice(0, 1) || '我' }}
              </UiAvatarFallback>
            </UiAvatar>
            <div class="min-w-0 space-y-2">
              <p class="truncate text-xl font-semibold">
                {{ currentUser?.name || '未命名用户' }}
              </p>
              <p class="truncate text-sm text-muted-foreground">
                {{ currentUser?.email || '未绑定账号' }}
              </p>
              <div class="flex flex-wrap gap-2">
                <UiBadge
                  v-for="role in currentUser?.roles ?? []"
                  :key="role"
                  variant="secondary"
                >
                  {{ getRoleText(role) }}
                </UiBadge>
              </div>
            </div>
          </div>

          <div class="grid gap-3">
            <div class="rounded-xl border bg-muted/20 p-4">
              <div class="mb-2 flex items-center gap-2 text-sm font-medium">
                <UserRoundIcon class="size-4 text-primary" />
                昵称
              </div>
              <p class="text-sm text-muted-foreground">
                {{ currentUser?.name || '未设置' }}
              </p>
            </div>

            <div class="rounded-xl border bg-muted/20 p-4">
              <div class="mb-2 flex items-center gap-2 text-sm font-medium">
                <ImageIcon class="size-4 text-primary" />
                头像地址
              </div>
              <p class="break-all text-sm text-muted-foreground">
                {{ currentUser?.avatar || '未上传头像' }}
              </p>
            </div>
          </div>
        </UiCardContent>
      </UiCard>

      <div class="space-y-6">
        <UiCard class="border-border/70">
          <UiCardHeader>
            <UiCardTitle>基本资料</UiCardTitle>
            <UiCardDescription>支持修改昵称和头像，保存后会立即同步到当前登录态。</UiCardDescription>
          </UiCardHeader>
          <UiCardContent>
            <form class="space-y-6" @submit="onSubmitProfile">
              <div class="space-y-2">
                <UiLabel>头像</UiLabel>
                <ImageUpload
                  :model-value="profileValues.userAvatar ? [profileValues.userAvatar] : []"
                  tips="支持 jpeg、jpg、svg、png、webp，单张不超过 1MB"
                  @update:model-value="handleAvatarChange"
                />
              </div>

              <FormField v-slot="{ componentField }" name="userName">
                <FormItem>
                  <FormLabel>昵称</FormLabel>
                  <FormControl>
                    <Input
                      type="text"
                      maxlength="20"
                      placeholder="请输入昵称"
                      v-bind="componentField"
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              </FormField>

              <div class="grid gap-4 md:grid-cols-2">
                <div class="space-y-2">
                  <UiLabel>登录账号</UiLabel>
                  <UiInput :model-value="currentUser?.email || ''" disabled />
                </div>
                <div class="space-y-2">
                  <UiLabel>角色</UiLabel>
                  <UiInput :model-value="currentUser?.roles?.map(getRoleText).join(' / ') || '普通用户'" disabled />
                </div>
              </div>

              <div class="flex justify-end">
                <UiButton type="submit" :disabled="isUpdatingProfile">
                  <LoaderCircleIcon v-if="isUpdatingProfile" class="mr-1 size-4 animate-spin" />
                  <SaveIcon v-else class="mr-1 size-4" />
                  保存资料
                </UiButton>
              </div>
            </form>
          </UiCardContent>
        </UiCard>

        <UiCard class="border-border/70">
          <UiCardHeader>
            <UiCardTitle>修改密码</UiCardTitle>
            <UiCardDescription>请输入旧密码，并设置新的登录密码。</UiCardDescription>
          </UiCardHeader>
          <UiCardContent>
            <form class="space-y-6" @submit="onSubmitPassword">
              <FormField v-slot="{ componentField }" name="oldPassword">
                <FormItem>
                  <FormLabel>旧密码</FormLabel>
                  <FormControl>
                    <Input type="password" autocomplete="current-password" placeholder="请输入旧密码" v-bind="componentField" />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              </FormField>

              <FormField v-slot="{ componentField }" name="newPassword">
                <FormItem>
                  <FormLabel>新密码</FormLabel>
                  <FormControl>
                    <Input type="password" autocomplete="new-password" placeholder="请输入新密码" v-bind="componentField" />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              </FormField>

              <FormField v-slot="{ componentField }" name="checkPassword">
                <FormItem>
                  <FormLabel>确认新密码</FormLabel>
                  <FormControl>
                    <Input type="password" autocomplete="new-password" placeholder="请再次输入新密码" v-bind="componentField" />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              </FormField>

              <div class="flex justify-end">
                <UiButton type="submit" :disabled="isUpdatingPassword">
                  <LoaderCircleIcon v-if="isUpdatingPassword" class="mr-1 size-4 animate-spin" />
                  <LockKeyholeIcon v-else class="mr-1 size-4" />
                  更新密码
                </UiButton>
              </div>
            </form>
          </UiCardContent>
        </UiCard>
      </div>
    </div>
  </div>
</template>

<route lang="yaml">
meta:
  layout: user
  auth: true
  section: user
</route>
