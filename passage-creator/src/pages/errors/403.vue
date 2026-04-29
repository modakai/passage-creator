<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import Error from '@/components/custom-error.vue'

const { t } = useI18n()
</script>

<template>
  <div class="flex items-center justify-center h-screen">
    <Error
      :code="403"
      :subtitle="t('errors.forbidden.subtitle')"
      :error="t('errors.forbidden.description')"
    />
  </div>
</template>

<route lang="yaml">
meta:
  layout: blank
  </route>
