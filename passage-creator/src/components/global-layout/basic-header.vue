<script lang="ts" setup>
import { useI18n } from 'vue-i18n'

import { cn } from '@/lib/utils'

import type { LayoutHeaderProps } from './types'

defineProps<LayoutHeaderProps>()
const { t } = useI18n()
</script>

<template>
  <header
    :class="cn(
      'flex flex-col md:flex-row gap-2 justify-between py-2',
      sticky ? 'sticky top-0 z-40 bg-background' : '',
    )"
  >
    <main class="space-y-2">
      <!-- 面包屑偏好通过根节点 breadcrumbs-hidden class 控制该节点显隐。 -->
      <UiBreadcrumb class="admin-breadcrumb">
        <UiBreadcrumbList>
          <UiBreadcrumbItem>
            <UiBreadcrumbLink as-child>
              <RouterLink to="/dashboard">
                {{ t('common.admin') }}
              </RouterLink>
            </UiBreadcrumbLink>
          </UiBreadcrumbItem>
          <UiBreadcrumbSeparator />
          <UiBreadcrumbItem>
            <UiBreadcrumbPage>
              {{ title }}
            </UiBreadcrumbPage>
          </UiBreadcrumbItem>
        </UiBreadcrumbList>
      </UiBreadcrumb>

      <!-- 页面标题偏好只影响后台通用标题组件，避免误伤普通内容标题。 -->
      <section class="admin-page-title">
        <h1 class="text-2xl font-bold">
          {{ title }}
        </h1>
        <p v-if="description" class="text-muted-foreground">
          {{ description }}
        </p>
      </section>
    </main>

    <aside class="flex items-center gap-2 flex-wrap">
      <slot name="actions" />
    </aside>
  </header>
</template>
