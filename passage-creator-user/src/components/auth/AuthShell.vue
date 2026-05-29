<script setup lang="ts">
import AuthHeroPanel from './AuthHeroPanel.vue'

interface Props {
  title: string
  subtitle: string
  mode: 'login' | 'register'
}

defineProps<Props>()
</script>

<template>
  <div class="relative grid min-h-[calc(100vh-8rem)] place-items-center overflow-hidden py-6">
    <div class="pointer-events-none absolute inset-x-8 top-6 h-56 rounded-full bg-[radial-gradient(circle_at_center,rgba(59,130,246,.14),transparent_65%)] blur-3xl" />
    <section class="relative grid w-full max-w-6xl gap-5 lg:grid-cols-[1.05fr_minmax(390px,460px)] lg:items-stretch">
      <AuthHeroPanel :mode="mode" :subtitle="subtitle" :title="title" />

      <div class="glass-panel flex min-h-[560px] items-center rounded-[2rem] p-4 sm:p-6">
        <slot />
      </div>
    </section>
  </div>
</template>
