<script setup lang="ts">
import { FileTextIcon, LoaderCircleIcon, RefreshCwIcon, SquarePenIcon, Trash2Icon } from '@lucide/vue'
import { toast } from 'vue-sonner'

import type { ArticleItem, ArticleQuery, ArticleStatus } from '@/services/types/article.type'

import { BasicPage } from '@/components/global-layout'
import {
  useDeleteArticleMutation,
  useGetArticlePageQuery,
} from '@/services/api/article.api'

import ArticleFormDialog from './components/article-form-dialog.vue'

const query = reactive<ArticleQuery>({
  page: 1,
  pageSize: 10,
  topic: '',
  title: '',
  status: '',
})

const { data, isFetching, refetch } = useGetArticlePageQuery(query)
const { mutateAsync: deleteArticle, isPending: isDeleting } = useDeleteArticleMutation()

/**
 * 当前文章列表。
 */
const articleList = computed(() => data.value?.data?.records ?? [])
const total = computed(() => data.value?.data?.totalRow ?? 0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / query.pageSize)))
const statusFilter = computed({
  get: () => query.status || 'all',
  set: value => query.status = value === 'all' ? '' : value as ArticleStatus,
})
const editingArticleId = ref<number | null>(null)
const deletingArticle = ref<ArticleItem | null>(null)
const isEditDialogOpen = ref(false)

const statusOptions: Array<{ label: string, value: ArticleStatus, variant: 'default' | 'secondary' | 'destructive' | 'outline' }> = [
  { label: '等待处理', value: 'PENDING', variant: 'secondary' },
  { label: '处理中', value: 'PROCESSING', variant: 'outline' },
  { label: '已完成', value: 'COMPLETED', variant: 'default' },
  { label: '失败', value: 'FAILED', variant: 'destructive' },
]

watch(isEditDialogOpen, (value) => {
  if (!value) {
    editingArticleId.value = null
  }
})

/**
 * 获取状态展示配置。
 */
function getStatusMeta(status?: string) {
  return statusOptions.find(item => item.value === status) ?? statusOptions[0]
}

/**
 * 格式化时间显示。
 */
function formatTime(value?: string) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}

/**
 * 提交筛选查询。
 */
function handleSearch() {
  query.page = 1
  refetch()
}

/**
 * 重置筛选条件。
 */
function handleReset() {
  query.page = 1
  query.pageSize = 10
  query.topic = ''
  query.title = ''
  query.status = ''
  refetch()
}

/**
 * 切换分页。
 */
function changePage(nextPage: number) {
  query.page = Math.min(Math.max(nextPage, 1), totalPages.value)
  refetch()
}

/**
 * 打开编辑弹窗。
 */
function openEditDialog(id: number) {
  editingArticleId.value = id
  isEditDialogOpen.value = true
}

/**
 * 删除当前确认的文章。
 */
async function handleDelete() {
  if (!deletingArticle.value?.id) {
    return
  }

  try {
    await deleteArticle(deletingArticle.value.id)
    toast.success('文章删除成功')
    deletingArticle.value = null
    refetch()
  }
  catch (error: any) {
    const message = error?.data?.message ?? error?.message ?? '文章删除失败'
    toast.error(message)
  }
}
</script>

