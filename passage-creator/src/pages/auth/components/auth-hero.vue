<script setup lang="ts">
import { ArrowRightIcon, ShieldCheckIcon, SparklesIcon } from '@lucide/vue'
import { useI18n } from 'vue-i18n'

import type { AuthEntry } from '@/utils/auth-routing'

import { getAuthEntryConfig } from './auth-entry-config'

interface Props {
  entry: AuthEntry
}

const props = defineProps<Props>()
const { t } = useI18n()

// 根据入口切换左侧品牌说明区的内容。
const config = computed(() => getAuthEntryConfig(props.entry))
</script>

<template>
  <section class="relative overflow-hidden rounded-[2rem] border border-white/20 bg-slate-950 px-6 py-8 text-slate-50 shadow-2xl shadow-slate-950/20 lg:px-10 lg:py-12">
    <div class="absolute inset-0 bg-[radial-gradient(circle_at_top_left,_rgba(56,189,248,0.28),_transparent_34%),radial-gradient(circle_at_80%_20%,_rgba(14,165,233,0.18),_transparent_24%),linear-gradient(160deg,_rgba(15,23,42,1),_rgba(12,74,110,0.92)_58%,_rgba(8,47,73,1))]" />
    <div class="absolute inset-y-10 right-8 hidden w-32 rounded-full border border-white/10 bg-white/5 blur-[1px] lg:block" />

    <div class="relative flex h-full flex-col justify-between gap-8">
      <div class="space-y-6">
        <div class="inline-flex w-fit items-center gap-2 rounded-full border border-white/15 bg-white/8 px-3 py-1 text-xs tracking-[0.24em] text-sky-100 uppercase">
          <SparklesIcon class="size-3.5" />
          {{ t(config.badgeKey) }}
        </div>

        <div class="space-y-4">
          <h2 class="max-w-md text-3xl leading-tight font-semibold lg:text-4xl">
            {{ t(config.heroTitleKey) }}
          </h2>
          <p class="max-w-lg text-sm leading-7 text-slate-200/82 lg:text-base">
            {{ t(config.heroDescriptionKey) }}
          </p>
        </div>
      </div>

      <div class="grid gap-3">
        <article
          v-for="(featureKey, index) in config.featureKeys"
          :key="featureKey"
          class="flex items-start gap-3 rounded-2xl border border-white/12 bg-white/8 px-4 py-4 backdrop-blur-sm"
        >
          <div class="mt-0.5 rounded-xl bg-sky-400/18 p-2 text-sky-100">
            <ShieldCheckIcon v-if="index === 1" class="size-4" />
            <ArrowRightIcon v-else class="size-4" />
          </div>
          <p class="text-sm leading-6 text-slate-100/90">
            {{ t(featureKey) }}
          </p>
        </article>
      </div>
    </div>
  </section>
</template>
