<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import Error from '@/components/custom-error.vue'

const { t } = useI18n()
</script>

<template>
  <div class="flex items-center justify-center h-screen">
    <Error
      :code="404"
      :subtitle="t('errors.notFound.subtitle')"
      :error="t('errors.notFound.description')"
    />
  </div>
</template>

<route lang="yaml">
meta:
  layout: false
</route>
