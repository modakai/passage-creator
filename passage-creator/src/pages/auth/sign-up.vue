<script setup lang="ts">
import { AlertCircleIcon, ArrowRightIcon, EyeIcon, EyeOffIcon, LockKeyholeIcon, UserRoundIcon } from '@lucide/vue'
import { toTypedSchema } from '@vee-validate/zod'
import { useForm } from 'vee-validate'
import { useI18n } from 'vue-i18n'
import { toast } from 'vue-sonner'

import { FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { registerUser } from '@/services/api/auth.api'

import AuthTitle from './components/auth-title.vue'
import PrivacyPolicyButton from './components/privacy-policy-button.vue'
import TermsOfServiceButton from './components/terms-of-service-button.vue'
import { createSignUpValidator } from './validators/sign-up.validator'

const router = useRouter()
const { t } = useI18n()

// 密码可见性状态：两个密码框独立控制，避免确认密码跟随主密码框误切换。
const showPassword = ref(false)
const showCheckPassword = ref(false)

// 触发一次轻微“抖动”动效，用于提示表单存在必填项未填。
const shakeForm = ref(false)
function triggerShakeForm() {
  shakeForm.value = false
  // 通过 nextTick 重置动画，确保每次点击都能重新触发。
  nextTick(() => {
    shakeForm.value = true
    window.setTimeout(() => {
      shakeForm.value = false
    }, 380)
  })
}

const signUpFormSchema = computed(() => toTypedSchema(createSignUpValidator({
  minLength: min => t('validation.minLength', { min }),
  passwordNotMatch: t('pages.signUp.passwordNotMatch'),
  required: t('validation.required'),
})))

const {
  handleSubmit,
  meta,
  validate,
} = useForm({
  validationSchema: signUpFormSchema,
  initialValues: {
    userAccount: '',
    userPassword: '',
    checkPassword: '',
  },
  validateOnMount: true,
})

const serverErrorMessage = ref('')
const loading = ref(false)

// 是否可提交：必填校验通过且不在提交中。
const canSubmit = computed(() => !loading.value && meta.value.valid)

/**
 * 禁用态点击提示：按钮 disabled 时无法触发 click，因此用覆盖层拦截并提示。
 */
async function handleDisabledSubmitClick() {
  await validate()
  triggerShakeForm()
  toast.error(t('pages.signUp.fillRequired'))
}

const handleRegister = handleSubmit(async (formValues) => {
  // 提交前清理服务端错误提示。
  serverErrorMessage.value = ''

  loading.value = true
  try {
    await registerUser({
      userAccount: formValues.userAccount,
      userPassword: formValues.userPassword,
      checkPassword: formValues.checkPassword,
    })

    toast.success(t('pages.signUp.success'))
    // 注册成功后固定跳转到统一登录页。
    await router.push('/auth/sign-in')
  }
  catch (error) {
    serverErrorMessage.value = error instanceof Error ? error.message : t('pages.signUp.failedRetry')
  }
  finally {
    loading.value = false
  }
}, () => {
  triggerShakeForm()
  toast.error(t('pages.signUp.fillRequired'))
})
</script>

<template>
  <div class="relative min-h-screen overflow-hidden bg-[linear-gradient(180deg,_#f8fbff_0%,_#f8fafc_38%,_#eef6ff_100%)] dark:bg-[linear-gradient(180deg,_#020617_0%,_#0f172a_42%,_#082f49_100%)]">
    <div class="pointer-events-none absolute inset-x-0 top-0 h-64 bg-[radial-gradient(circle_at_top,_rgba(56,189,248,0.24),_transparent_60%)]" />
    <div class="pointer-events-none absolute inset-x-0 bottom-0 h-56 bg-[radial-gradient(circle_at_bottom,_rgba(14,165,233,0.12),_transparent_60%)]" />

    <main class="relative mx-auto flex min-h-screen w-full max-w-[560px] flex-col justify-center px-4 py-10 sm:px-6">
      <div class="grid gap-4">
        <AuthTitle />
        <!--
          注册卡片沿用登录页背景层级：桌面端保持舒适宽度，小屏通过外层 px 控制不溢出。
          - max-w-[560px]：与登录页视觉宽度对齐
          - bg-white/92：保留半透明卡片，衔接统一背景渐变
        -->
        <UiCard class="w-full rounded-xl border border-slate-200/80 bg-white/92 shadow-sm backdrop-blur dark:border-slate-800/80 dark:bg-slate-950/88">
          <UiCardHeader>
            <UiCardTitle class="text-xl">
              {{ t('pages.signUp.title') }}
            </UiCardTitle>
            <UiCardDescription>
              {{ t('pages.signUp.description') }}
              {{ t('pages.signUp.hasAccount') }}
              <UiButton
                variant="link" class="px-0 text-muted-foreground"
                @click="router.push('/auth/sign-in')"
              >
                {{ t('pages.signUp.toLogin') }}
              </UiButton>
            </UiCardDescription>
          </UiCardHeader>
          <UiCardContent>
            <form class="grid gap-4" :class="{ 'sign-up-form-shake': shakeForm }" @submit="handleRegister">
              <UiAlert v-if="serverErrorMessage" variant="destructive">
                <AlertCircleIcon class="size-4" />
                <UiAlertTitle>{{ t('pages.signUp.failedTitle') }}</UiAlertTitle>
                <UiAlertDescription>{{ serverErrorMessage }}</UiAlertDescription>
              </UiAlert>

              <FormField v-slot="{ componentField, errorMessage }" name="userAccount">
                <FormItem>
                  <FormLabel>{{ t('common.account') }}</FormLabel>
                  <div class="relative">
                    <UserRoundIcon class="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
                    <FormControl>
                      <UiInput
                        type="text"
                        class="h-12 rounded-xl border-slate-200 bg-slate-50/80 pl-11 shadow-none focus-visible:bg-white dark:border-slate-800 dark:bg-slate-900/70"
                        :class="{ 'border-destructive focus-visible:ring-2 focus-visible:ring-destructive/30': !!errorMessage }"
                        :placeholder="t('pages.signUp.accountPlaceholder')"
                        v-bind="componentField"
                      />
                    </FormControl>
                  </div>
                  <FormMessage class="text-xs" />
                </FormItem>
              </FormField>

              <FormField v-slot="{ componentField, errorMessage }" name="userPassword">
                <FormItem>
                  <FormLabel>{{ t('common.password') }}</FormLabel>
                  <div class="relative">
                    <LockKeyholeIcon class="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
                    <FormControl>
                      <UiInput
                        :type="showPassword ? 'text' : 'password'"
                        class="h-12 rounded-xl border-slate-200 bg-slate-50/80 pl-11 pr-12 shadow-none focus-visible:bg-white dark:border-slate-800 dark:bg-slate-900/70"
                        :class="{ 'border-destructive focus-visible:ring-2 focus-visible:ring-destructive/30': !!errorMessage }"
                        :placeholder="t('pages.signUp.passwordPlaceholder')"
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

              <FormField v-slot="{ componentField, errorMessage }" name="checkPassword">
                <FormItem>
                  <FormLabel>{{ t('pages.signUp.confirmPassword') }}</FormLabel>
                  <div class="relative">
                    <LockKeyholeIcon class="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
                    <FormControl>
                      <UiInput
                        :type="showCheckPassword ? 'text' : 'password'"
                        class="h-12 rounded-xl border-slate-200 bg-slate-50/80 pl-11 pr-12 shadow-none focus-visible:bg-white dark:border-slate-800 dark:bg-slate-900/70"
                        :class="{ 'border-destructive focus-visible:ring-2 focus-visible:ring-destructive/30': !!errorMessage }"
                        :placeholder="t('pages.signUp.passwordPlaceholder')"
                        v-bind="componentField"
                      />
                    </FormControl>
                    <button
                      type="button"
                      class="absolute right-3 top-1/2 flex size-8 -translate-y-1/2 items-center justify-center rounded-md text-slate-400 transition-colors hover:text-slate-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring dark:hover:text-slate-200"
                      :aria-label="showCheckPassword ? '隐藏确认密码' : '查看确认密码'"
                      @click="showCheckPassword = !showCheckPassword"
                    >
                      <EyeOffIcon v-if="showCheckPassword" class="size-4" />
                      <EyeIcon v-else class="size-4" />
                    </button>
                  </div>
                  <FormMessage class="text-xs" />
                </FormItem>
              </FormField>

              <div class="relative mt-2">
                <UiButton
                  type="submit"
                  class="group h-12 w-full rounded-xl bg-slate-950 text-base hover:bg-slate-800 dark:bg-sky-500 dark:text-slate-950 dark:hover:bg-sky-400"
                  :disabled="!canSubmit"
                >
                  <UiSpinner v-if="loading" class="mr-2" />
                  <template v-else>
                    {{ t('pages.signUp.submit') }}
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
                  aria-label="form-invalid-overlay"
                  @click="handleDisabledSubmitClick"
                />
              </div>

              <UiCardDescription>
                {{ t('pages.signUp.agreePrefix') }}
                <TermsOfServiceButton />
                {{ t('pages.signUp.agreeAnd') }}
                <PrivacyPolicyButton />
              </UiCardDescription>
            </form>
          </UiCardContent>
        </UiCard>
      </div>
    </main>
  </div>
</template>

<route lang="yaml">
meta:
  layout: false
  guestOnly: true
</route>

<style scoped>
/**
 * 表单轻微抖动动效：用于提示用户当前还有必填项未填。
 */
.sign-up-form-shake {
  animation: sign-up-shake 380ms cubic-bezier(0.36, 0.07, 0.19, 0.97) both;
}

@keyframes sign-up-shake {
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
