<script setup lang="ts">
import { GlobeIcon, LoaderCircleIcon, LockKeyholeIcon, SaveIcon } from '@lucide/vue'
import { toTypedSchema } from '@vee-validate/zod'
import { useForm } from 'vee-validate'
import { useI18n } from 'vue-i18n'
import { toast } from 'vue-sonner'

import { FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { appLocale } from '@/plugins/i18n'
import { useUpdateMyPasswordMutation } from '@/services/api/user.api'

import { accountPasswordValidator, accountPreferenceValidator } from '../validators/account.validator'

const { t, locale } = useI18n()

const languageOptions = computed(() => [
  { label: t('language.option.zh'), value: 'zh' },
  { label: t('language.option.en'), value: 'en' },
])

const preferenceFormSchema = toTypedSchema(accountPreferenceValidator)
const passwordFormSchema = toTypedSchema(accountPasswordValidator)

const {
  handleSubmit: handlePreferenceSubmit,
  resetForm: resetPreferenceForm,
} = useForm({
  validationSchema: preferenceFormSchema,
  initialValues: {
    language: appLocale.value,
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

const updateMyPasswordMutation = useUpdateMyPasswordMutation()
const isUpdatingPassword = computed(() => updateMyPasswordMutation.isPending.value)

/**
 * 当外部语言被切换时，同步账户设置里的默认语言表单。
 */
watch(appLocale, (value) => {
  resetPreferenceForm({
    values: {
      language: value,
    },
  })
}, { immediate: true })

const onSubmitPreference = handlePreferenceSubmit(async (values) => {
  appLocale.value = values.language as typeof appLocale.value
  locale.value = values.language
  toast.success(t('pages.settings.account.languageSaved'))
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
    toast.success(t('pages.settings.account.passwordSaved'))
  }
  catch (error: any) {
    const message = error?.data?.message ?? error?.message ?? t('pages.settings.account.passwordSaveFailed')
    toast.error(message)
  }
})
</script>

<template>
  <div class="space-y-8">
    <div>
      <h3 class="text-lg font-medium">
        {{ t('pages.settings.account.title') }}
      </h3>
      <p class="text-sm text-muted-foreground">
        {{ t('pages.settings.account.description') }}
      </p>
    </div>

    <UiSeparator class="my-4" />

    <UiCard class="border-border/70">
      <UiCardHeader>
        <UiCardTitle class="flex items-center gap-2 text-base">
          <GlobeIcon class="size-4 text-primary" />
          {{ t('pages.settings.account.languageTitle') }}
        </UiCardTitle>
        <UiCardDescription>
          {{ t('pages.settings.account.languageDescription') }}
        </UiCardDescription>
      </UiCardHeader>
      <UiCardContent>
        <form class="space-y-6" @submit="onSubmitPreference">
          <FormField v-slot="{ componentField }" name="language">
            <FormItem>
              <FormLabel>{{ t('pages.settings.account.languageLabel') }}</FormLabel>
              <UiSelect v-bind="componentField">
                <FormControl>
                  <UiSelectTrigger class="w-full">
                    <UiSelectValue :placeholder="t('pages.settings.account.languagePlaceholder')" />
                  </UiSelectTrigger>
                </FormControl>
                <UiSelectContent>
                  <UiSelectItem
                    v-for="item in languageOptions"
                    :key="item.value"
                    :value="item.value"
                  >
                    {{ item.label }}
                  </UiSelectItem>
                </UiSelectContent>
              </UiSelect>
              <FormMessage />
            </FormItem>
          </FormField>

          <div class="flex justify-end">
            <UiButton type="submit">
              <SaveIcon class="mr-1 size-4" />
              {{ t('pages.settings.account.saveLanguage') }}
            </UiButton>
          </div>
        </form>
      </UiCardContent>
    </UiCard>

    <UiCard class="border-border/70">
      <UiCardHeader>
        <UiCardTitle class="flex items-center gap-2 text-base">
          <LockKeyholeIcon class="size-4 text-primary" />
          {{ t('pages.settings.account.passwordTitle') }}
        </UiCardTitle>
        <UiCardDescription>
          {{ t('pages.settings.account.passwordDescription') }}
        </UiCardDescription>
      </UiCardHeader>
      <UiCardContent>
        <form class="space-y-6" @submit="onSubmitPassword">
          <FormField v-slot="{ componentField }" name="oldPassword">
            <FormItem>
              <FormLabel>{{ t('pages.settings.account.oldPassword') }}</FormLabel>
              <FormControl>
                <Input
                  type="password"
                  autocomplete="current-password"
                  :placeholder="t('pages.settings.account.oldPasswordPlaceholder')"
                  v-bind="componentField"
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          </FormField>

          <FormField v-slot="{ componentField }" name="newPassword">
            <FormItem>
              <FormLabel>{{ t('pages.settings.account.newPassword') }}</FormLabel>
              <FormControl>
                <Input
                  type="password"
                  autocomplete="new-password"
                  :placeholder="t('pages.settings.account.newPasswordPlaceholder')"
                  v-bind="componentField"
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          </FormField>

          <FormField v-slot="{ componentField }" name="checkPassword">
            <FormItem>
              <FormLabel>{{ t('pages.settings.account.checkPassword') }}</FormLabel>
              <FormControl>
                <Input
                  type="password"
                  autocomplete="new-password"
                  :placeholder="t('pages.settings.account.checkPasswordPlaceholder')"
                  v-bind="componentField"
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          </FormField>

          <div class="flex justify-end">
            <UiButton type="submit" :disabled="isUpdatingPassword">
              <LoaderCircleIcon v-if="isUpdatingPassword" class="mr-1 size-4 animate-spin" />
              <SaveIcon v-else class="mr-1 size-4" />
              {{ t('pages.settings.account.savePassword') }}
            </UiButton>
          </div>
        </form>
      </UiCardContent>
    </UiCard>
  </div>
</template>
