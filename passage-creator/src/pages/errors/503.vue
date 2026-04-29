<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import Error from '@/components/custom-error.vue'

const { t } = useI18n()
</script>

<template>
  <div class="flex items-center justify-center h-screen">
    <Error
      :code="503"
      :subtitle="t('errors.serviceUnavailable.subtitle')"
      :error="t('errors.serviceUnavailable.description')"
    />
  </div>
</template>

<route lang="yaml">
meta:
  layout: blank
  </route>
