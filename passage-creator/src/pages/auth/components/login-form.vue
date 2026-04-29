<script lang="ts" setup>
import { AlertCircleIcon, ArrowRightIcon, EyeIcon, EyeOffIcon, LockKeyholeIcon, UserRoundIcon } from '@lucide/vue'
import { toTypedSchema } from '@vee-validate/zod'
import { useForm } from 'vee-validate'
import { useI18n } from 'vue-i18n'
import { toast } from 'vue-sonner'

import type { AuthEntry } from '@/utils/auth-routing'

import { FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { useAuth } from '@/composables/use-auth'

import { createLoginValidator } from '../validators/login.validator'
import { getAuthEntryConfig } from './auth-entry-config'
import PrivacyPolicyButton from './privacy-policy-button.vue'
import TermsOfServiceButton from './terms-of-service-button.vue'

const props = withDefaults(defineProps<Props>(), {
  entry: 'admin',
  variant: 'portal',
})

interface Props {
  entry?: AuthEntry
  variant?: 'portal' | 'compact'
}

const { login, loading } = useAuth()
const { t } = useI18n()
const router = useRouter()

// 前后台共用一套表单状态，差异从入口配置中读取。
const config = computed(() => getAuthEntryConfig(props.entry))
const errorMessage = ref('')
const showPassword = ref(false)
const shakeForm = ref(false)

// 紧凑版用于居中卡片登录页，保留门户版给后台入口继续使用。
const isCompact = computed(() => props.variant === 'compact')

// 登录表单只做必填校验，长度等规则不在前端拦截。
const loginFormSchema = computed(() => toTypedSchema(createLoginValidator({
  required: t('validation.required'),
})))

const {
  handleSubmit,
  meta,
  resetForm,
  validate,
} = useForm({
  validationSchema: loginFormSchema,
  initialValues: {
    userAccount: config.value.defaultAccount,
    userPassword: '12345678',
  },
  validateOnMount: true,
})

// 是否可登录：账号、密码必填通过且当前没有提交中的请求。
const canSubmit = computed(() => !loading.value && meta.value.valid)

function triggerShakeForm() {
  shakeForm.value = false
  // 通过 nextTick 重置动画，确保每次点击禁用按钮都能重新触发。
  nextTick(() => {
    shakeForm.value = true
    window.setTimeout(() => {
      shakeForm.value = false
    }, 380)
  })
}

// 当入口切换时，同步示例账号，避免前后台残留上一次输入模板。
watch(() => props.entry, () => {
  resetForm({
    values: {
      userAccount: config.value.defaultAccount,
      userPassword: '12345678',
    },
  })
  errorMessage.value = ''
}, { immediate: true })

/**
 * 禁用态点击提示：按钮 disabled 时无法触发 click，因此用覆盖层拦截并提示。
 */
async function handleDisabledSubmitClick() {
  await validate()
  triggerShakeForm()
  toast.error(t('pages.login.fillRequired'))
}

const handleLogin = handleSubmit(async (values) => {
  errorMessage.value = ''

  try {
    await login({
      userAccount: values.userAccount,
      userPassword: values.userPassword,
      entry: props.entry,
    })
  }
  catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('pages.login.failedRetry')
  }
}, () => {
  triggerShakeForm()
  toast.error(t('pages.login.fillRequired'))
})
</script>

