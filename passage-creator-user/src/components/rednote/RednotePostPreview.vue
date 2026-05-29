<script setup lang="ts">
import { BookmarkIcon, ChevronLeftIcon, HeartIcon, MessageCircleIcon, MoreHorizontalIcon, SendIcon } from '@lucide/vue'
import { computed } from 'vue'

interface PreviewImage {
  url?: string
  prompt?: string
}

interface Props {
  title: string
  body?: string
  context?: string
  coverTitle?: string
  coverImage?: string
  images: PreviewImage[]
  tags: string[]
}

const props = defineProps<Props>()

const displayTitle = computed(() => props.title || props.coverTitle || '小红书笔记预览')
const displayBody = computed(() => formatPublishedBody(props.body || props.context || '正文生成完成后会在这里展示发布后的详情页效果。'))
const publishImages = computed(() => {
  const cover = props.coverImage ? [{ url: props.coverImage, prompt: props.coverTitle || props.title }] : []
  return [...cover, ...props.images.filter(image => image.url)]
})
const leadingImage = computed(() => publishImages.value[0])
const secondaryImages = computed(() => publishImages.value.slice(1, 4))
const readableTime = computed(() => new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(new Date()))

/**
 * 发布预览不展示生成过程里的 Markdown 章节，只保留用户真正会看到的正文。
 */
function formatPublishedBody(value: string) {
  const normalized = value.replace(/\r\n/g, '\n').trim()
  const bodySection = normalized.match(/(?:^|\n)#{1,3}\s*正文\s*\n([\s\S]*)/u)?.[1] ?? normalized
  return bodySection
    .split('\n')
    .map(line => line.replace(/^#{1,6}\s*/u, '').trimEnd())
    .filter(line => !/^备选标题/u.test(line))
    .join('\n')
    .trim()
}
</script>

<template>
  <section class="rednote-preview-stage">
    <div class="rednote-phone-shell">
      <div class="rednote-phone-screen">
        <!-- 顶部模拟移动端发布详情页导航，不调用真实平台能力。 -->
        <header class="flex items-center justify-between border-b border-slate-100 px-4 py-3">
          <button type="button" class="grid size-9 place-items-center rounded-full text-slate-900" aria-label="返回">
            <ChevronLeftIcon class="size-5" />
          </button>
          <div class="flex min-w-0 items-center gap-2">
            <div class="grid size-8 shrink-0 place-items-center rounded-full bg-slate-950 text-xs font-semibold text-white">S</div>
            <div class="min-w-0">
              <p class="truncate text-sm font-semibold text-slate-950">Sakura Passage AI</p>
              <p class="text-[11px] text-slate-400">刚刚发布</p>
            </div>
          </div>
          <button type="button" class="grid size-9 place-items-center rounded-full text-slate-900" aria-label="更多">
            <MoreHorizontalIcon class="size-5" />
          </button>
        </header>

        <main class="max-h-[720px] overflow-y-auto pb-24">
          <div class="relative bg-slate-100">
            <img
              v-if="leadingImage?.url"
              :src="leadingImage.url"
              :alt="leadingImage.prompt || displayTitle"
              class="aspect-[3/4] w-full object-cover"
            >
            <div v-else class="grid aspect-[3/4] place-items-center bg-slate-100 px-8 text-center text-sm leading-6 text-slate-400">
              封面生成完成后会显示在这里
            </div>
            <div v-if="publishImages.length > 1" class="absolute bottom-3 left-1/2 flex -translate-x-1/2 gap-1.5 rounded-full bg-slate-950/45 px-2.5 py-1.5 backdrop-blur">
              <span
                v-for="(_, index) in publishImages"
                :key="index"
                class="size-1.5 rounded-full"
                :class="index === 0 ? 'bg-white' : 'bg-white/45'"
              />
            </div>
          </div>

          <article class="space-y-4 px-4 py-5">
            <h2 class="text-[20px] font-semibold leading-7 tracking-[-0.03em] text-slate-950">{{ displayTitle }}</h2>
            <p class="whitespace-pre-wrap text-[15px] leading-7 text-slate-800">{{ displayBody }}</p>

            <div v-if="tags.length" class="flex flex-wrap gap-2">
              <span v-for="tag in tags" :key="tag" class="text-[14px] font-medium text-sky-700">
                {{ tag.startsWith('#') ? tag : `#${tag}` }}
              </span>
            </div>

            <div v-if="secondaryImages.length" class="grid grid-cols-3 gap-2 pt-1">
              <img
                v-for="image in secondaryImages"
                :key="image.url"
                :src="image.url"
                :alt="image.prompt || '发布配图'"
                class="aspect-square rounded-2xl object-cover"
              >
            </div>

            <footer class="flex items-center justify-between border-t border-slate-100 pt-4 text-xs text-slate-400">
              <span>{{ readableTime }}</span>
              <span>模拟发布详情预览</span>
            </footer>
          </article>
        </main>

        <!-- 底部互动栏模拟发布后的阅读入口，固定在手机屏幕底部。 -->
        <nav class="absolute inset-x-0 bottom-0 flex items-center justify-between border-t border-slate-100 bg-white/95 px-4 py-3 backdrop-blur">
          <div class="rounded-full bg-slate-100 px-4 py-2 text-sm text-slate-400">说点什么...</div>
          <div class="flex items-center gap-4 text-slate-900">
            <button type="button" class="grid place-items-center gap-0.5 text-xs" aria-label="点赞">
              <HeartIcon class="size-5" />
              赞
            </button>
            <button type="button" class="grid place-items-center gap-0.5 text-xs" aria-label="评论">
              <MessageCircleIcon class="size-5" />
              评
            </button>
            <button type="button" class="grid place-items-center gap-0.5 text-xs" aria-label="收藏">
              <BookmarkIcon class="size-5" />
              藏
            </button>
            <button type="button" class="grid place-items-center gap-0.5 text-xs" aria-label="分享">
              <SendIcon class="size-5" />
              享
            </button>
          </div>
        </nav>
      </div>
    </div>
  </section>
</template>
