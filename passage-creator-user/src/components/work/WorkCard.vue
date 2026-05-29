<script setup lang="ts">
import { FileTextIcon, WandSparklesIcon } from '@lucide/vue'

interface WorkCardItem {
  id: string
  type: 'article' | 'rednote'
  title: string
  desc?: string
  status: string
  time?: string
  coverUrl?: string
  taskId: string
}

interface Props {
  work: WorkCardItem
  statusLabel: string
  formattedTime: string
}

defineProps<Props>()
const emit = defineEmits<{
  open: [work: WorkCardItem]
}>()
</script>

<template>
  <article class="group overflow-hidden rounded-[2rem] border border-white/90 bg-white/75 shadow-xl shadow-slate-900/5 transition hover:-translate-y-1 hover:bg-white">
    <!-- 封面区优先展示真实作品图，没有图片时才回落到轻量图标占位。 -->
    <div class="relative h-44 overflow-hidden bg-slate-100">
      <img
        v-if="work.coverUrl"
        :src="work.coverUrl"
        :alt="`${work.title}封面`"
        class="size-full object-cover transition duration-500 group-hover:scale-[1.03]"
        loading="lazy"
      >
      <div v-else class="ai-gradient grid size-full place-items-center text-white">
        <FileTextIcon v-if="work.type === 'article'" class="size-12 opacity-80" />
        <WandSparklesIcon v-else class="size-12 opacity-80" />
      </div>
      <div class="pointer-events-none absolute inset-x-0 bottom-0 h-16 bg-gradient-to-t from-white/20 to-transparent" />
    </div>

    <div class="space-y-4 p-5">
      <!-- 状态与操作仍由列表页传入，卡片只负责统一的视觉呈现。 -->
      <div class="flex items-center justify-between gap-3">
        <span class="rounded-full border px-3 py-1 text-xs" :class="work.status === 'COMPLETED' ? 'border-emerald-100 bg-emerald-50 text-emerald-700' : 'border-blue-100 bg-blue-50 text-blue-700'">{{ statusLabel }}</span>
        <span class="text-xs text-slate-400">{{ formattedTime }}</span>
      </div>
      <div>
        <h2 class="line-clamp-2 text-xl font-semibold tracking-[-0.04em]">{{ work.title }}</h2>
        <p class="mt-2 line-clamp-3 min-h-12 text-sm leading-6 text-slate-500">{{ work.desc }}</p>
      </div>
      <button type="button" class="w-full rounded-2xl bg-slate-950 px-4 py-3 text-sm font-semibold text-white transition hover:bg-slate-800" @click="emit('open', work)">
        {{ work.status === 'COMPLETED' ? '查看作品' : '继续创作' }}
      </button>
    </div>
  </article>
</template>