<template>
  <UiCard class="w-full border-0 bg-transparent shadow-none">
    <UiCardHeader :class="isCompact ? 'space-y-3 px-0 pb-4' : 'space-y-5 px-2'">
      <div v-if="!isCompact" class="inline-flex w-fit items-center rounded-full border border-sky-200 bg-sky-50 px-3 py-1 text-xs font-medium tracking-[0.2em] text-sky-700 uppercase dark:border-sky-500/25 dark:bg-sky-500/10 dark:text-sky-100">
        {{ t(config.badgeKey) }}
      </div>

      <div class="space-y-2">
        <UiCardTitle :class="isCompact ? 'text-xl font-semibold tracking-tight text-slate-950 dark:text-slate-50' : 'text-3xl font-semibold tracking-tight text-slate-950 dark:text-slate-50'">
          {{ isCompact ? t('pages.login.title') : t(config.titleKey) }}
        </UiCardTitle>
        <UiCardDescription :class="isCompact ? 'text-sm leading-6 text-slate-500 dark:text-slate-400' : 'max-w-md text-sm leading-6 text-slate-600 dark:text-slate-300'">
          {{ isCompact ? t('pages.login.description') : t(config.descriptionKey) }}
          <template v-if="isCompact">
            <span class="mx-1 text-slate-400 dark:text-slate-500">{{ t('pages.login.noAccount') }}</span>
            <UiButton
              variant="link"
              class="h-auto px-0 py-0 text-sm font-normal text-slate-600 underline-offset-4 hover:text-slate-950 dark:text-slate-300 dark:hover:text-slate-50"
              @click="router.push('/auth/sign-up')"
            >
              {{ t('pages.login.toSignUp') }}
            </UiButton>
          </template>
        </UiCardDescription>
      </div>
    </UiCardHeader>

    <UiCardContent :class="isCompact ? 'px-0 pb-0' : 'grid gap-5 px-2 pb-2'">
      <UiAlert v-if="errorMessage" variant="destructive">
        <AlertCircleIcon class="size-4" />
        <UiAlertTitle>{{ t('pages.login.failedTitle') }}</UiAlertTitle>
        <UiAlertDescription>{{ errorMessage }}</UiAlertDescription>
      </UiAlert>

      <form :class="[isCompact ? 'grid gap-4' : 'grid gap-5', { 'login-form-shake': shakeForm }]" @submit="handleLogin">
        <FormField v-slot="{ componentField, errorMessage: fieldErrorMessage }" name="userAccount">
          <FormItem>
            <FormLabel for="auth-account" :class="isCompact ? 'text-xs font-medium text-slate-950 dark:text-slate-100' : ''">
              {{ t('common.account') }}
            </FormLabel>
            <div class="relative">
              <UserRoundIcon class="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
              <FormControl>
                <UiInput
                  id="auth-account"
                  type="text"
                  class="h-12 rounded-xl border-slate-200 bg-slate-50/80 pl-11 shadow-none focus-visible:bg-white dark:border-slate-800 dark:bg-slate-900/70"
                  :class="{ 'border-destructive focus-visible:ring-2 focus-visible:ring-destructive/30': !!fieldErrorMessage }"
                  :placeholder="config.defaultAccount"
                  v-bind="componentField"
                />
              </FormControl>
            </div>
            <FormMessage class="text-xs" />
          </FormItem>
        </FormField>

        <FormField v-slot="{ componentField, errorMessage: fieldErrorMessage }" name="userPassword">
          <FormItem>
            <FormLabel for="auth-password" :class="isCompact ? 'text-xs font-medium text-slate-950 dark:text-slate-100' : ''">
              {{ t('common.password') }}
            </FormLabel>
            <div class="relative">
              <LockKeyholeIcon class="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
              <FormControl>
                <UiInput
                  id="auth-password"
                  :type="showPassword ? 'text' : 'password'"
                  class="h-12 rounded-xl border-slate-200 bg-slate-50/80 pl-11 pr-12 shadow-none focus-visible:bg-white dark:border-slate-800 dark:bg-slate-900/70"
                  :class="{ 'border-destructive focus-visible:ring-2 focus-visible:ring-destructive/30': !!fieldErrorMessage }"
                  :placeholder="t('pages.login.passwordPlaceholder')"
                  v-bind="componentField"
                />
              </FormControl>
              <button
                type="button"
                class="absolute right-3 top-1/2 flex size-8 -translate-y-1/2 items-center justify-center rounded-md text-slate-400 transition-colors hover:text-slate-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring dark:hover:text-slate-200"
                :aria-label="showPassword ? '隐藏密码' : '查看密码'"
                @click="showPassword = !showPassword"
              >
                <EyeOffIcon v-if="showPassword" class="size-4" />
                <EyeIcon v-else class="size-4" />
              </button>
            </div>
            <FormMessage class="text-xs" />
          </FormItem>
        </FormField>

        <div class="relative mt-2">
          <UiButton
            type="submit"
            :class="isCompact
              ? 'group h-12 w-full rounded-xl bg-slate-900 text-base text-white hover:bg-slate-800 disabled:bg-slate-400 dark:bg-sky-500 dark:text-slate-950 dark:hover:bg-sky-400'
              : 'group h-12 w-full rounded-xl bg-slate-950 text-base hover:bg-slate-800 dark:bg-sky-500 dark:text-slate-950 dark:hover:bg-sky-400'"
            :disabled="!canSubmit"
          >
            <UiSpinner v-if="loading" class="mr-2" />
            <template v-else>
              {{ t(config.submitKey) }}
              <ArrowRightIcon class="ml-2 size-4 transition-transform group-hover:translate-x-0.5" />
            </template>
          </UiButton>

          <!--
            禁用态点击提示：disabled 按钮不会触发 click，这里用透明覆盖层捕获交互。
            仅在“不是 loading 但表单无效”时启用，避免影响正常提交。
          -->
          <button
            v-if="!loading && !canSubmit"
            type="button"
            class="absolute inset-0 cursor-not-allowed rounded-xl bg-transparent"
            aria-label="login-form-invalid-overlay"
            @click="handleDisabledSubmitClick"
          />
        </div>

        <UiCardDescription v-if="isCompact" class="text-sm leading-6 text-slate-500 dark:text-slate-400">
          {{ t('pages.login.agreePrefix') }}
          <TermsOfServiceButton />
          {{ t('pages.login.agreeAnd') }}
          <PrivacyPolicyButton />
        </UiCardDescription>
      </form>
    </UiCardContent>
  </UiCard>
</template>

<style scoped>
/**
 * 登录表单轻微抖动动效：用于提示用户当前还有必填项未填。
 */
.login-form-shake {
  animation: login-shake 380ms cubic-bezier(0.36, 0.07, 0.19, 0.97) both;
}

@keyframes login-shake {
  10%,
  90% {
    transform: translate3d(-1px, 0, 0);
  }

  20%,
  80% {
    transform: translate3d(2px, 0, 0);
  }

  30%,
  50%,
  70% {
    transform: translate3d(-3px, 0, 0);
  }

  40%,
  60% {
    transform: translate3d(3px, 0, 0);
  }
}
</style>
