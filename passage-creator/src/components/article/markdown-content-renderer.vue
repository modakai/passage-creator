<script setup lang="ts">
import { renderMarkdown } from './markdown-renderer'

/**
 * Markdown 正文渲染组件属性。
 */
const props = defineProps<{
  content: string
}>()

const renderedContent = computed(() => renderMarkdown(props.content))
</script>

<template>
  <article
    class="article-markdown-renderer"
    v-html="renderedContent"
  />
</template>

<style scoped>
/**
 * 正文阅读区样式，保持文章页的轻量阅读体验。
 */
.article-markdown-renderer {
  color: #0f172a;
  font-size: 15px;
  line-height: 1.9;
}

.article-markdown-renderer :deep(h1) {
  margin: 0 0 1.2rem;
  color: #062018;
  font-size: 1.9rem;
  font-weight: 800;
  line-height: 1.25;
  letter-spacing: 0;
}

.article-markdown-renderer :deep(h2) {
  margin: 2rem 0 0.8rem;
  padding-left: 0.8rem;
  border-left: 4px solid #059669;
  color: #064e3b;
  font-size: 1.3rem;
  font-weight: 750;
  line-height: 1.35;
}

.article-markdown-renderer :deep(h3),
.article-markdown-renderer :deep(h4),
.article-markdown-renderer :deep(h5),
.article-markdown-renderer :deep(h6) {
  margin: 1.6rem 0 0.6rem;
  color: #0f3f32;
  font-size: 1.05rem;
  font-weight: 700;
}

.article-markdown-renderer :deep(p) {
  margin: 1rem 0;
}

.article-markdown-renderer :deep(strong) {
  color: #052e24;
  font-weight: 750;
}

.article-markdown-renderer :deep(em) {
  color: #166534;
}

.article-markdown-renderer :deep(ul),
.article-markdown-renderer :deep(ol) {
  margin: 1rem 0;
  padding-left: 1.4rem;
}

.article-markdown-renderer :deep(ul) {
  list-style: disc;
}

.article-markdown-renderer :deep(ol) {
  list-style: decimal;
}

.article-markdown-renderer :deep(li) {
  margin: 0.45rem 0;
  padding-left: 0.2rem;
}

.article-markdown-renderer :deep(blockquote) {
  margin: 1.2rem 0;
  padding: 0.9rem 1rem;
  border-left: 4px solid #34d399;
  border-radius: 0.8rem;
  background: #ecfdf5;
  color: #14532d;
}

.article-markdown-renderer :deep(code) {
  border-radius: 0.35rem;
  background: #ecfdf5;
  color: #047857;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 0.9em;
  padding: 0.12rem 0.35rem;
}

.article-markdown-renderer :deep(pre) {
  margin: 1.2rem 0;
  overflow-x: auto;
  border-radius: 1rem;
  background: #062018;
  padding: 1rem;
}

.article-markdown-renderer :deep(pre code) {
  background: transparent;
  color: #d1fae5;
  padding: 0;
}

.article-markdown-renderer :deep(figure) {
  margin: 1.4rem 0;
}

.article-markdown-renderer :deep(img) {
  display: block;
  width: 100%;
  max-height: 520px;
  border-radius: 0.75rem;
  object-fit: cover;
}

.article-markdown-renderer :deep(figcaption) {
  margin-top: 0.5rem;
  color: #64748b;
  font-size: 0.86rem;
  line-height: 1.5;
  text-align: center;
}
</style>
