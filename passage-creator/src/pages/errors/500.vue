<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import Error from '@/components/custom-error.vue'

const { t } = useI18n()
</script>

<template>
  <div class="flex items-center justify-center h-screen">
    <Error
      :code="500"
      :subtitle="t('errors.serverError.subtitle')"
      :error="t('errors.serverError.description')"
    />
  </div>
</template>

<route lang="yaml">
meta:
  layout: blank
  </route>
