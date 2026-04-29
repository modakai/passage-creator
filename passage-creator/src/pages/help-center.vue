<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import { BasicPage } from '@/components/global-layout'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'

interface ISocialMediaAccount {
  title: string
  desc: string
  url: string
}

const { t } = useI18n()
const socialMediaAccounts = computed<ISocialMediaAccount[]>(() => [
  // 帮助中心先保留占位链接，后续接入项目真实仓库或文档站。
  { title: 'GitHub', desc: t('pages.help.accounts.github'), url: '' },
  { title: 'Discord', desc: t('pages.help.accounts.discord'), url: '' },
  { title: 'Bilibili', desc: t('pages.help.accounts.bilibili'), url: 'https://space.bilibili.com/104376935' },
])
</script>

<template>
  <BasicPage
    :title="t('pages.help.title')"
    :description="t('pages.help.description')"
  >
    <Alert>
      <AlertTitle>{{ t('pages.help.alertTitle') }}</AlertTitle>
      <AlertDescription>
        {{ t('pages.help.alertDesc') }}
      </AlertDescription>
    </Alert>

    <ul class="grid grid-cols-1 gap-4 mt-4 sm:grid-cols-2 lg:grid-cols-3">
      <li v-for="account in socialMediaAccounts" :key="account.title">
        <a :href="account.url || '#'" target="_blank" rel="noopener noreferrer" class="block p-4 border rounded-lg shadow-sm bg-background border-border">
          <h2 class="mb-2 text-lg font-bold">
            {{ account.title }}
          </h2>
          <p class="text-sm text-stone-500">
            {{ account.desc }}
          </p>
        </a>
      </li>
    </ul>
  </BasicPage>
</template>