<template>
  <BasicPage title="文章管理" description="维护文章任务、标题、正文、配图和生成状态。" sticky>
    <template #actions>
      <ArticleFormDialog @success="refetch()" />
      <UiButton variant="outline" :disabled="isFetching" @click="refetch()">
        <RefreshCwIcon class="mr-1 size-4" :class="{ 'animate-spin': isFetching }" />
        刷新
      </UiButton>
    </template>

    <div class="space-y-4">
      <UiCard class="border-border/70 bg-gradient-to-br from-background to-muted/30">
        <UiCardHeader>
          <UiCardTitle>筛选条件</UiCardTitle>
          <UiCardDescription>支持按选题、标题和状态筛选文章。</UiCardDescription>
        </UiCardHeader>
        <UiCardContent class="grid gap-4 md:grid-cols-4">
          <div class="space-y-2">
            <UiLabel>选题</UiLabel>
            <UiInput v-model="query.topic" placeholder="按选题模糊查询" />
          </div>

          <div class="space-y-2">
            <UiLabel>标题</UiLabel>
            <UiInput v-model="query.title" placeholder="按主标题或副标题查询" />
          </div>

          <div class="space-y-2">
            <UiLabel>状态</UiLabel>
            <UiSelect v-model="statusFilter">
              <UiSelectTrigger class="w-full">
                <UiSelectValue placeholder="全部状态" />
              </UiSelectTrigger>
              <UiSelectContent>
                <UiSelectItem value="all">
                  全部状态
                </UiSelectItem>
                <UiSelectItem v-for="item in statusOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </UiSelectItem>
              </UiSelectContent>
            </UiSelect>
          </div>

          <div class="flex items-end gap-2">
            <UiButton class="flex-1" @click="handleSearch">
              查询
            </UiButton>
            <UiButton variant="outline" class="flex-1" @click="handleReset">
              重置
            </UiButton>
          </div>
        </UiCardContent>
      </UiCard>

      <UiCard class="overflow-hidden border-border/70">
        <UiCardHeader class="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
          <div>
            <UiCardTitle class="flex items-center gap-2">
              <FileTextIcon class="size-5 text-primary" />
              文章列表
            </UiCardTitle>
            <UiCardDescription>当前共 {{ total }} 篇文章。</UiCardDescription>
          </div>
        </UiCardHeader>
        <UiCardContent>
          <div v-if="isFetching" class="flex items-center justify-center py-12 text-sm text-muted-foreground">
            <LoaderCircleIcon class="mr-2 size-4 animate-spin" />
            正在加载文章数据...
          </div>

          <div v-else class="overflow-x-auto rounded-xl border border-border/70">
            <table class="w-full text-sm">
              <thead class="bg-muted/50">
                <tr class="border-b text-left">
                  <th class="px-4 py-3 font-medium">
                    选题
                  </th>
                  <th class="px-4 py-3 font-medium">
                    标题
                  </th>
                  <th class="px-4 py-3 font-medium">
                    状态
                  </th>
                  <th class="px-4 py-3 font-medium">
                    创建时间
                  </th>
                  <th class="px-4 py-3 font-medium">
                    更新时间
                  </th>
                  <th class="px-4 py-3 font-medium text-right">
                    操作
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in articleList" :key="item.id" class="border-b last:border-b-0">
                  <td class="max-w-sm px-4 py-3 align-top">
                    <div class="font-medium">
                      {{ item.topic }}
                    </div>
                    <div class="mt-1 text-xs text-muted-foreground">
                      {{ item.taskId }}
                    </div>
                  </td>
                  <td class="max-w-md px-4 py-3 align-top">
                    <div class="font-medium">
                      {{ item.mainTitle || '-' }}
                    </div>
                    <div v-if="item.subTitle" class="mt-1 line-clamp-2 text-xs text-muted-foreground">
                      {{ item.subTitle }}
                    </div>
                  </td>
                  <td class="px-4 py-3">
                    <UiBadge :variant="getStatusMeta(item.status).variant">
                      {{ getStatusMeta(item.status).label }}
                    </UiBadge>
                    <div v-if="item.errorMessage" class="mt-1 line-clamp-2 max-w-xs text-xs text-destructive">
                      {{ item.errorMessage }}
                    </div>
                  </td>
                  <td class="px-4 py-3 text-muted-foreground">
                    {{ formatTime(item.createTime) }}
                  </td>
                  <td class="px-4 py-3 text-muted-foreground">
                    {{ formatTime(item.updateTime) }}
                  </td>
                  <td class="px-4 py-3">
                    <div class="flex justify-end gap-2">
                      <UiButton variant="outline" size="sm" @click="openEditDialog(item.id)">
                        <SquarePenIcon class="mr-1 size-4" />
                        编辑
                      </UiButton>
                      <UiButton variant="outline" size="sm" :disabled="isDeleting" @click="deletingArticle = item">
                        <Trash2Icon class="mr-1 size-4" />
                        删除
                      </UiButton>
                    </div>
                  </td>
                </tr>
                <tr v-if="articleList.length === 0">
                  <td colspan="6" class="px-4 py-10 text-center text-muted-foreground">
                    暂无文章数据
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="mt-4 flex items-center justify-between text-sm text-muted-foreground">
            <span>第 {{ query.page }} / {{ totalPages }} 页</span>
            <div class="flex gap-2">
              <UiButton variant="outline" size="sm" :disabled="query.page <= 1" @click="changePage(query.page - 1)">
                上一页
              </UiButton>
              <UiButton variant="outline" size="sm" :disabled="query.page >= totalPages" @click="changePage(query.page + 1)">
                下一页
              </UiButton>
            </div>
          </div>
        </UiCardContent>
      </UiCard>
    </div>

    <ArticleFormDialog
      v-if="editingArticleId"
      v-model:open="isEditDialogOpen"
      :article-id="editingArticleId"
      :hide-trigger="true"
      @success="() => { isEditDialogOpen = false; refetch() }"
    />

    <UiAlertDialog :open="deletingArticle !== null" @update:open="value => !value ? deletingArticle = null : undefined">
      <UiAlertDialogContent>
        <UiAlertDialogHeader>
          <UiAlertDialogTitle>确认删除文章</UiAlertDialogTitle>
          <UiAlertDialogDescription>
            删除后当前文章将不可恢复，请确认是否继续。
          </UiAlertDialogDescription>
        </UiAlertDialogHeader>
        <UiAlertDialogFooter>
          <UiAlertDialogCancel @click="deletingArticle = null">
            取消
          </UiAlertDialogCancel>
          <UiButton variant="destructive" :disabled="isDeleting" @click="handleDelete">
            确认删除
          </UiButton>
        </UiAlertDialogFooter>
      </UiAlertDialogContent>
    </UiAlertDialog>
  </BasicPage>
</template>

<route lang="yaml">
meta:
  auth: true
</route>
