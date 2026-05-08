<script setup lang="ts">
import { ArchiveIcon, BotIcon, CopyIcon, EyeIcon, FileCode2Icon, LoaderCircleIcon, RefreshCwIcon, RocketIcon, ScrollTextIcon, SquarePenIcon, Trash2Icon } from '@lucide/vue'
import { toast } from 'vue-sonner'

import type {
  PromptTemplateForm,
  PromptTemplateItem,
  PromptTemplateQuery,
  PromptTemplateStatus,
  PromptUsageLogQuery,
} from '@/services/types/prompt-template.type'

import { BasicPage } from '@/components/global-layout'
import {
  useCreatePromptTemplateMutation,
  useDeletePromptTemplateMutation,
  useGetPromptTemplatePageQuery,
  useGetPromptUsageLogPageQuery,
  usePromptTemplateVersionsRequest,
  usePromptTemplateActionMutation,
  useRefreshPromptTemplateMutation,
  useUpdatePromptTemplateMutation,
} from '@/services/api/prompt-template.api'

const ALL_SELECT_VALUE = '__all__'

/**
 * Prompt 模板分页查询条件。
 */
const templateQuery = reactive<PromptTemplateQuery>({
  page: 1,
  pageSize: 10,
  templateKey: '',
  version: '',
  status: '',
  environment: 'production',
})

/**
 * Prompt 使用日志分页查询条件。
 */
const usageQuery = reactive<PromptUsageLogQuery>({
  page: 1,
  pageSize: 10,
  templateKey: '',
  agentName: '',
  taskId: '',
  environment: 'production',
  responseOk: '',
})

/**
 * Prompt 表单默认值。
 */
const form = reactive<PromptTemplateForm>({
  templateKey: 'article.title.user',
  version: '1.0.1',
  content: '选题：{topic}\n',
  variablesSchema: '[{"name":"topic","label":"选题","required":true}]',
  description: '',
  environment: 'production',
})

const activeTab = ref('templates')
const formOpen = ref(false)
const detailOpen = ref(false)
const selectedTemplate = ref<PromptTemplateItem | null>(null)
const deletingTemplate = ref<PromptTemplateItem | null>(null)
const { data: templateData, isFetching: isFetchingTemplates, refetch: refetchTemplates } = useGetPromptTemplatePageQuery(templateQuery)
const { data: usageData, isFetching: isFetchingUsage, refetch: refetchUsage } = useGetPromptUsageLogPageQuery(usageQuery)
const { mutateAsync: createTemplate, isPending: isCreating } = useCreatePromptTemplateMutation()
const { mutateAsync: updateTemplate, isPending: isUpdating } = useUpdatePromptTemplateMutation()
const { mutateAsync: deleteTemplate, isPending: isDeleting } = useDeletePromptTemplateMutation()
const publishMutation = usePromptTemplateActionMutation('publish')
const archiveMutation = usePromptTemplateActionMutation('archive')
const requestTemplateVersions = usePromptTemplateVersionsRequest()
const { mutateAsync: refreshTemplate, isPending: isRefreshingRuntime } = useRefreshPromptTemplateMutation()

const templates = computed(() => templateData.value?.data?.records ?? [])
const usageLogs = computed(() => usageData.value?.data?.records ?? [])
const templateTotal = computed(() => templateData.value?.data?.totalRow ?? 0)
const usageTotal = computed(() => usageData.value?.data?.totalRow ?? 0)
const templateTotalPages = computed(() => Math.max(1, Math.ceil(templateTotal.value / templateQuery.pageSize)))
const usageTotalPages = computed(() => Math.max(1, Math.ceil(usageTotal.value / usageQuery.pageSize)))
const isSaving = computed(() => isCreating.value || isUpdating.value)
const isPublishing = computed(() => publishMutation.isPending.value)
const isArchiving = computed(() => archiveMutation.isPending.value)
const isEditing = computed(() => Boolean(form.id))

const templateStatusFilter = computed({
  // Reka Select 不能使用空字符串选项，使用哨兵值代表全部状态。
  get: () => templateQuery.status === '' ? ALL_SELECT_VALUE : String(templateQuery.status),
  set: (value) => {
    templateQuery.status = value === ALL_SELECT_VALUE ? '' : value as PromptTemplateStatus
  },
})

const usageResponseFilter = computed({
  // Reka Select 不能使用空字符串选项，使用哨兵值代表全部状态。
  get: () => usageQuery.responseOk === '' ? ALL_SELECT_VALUE : String(usageQuery.responseOk),
  set: (value) => {
    usageQuery.responseOk = value === ALL_SELECT_VALUE ? '' : value === 'true'
  },
})

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
 * 根据状态映射标签样式。
 */
function getStatusVariant(status: PromptTemplateStatus) {
  if (status === 'ACTIVE') {
    return 'default'
  }
  if (status === 'ARCHIVED') {
    return 'secondary'
  }
  return 'outline'
}

/**
 * 根据成功状态映射日志标签样式。
 */
function getResultVariant(responseOk?: boolean) {
  if (responseOk === true) {
    return 'default'
  }
  if (responseOk === false) {
    return 'destructive'
  }
  return 'secondary'
}

/**
 * 重置 Prompt 表单。
 */
function resetForm() {
  Object.assign(form, {
    id: undefined,
    templateKey: 'article.title.user',
    version: '1.0.1',
    content: '选题：{topic}\n',
    variablesSchema: '[{"name":"topic","label":"选题","required":true}]',
    description: '',
    environment: 'production',
  })
}

/**
 * 合并当前列表和后端返回的版本号，避免跨分页时生成重复版本。
 */
function getUsedVersions(templateKey: string, environment: string, remoteVersions: string[] = []) {
  const pageVersions = templates.value
    .filter(item => item.templateKey === templateKey && item.environment === environment)
    .map(item => item.version)
  return new Set([...pageVersions, ...remoteVersions])
}

/**
 * 根据当前版本号生成未被占用的下一个草稿版本号。
 */
function suggestNextVersion(version: string, usedVersions: Set<string>) {
  const segments = version.split('.').map(segment => Number(segment))
  if (segments.length === 3 && segments.every(segment => Number.isInteger(segment) && segment >= 0)) {
    let patch = segments[2] + 1
    let nextVersion = `${segments[0]}.${segments[1]}.${patch}`
    while (usedVersions.has(nextVersion)) {
      patch += 1
      nextVersion = `${segments[0]}.${segments[1]}.${patch}`
    }
    return nextVersion
  }

  let suffix = 1
  let nextVersion = `${version}-draft`
  while (usedVersions.has(nextVersion)) {
    suffix += 1
    nextVersion = `${version}-draft-${suffix}`
  }
  return nextVersion
}

/**
 * 打开新增 Prompt 模板弹窗。
 */
function openCreateDialog() {
  resetForm()
  formOpen.value = true
}

/**
 * 打开编辑 Prompt 模板弹窗。
 */
function openEditDialog(item: PromptTemplateItem) {
  Object.assign(form, {
    id: item.id,
    templateKey: item.templateKey,
    version: item.version,
    content: item.content,
    variablesSchema: item.variablesSchema ?? '',
    description: item.description ?? '',
    environment: item.environment,
  })
  formOpen.value = true
}

/**
 * 打开 Prompt 模板详情弹窗。
 */
function openDetailDialog(item: PromptTemplateItem) {
  selectedTemplate.value = item
  detailOpen.value = true
}

/**
 * 从任意历史版本复制为新的 DRAFT，避免直接修改 ACTIVE 版本影响线上 Agent。
 */
async function cloneAsDraft(item: PromptTemplateItem) {
  let remoteVersions: string[] = []
  try {
    const response = await requestTemplateVersions({ templateKey: item.templateKey, environment: item.environment })
    remoteVersions = response.data ?? []
  }
  catch (error: any) {
    toast.error(error?.data?.message ?? error?.message ?? '版本号查询失败')
    return
  }
  const usedVersions = getUsedVersions(item.templateKey, item.environment, remoteVersions)
  Object.assign(form, {
    id: undefined,
    templateKey: item.templateKey,
    version: suggestNextVersion(item.version, usedVersions),
    content: item.content,
    variablesSchema: item.variablesSchema ?? '',
    description: item.description ? `基于 ${item.version} 调整：${item.description}` : `基于 ${item.version} 调整`,
    environment: item.environment,
  })
  formOpen.value = true
}

/**
 * 保存 Prompt 模板草稿。
 */
async function handleSubmit() {
  try {
    if (form.id) {
      await updateTemplate({ ...form })
      toast.success('Prompt 草稿已更新')
    }
    else {
      await createTemplate({ ...form })
      toast.success('Prompt 草稿已创建')
    }
    formOpen.value = false
    resetForm()
    refetchTemplates()
  }
  catch (error: any) {
    toast.error(error?.data?.message ?? error?.message ?? 'Prompt 保存失败')
  }
}

/**
 * 发布 Prompt 模板版本。
 */
async function handlePublish(item: PromptTemplateItem) {
  try {
    await publishMutation.mutateAsync(item.id)
    toast.success('Prompt 版本已发布')
    refetchTemplates()
  }
  catch (error: any) {
    toast.error(error?.data?.message ?? error?.message ?? '发布失败')
  }
}

/**
 * 归档 Prompt 模板版本。
 */
async function handleArchive(item: PromptTemplateItem) {
  try {
    await archiveMutation.mutateAsync(item.id)
    toast.success('Prompt 版本已归档')
    refetchTemplates()
  }
  catch (error: any) {
    toast.error(error?.data?.message ?? error?.message ?? '归档失败')
  }
}

/**
 * 打开 Prompt 模板删除确认框。
 */
function openDeleteDialog(item: PromptTemplateItem) {
  deletingTemplate.value = item
}

/**
 * 删除当前确认的非 ACTIVE Prompt 模板版本。
 */
async function handleDeleteTemplate() {
  const targetTemplate = deletingTemplate.value
  if (!targetTemplate) {
    return
  }
  try {
    await deleteTemplate(targetTemplate.id)
    toast.success('Prompt 版本已删除')
    deletingTemplate.value = null
    refetchTemplates()
  }
  catch (error: any) {
    toast.error(error?.data?.message ?? error?.message ?? '删除失败')
  }
}

/**
 * 刷新单个 Prompt 运行时缓存。
 */
async function handleRefreshRuntime(item: PromptTemplateItem) {
  try {
    await refreshTemplate({ templateKey: item.templateKey, environment: item.environment })
    toast.success('运行时缓存已刷新')
  }
  catch (error: any) {
    toast.error(error?.data?.message ?? error?.message ?? '刷新失败')
  }
}

/**
 * 查询 Prompt 模板。
 */
function handleSearchTemplates() {
  templateQuery.page = 1
  refetchTemplates()
}

/**
 * 重置 Prompt 模板查询条件。
 */
function handleResetTemplates() {
  templateQuery.page = 1
  templateQuery.templateKey = ''
  templateQuery.version = ''
  templateQuery.status = ''
  templateQuery.environment = 'production'
  refetchTemplates()
}

/**
 * 查询 Prompt 使用日志。
 */
function handleSearchUsage() {
  usageQuery.page = 1
  refetchUsage()
}

/**
 * 重置 Prompt 使用日志查询条件。
 */
function handleResetUsage() {
  usageQuery.page = 1
  usageQuery.templateKey = ''
  usageQuery.agentName = ''
  usageQuery.taskId = ''
  usageQuery.environment = 'production'
  usageQuery.responseOk = ''
  refetchUsage()
}
</script>

<template>
  <BasicPage title="Prompt 管理" description="管理内部 Agent 的 Prompt 模板版本、发布状态与使用记录。" sticky>
    <template #actions>
      <UiButton variant="outline" :disabled="isFetchingTemplates || isFetchingUsage" @click="() => { refetchTemplates(); refetchUsage() }">
        <RefreshCwIcon class="mr-1 size-4" :class="{ 'animate-spin': isFetchingTemplates || isFetchingUsage }" />
        刷新
      </UiButton>
      <UiButton @click="openCreateDialog">
        <FileCode2Icon class="mr-1 size-4" />
        新建 Prompt
      </UiButton>
    </template>

    <div class="grid gap-4 xl:grid-cols-[minmax(0,1fr)_320px]">
      <UiTabs v-model="activeTab" class="min-w-0">
        <UiTabsList class="mb-3">
          <UiTabsTrigger value="templates">
            <BotIcon class="size-4" />
            模板版本
          </UiTabsTrigger>
          <UiTabsTrigger value="usage">
            <ScrollTextIcon class="size-4" />
            使用日志
          </UiTabsTrigger>
        </UiTabsList>

        <UiTabsContent value="templates" class="space-y-4">
          <UiCard class="border-border/70">
            <UiCardHeader class="border-b bg-muted/30">
              <UiCardTitle class="text-base">
                筛选模板
              </UiCardTitle>
              <UiCardDescription>按模板标识、版本、状态和运行环境定位 Prompt。</UiCardDescription>
            </UiCardHeader>
            <UiCardContent class="grid gap-3 pt-5 md:grid-cols-[1.2fr_0.7fr_0.7fr_0.7fr_auto]">
              <UiInput v-model="templateQuery.templateKey" placeholder="模板标识" />
              <UiInput v-model="templateQuery.version" placeholder="版本号" />
              <UiSelect v-model="templateStatusFilter">
                <UiSelectTrigger>
                  <UiSelectValue placeholder="全部状态" />
                </UiSelectTrigger>
                <UiSelectContent>
                  <UiSelectItem :value="ALL_SELECT_VALUE">
                    全部状态
                  </UiSelectItem>
                  <UiSelectItem value="DRAFT">
                    DRAFT
                  </UiSelectItem>
                  <UiSelectItem value="ACTIVE">
                    ACTIVE
                  </UiSelectItem>
                  <UiSelectItem value="ARCHIVED">
                    ARCHIVED
                  </UiSelectItem>
                </UiSelectContent>
              </UiSelect>
              <UiInput v-model="templateQuery.environment" placeholder="运行环境" />
              <div class="flex gap-2">
                <UiButton @click="handleSearchTemplates">
                  查询
                </UiButton>
                <UiButton variant="outline" @click="handleResetTemplates">
                  重置
                </UiButton>
              </div>
            </UiCardContent>
          </UiCard>

          <UiCard class="border-border/70">
            <UiCardHeader class="flex flex-col gap-3 border-b bg-gradient-to-br from-background to-muted/40 md:flex-row md:items-center md:justify-between">
              <div>
                <UiCardTitle>模板版本</UiCardTitle>
                <UiCardDescription>当前共 {{ templateTotal }} 个 Prompt 版本。</UiCardDescription>
              </div>
              <div class="flex items-center gap-2 text-xs text-muted-foreground">
                <span class="h-2 w-2 rounded-full bg-primary" />
                ACTIVE 会被 Agent 运行时优先读取
              </div>
            </UiCardHeader>
            <UiCardContent>
              <div v-if="isFetchingTemplates" class="flex items-center justify-center py-12 text-sm text-muted-foreground">
                <LoaderCircleIcon class="mr-2 size-4 animate-spin" />
                正在加载 Prompt 模板...
              </div>
              <div v-else class="overflow-x-auto rounded-xl border border-border/70">
                <table class="w-full text-sm">
                  <thead class="bg-muted/50">
                    <tr class="border-b text-left">
                      <th class="px-4 py-3 font-medium">
                        模板
                      </th>
                      <th class="px-4 py-3 font-medium">
                        版本
                      </th>
                      <th class="px-4 py-3 font-medium">
                        状态
                      </th>
                      <th class="px-4 py-3 font-medium">
                        环境
                      </th>
                      <th class="px-4 py-3 font-medium">
                        说明
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
                    <tr v-for="item in templates" :key="item.id" class="border-b last:border-b-0">
                      <td class="px-4 py-3 align-top">
                        <div class="font-medium">
                          {{ item.templateKey }}
                        </div>
                        <div class="mt-1 line-clamp-2 max-w-[360px] text-xs text-muted-foreground">
                          {{ item.content }}
                        </div>
                      </td>
                      <td class="px-4 py-3 align-top font-medium">
                        {{ item.version }}
                      </td>
                      <td class="px-4 py-3 align-top">
                        <UiBadge :variant="getStatusVariant(item.status)">
                          {{ item.status }}
                        </UiBadge>
                      </td>
                      <td class="px-4 py-3 align-top text-muted-foreground">
                        {{ item.environment }}
                      </td>
                      <td class="max-w-[220px] px-4 py-3 align-top text-muted-foreground">
                        <span class="line-clamp-2">{{ item.description || '-' }}</span>
                      </td>
                      <td class="px-4 py-3 align-top text-muted-foreground">
                        {{ formatTime(item.updatedAt) }}
                      </td>
                      <td class="px-4 py-3 align-top">
                        <div class="flex justify-end gap-2">
                          <UiButton size="sm" variant="outline" @click="openDetailDialog(item)">
                            <EyeIcon class="mr-1 size-4" />
                            查看
                          </UiButton>
                          <UiButton size="sm" variant="outline" :disabled="item.status !== 'DRAFT'" @click="openEditDialog(item)">
                            <SquarePenIcon class="mr-1 size-4" />
                            编辑
                          </UiButton>
                          <UiButton size="sm" variant="outline" @click="cloneAsDraft(item)">
                            <CopyIcon class="mr-1 size-4" />
                            复制
                          </UiButton>
                          <UiButton size="sm" variant="outline" :disabled="item.status === 'ACTIVE' || isPublishing" @click="handlePublish(item)">
                            <RocketIcon class="mr-1 size-4" />
                            发布
                          </UiButton>
                          <UiButton size="sm" variant="outline" :disabled="item.status !== 'DRAFT' || isArchiving" @click="handleArchive(item)">
                            <ArchiveIcon class="mr-1 size-4" />
                            归档
                          </UiButton>
                          <UiButton size="sm" variant="outline" :disabled="item.status === 'ACTIVE' || isDeleting" @click="openDeleteDialog(item)">
                            <Trash2Icon class="mr-1 size-4" />
                            删除
                          </UiButton>
                          <UiButton size="sm" variant="outline" :disabled="isRefreshingRuntime" @click="handleRefreshRuntime(item)">
                            <RefreshCwIcon class="mr-1 size-4" />
                            刷新
                          </UiButton>
                        </div>
                      </td>
                    </tr>
                    <tr v-if="templates.length === 0">
                      <td colspan="7" class="px-4 py-10 text-center text-muted-foreground">
                        暂无 Prompt 模板
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div class="mt-4 flex items-center justify-between text-sm text-muted-foreground">
                <span>第 {{ templateQuery.page }} / {{ templateTotalPages }} 页</span>
                <div class="flex gap-2">
                  <UiButton variant="outline" size="sm" :disabled="templateQuery.page <= 1" @click="templateQuery.page--; refetchTemplates()">
                    上一页
                  </UiButton>
                  <UiButton variant="outline" size="sm" :disabled="templateQuery.page >= templateTotalPages" @click="templateQuery.page++; refetchTemplates()">
                    下一页
                  </UiButton>
                </div>
              </div>
            </UiCardContent>
          </UiCard>
        </UiTabsContent>

        <UiTabsContent value="usage" class="space-y-4">
          <UiCard class="border-border/70">
            <UiCardHeader class="border-b bg-muted/30">
              <UiCardTitle class="text-base">
                筛选日志
              </UiCardTitle>
              <UiCardDescription>按模板、Agent、任务和调用结果追溯实际使用版本。</UiCardDescription>
            </UiCardHeader>
            <UiCardContent class="grid gap-3 pt-5 md:grid-cols-[1fr_1fr_1fr_0.8fr_0.8fr_auto]">
              <UiInput v-model="usageQuery.templateKey" placeholder="模板标识" />
              <UiInput v-model="usageQuery.agentName" placeholder="Agent 名称" />
              <UiInput v-model="usageQuery.taskId" placeholder="任务 ID" />
              <UiInput v-model="usageQuery.environment" placeholder="环境" />
              <UiSelect v-model="usageResponseFilter">
                <UiSelectTrigger>
                  <UiSelectValue placeholder="全部结果" />
                </UiSelectTrigger>
                <UiSelectContent>
                  <UiSelectItem :value="ALL_SELECT_VALUE">
                    全部结果
                  </UiSelectItem>
                  <UiSelectItem value="true">
                    成功
                  </UiSelectItem>
                  <UiSelectItem value="false">
                    失败
                  </UiSelectItem>
                </UiSelectContent>
              </UiSelect>
              <div class="flex gap-2">
                <UiButton @click="handleSearchUsage">
                  查询
                </UiButton>
                <UiButton variant="outline" @click="handleResetUsage">
                  重置
                </UiButton>
              </div>
            </UiCardContent>
          </UiCard>

          <UiCard class="border-border/70">
            <UiCardHeader class="border-b bg-gradient-to-br from-background to-muted/40">
              <UiCardTitle>使用日志</UiCardTitle>
              <UiCardDescription>当前共 {{ usageTotal }} 条调用记录。</UiCardDescription>
            </UiCardHeader>
            <UiCardContent>
              <div v-if="isFetchingUsage" class="flex items-center justify-center py-12 text-sm text-muted-foreground">
                <LoaderCircleIcon class="mr-2 size-4 animate-spin" />
                正在加载 Prompt 使用日志...
              </div>
              <div v-else class="overflow-x-auto rounded-xl border border-border/70">
                <table class="w-full text-sm">
                  <thead class="bg-muted/50">
                    <tr class="border-b text-left">
                      <th class="px-4 py-3 font-medium">
                        模板
                      </th>
                      <th class="px-4 py-3 font-medium">
                        Agent
                      </th>
                      <th class="px-4 py-3 font-medium">
                        任务
                      </th>
                      <th class="px-4 py-3 font-medium">
                        结果
                      </th>
                      <th class="px-4 py-3 font-medium">
                        耗时
                      </th>
                      <th class="px-4 py-3 font-medium">
                        使用时间
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in usageLogs" :key="item.id" class="border-b last:border-b-0">
                      <td class="px-4 py-3 align-top">
                        <div class="font-medium">
                          {{ item.templateKey }}
                        </div>
                        <div class="mt-1 text-xs text-muted-foreground">
                          {{ item.version }} / {{ item.environment }}
                        </div>
                      </td>
                      <td class="px-4 py-3 align-top">
                        {{ item.agentName }}
                      </td>
                      <td class="max-w-[260px] px-4 py-3 align-top text-muted-foreground">
                        <span class="line-clamp-1">{{ item.taskId || '-' }}</span>
                      </td>
                      <td class="px-4 py-3 align-top">
                        <UiBadge :variant="getResultVariant(item.responseOk)">
                          {{ item.responseOk === true ? '成功' : item.responseOk === false ? '失败' : '未知' }}
                        </UiBadge>
                        <div v-if="item.errorMessage" class="mt-1 line-clamp-2 max-w-[260px] text-xs text-destructive">
                          {{ item.errorMessage }}
                        </div>
                      </td>
                      <td class="px-4 py-3 align-top text-muted-foreground">
                        {{ item.latencyMs ?? '-' }} ms
                      </td>
                      <td class="px-4 py-3 align-top text-muted-foreground">
                        {{ formatTime(item.usedAt) }}
                      </td>
                    </tr>
                    <tr v-if="usageLogs.length === 0">
                      <td colspan="6" class="px-4 py-10 text-center text-muted-foreground">
                        暂无使用日志
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div class="mt-4 flex items-center justify-between text-sm text-muted-foreground">
                <span>第 {{ usageQuery.page }} / {{ usageTotalPages }} 页</span>
                <div class="flex gap-2">
                  <UiButton variant="outline" size="sm" :disabled="usageQuery.page <= 1" @click="usageQuery.page--; refetchUsage()">
                    上一页
                  </UiButton>
                  <UiButton variant="outline" size="sm" :disabled="usageQuery.page >= usageTotalPages" @click="usageQuery.page++; refetchUsage()">
                    下一页
                  </UiButton>
                </div>
              </div>
            </UiCardContent>
          </UiCard>
        </UiTabsContent>
      </UiTabs>

      <aside class="space-y-4">
        <UiCard class="border-border/70 bg-muted/20">
          <UiCardHeader>
            <UiCardTitle class="flex items-center gap-2 text-base">
              <BotIcon class="size-4 text-primary" />
              运行规则
            </UiCardTitle>
          </UiCardHeader>
          <UiCardContent class="space-y-3 text-sm text-muted-foreground">
            <p>同一个模板标识和环境只应存在一个 ACTIVE 版本。</p>
            <p>DRAFT 可以编辑；发布后会覆盖运行时读取版本；ARCHIVED 保留历史追溯。</p>
            <p>变量定义必须声明模板中的 {name} 占位符，否则后端会拒绝保存。</p>
          </UiCardContent>
        </UiCard>

        <UiCard class="border-border/70">
          <UiCardHeader>
            <UiCardTitle class="text-base">
              默认模板 key
            </UiCardTitle>
            <UiCardDescription>当前 Agent 已接入的模板标识。</UiCardDescription>
          </UiCardHeader>
          <UiCardContent class="space-y-2 text-sm">
            <code class="block rounded bg-muted px-3 py-2">article.title.system</code>
            <code class="block rounded bg-muted px-3 py-2">article.title.user</code>
            <code class="block rounded bg-muted px-3 py-2">article.outline.system</code>
            <code class="block rounded bg-muted px-3 py-2">article.outline.user</code>
            <code class="block rounded bg-muted px-3 py-2">article.content.system</code>
            <code class="block rounded bg-muted px-3 py-2">article.content.user</code>
          </UiCardContent>
        </UiCard>
      </aside>
    </div>

    <UiDialog v-model:open="formOpen">
      <UiDialogContent class="max-w-5xl">
        <UiDialogHeader>
          <UiDialogTitle>{{ isEditing ? '编辑 Prompt 草稿' : '新建 Prompt 草稿' }}</UiDialogTitle>
          <UiDialogDescription>DRAFT 状态用于编辑，发布后才会进入 Agent 运行时读取链路。</UiDialogDescription>
        </UiDialogHeader>
        <div class="grid gap-4 md:grid-cols-4">
          <div class="space-y-2 md:col-span-2">
            <UiLabel>模板标识</UiLabel>
            <UiInput v-model="form.templateKey" :disabled="isEditing" />
          </div>
          <div class="space-y-2">
            <UiLabel>版本号</UiLabel>
            <UiInput v-model="form.version" :disabled="isEditing" />
          </div>
          <div class="space-y-2">
            <UiLabel>运行环境</UiLabel>
            <UiInput v-model="form.environment" :disabled="isEditing" />
          </div>
          <div class="space-y-2 md:col-span-4">
            <UiLabel>Prompt 内容</UiLabel>
            <UiTextarea v-model="form.content" class="min-h-[260px] font-mono text-sm" />
          </div>
          <div class="space-y-2 md:col-span-2">
            <UiLabel>变量定义 JSON</UiLabel>
            <UiTextarea v-model="form.variablesSchema" class="min-h-[160px] font-mono text-sm" />
          </div>
          <div class="space-y-2 md:col-span-2">
            <UiLabel>版本说明</UiLabel>
            <UiTextarea v-model="form.description" class="min-h-[160px]" placeholder="说明本版本改动原因、影响范围和回滚提示" />
          </div>
        </div>
        <UiDialogFooter>
          <UiButton variant="outline" @click="formOpen = false">
            取消
          </UiButton>
          <UiButton :disabled="isSaving" @click="handleSubmit">
            保存草稿
          </UiButton>
        </UiDialogFooter>
      </UiDialogContent>
    </UiDialog>

    <UiDialog v-model:open="detailOpen">
      <UiDialogContent class="!w-[calc(100vw-3rem)] !max-w-[calc(100vw-3rem)] gap-0 overflow-hidden p-0 2xl:!w-[1500px] 2xl:!max-w-[1500px]">
        <UiDialogHeader class="border-b px-6 py-5">
          <UiDialogTitle>Prompt 模板详情</UiDialogTitle>
          <UiDialogDescription>优先查看当前版本的完整 Prompt 内容，变量和发布时间作为辅助信息。</UiDialogDescription>
        </UiDialogHeader>
        <div v-if="selectedTemplate" class="max-h-[calc(100vh-11rem)] overflow-y-auto px-6 py-5">
          <div class="grid gap-3 md:grid-cols-[minmax(0,1fr)_120px_120px_140px]">
            <div class="rounded-md border bg-muted/20 p-3">
              <div class="text-xs text-muted-foreground">
                模板标识
              </div>
              <div class="mt-1 break-all font-medium">
                {{ selectedTemplate.templateKey }}
              </div>
            </div>
            <div class="rounded-md border bg-muted/20 p-3">
              <div class="text-xs text-muted-foreground">
                版本
              </div>
              <div class="mt-1 font-medium">
                {{ selectedTemplate.version }}
              </div>
            </div>
            <div class="rounded-md border bg-muted/20 p-3">
              <div class="text-xs text-muted-foreground">
                状态
              </div>
              <UiBadge class="mt-1" :variant="getStatusVariant(selectedTemplate.status)">
                {{ selectedTemplate.status }}
              </UiBadge>
            </div>
            <div class="rounded-md border bg-muted/20 p-3">
              <div class="text-xs text-muted-foreground">
                环境
              </div>
              <div class="mt-1 font-medium">
                {{ selectedTemplate.environment }}
              </div>
            </div>
          </div>

          <div class="mt-4 space-y-4">
            <div class="space-y-2">
              <div class="flex items-center justify-between gap-3">
                <UiLabel>Prompt 内容</UiLabel>
                <span class="text-xs text-muted-foreground">{{ selectedTemplate.content.length }} 字符</span>
              </div>
              <pre class="min-h-[360px] max-h-[calc(100vh-24rem)] overflow-auto whitespace-pre-wrap rounded-md border bg-muted/20 p-5 font-mono text-sm leading-7">{{ selectedTemplate.content }}</pre>
            </div>

            <div class="grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
              <div class="space-y-2">
                <UiLabel>变量定义 JSON</UiLabel>
                <pre class="max-h-[220px] overflow-auto whitespace-pre-wrap rounded-md border bg-muted/20 p-4 font-mono text-sm">{{ selectedTemplate.variablesSchema || '-' }}</pre>
              </div>
              <div class="space-y-2">
                <UiLabel>版本说明</UiLabel>
                <div class="min-h-[120px] rounded-md border bg-muted/20 p-4 text-sm text-muted-foreground">
                  {{ selectedTemplate.description || '-' }}
                </div>
              </div>
            </div>

            <div class="grid gap-3 rounded-md border bg-muted/20 p-4 text-xs text-muted-foreground md:grid-cols-4">
              <div>创建人：{{ selectedTemplate.createdBy || '-' }}</div>
              <div>发布人：{{ selectedTemplate.publishedBy || '-' }}</div>
              <div>发布时间：{{ formatTime(selectedTemplate.publishedAt) }}</div>
              <div>更新时间：{{ formatTime(selectedTemplate.updatedAt) }}</div>
            </div>
          </div>
        </div>
        <UiDialogFooter class="border-t px-6 py-4">
          <UiButton variant="outline" @click="detailOpen = false">
            关闭
          </UiButton>
          <UiButton v-if="selectedTemplate" variant="outline" @click="cloneAsDraft(selectedTemplate); detailOpen = false">
            <CopyIcon class="mr-1 size-4" />
            复制为草稿
          </UiButton>
          <UiButton v-if="selectedTemplate?.status === 'DRAFT'" @click="openEditDialog(selectedTemplate); detailOpen = false">
            <SquarePenIcon class="mr-1 size-4" />
            编辑草稿
          </UiButton>
        </UiDialogFooter>
      </UiDialogContent>
    </UiDialog>

    <UiAlertDialog :open="deletingTemplate !== null" @update:open="value => !value ? deletingTemplate = null : undefined">
      <UiAlertDialogContent>
        <UiAlertDialogHeader>
          <UiAlertDialogTitle>确认删除 Prompt 版本</UiAlertDialogTitle>
          <UiAlertDialogDescription>
            删除后该版本将不再占用版本号。ACTIVE 版本不允许删除，请先发布其他版本替换。
          </UiAlertDialogDescription>
        </UiAlertDialogHeader>
        <div v-if="deletingTemplate" class="rounded-md border bg-muted/30 p-3 text-sm">
          <div class="font-medium">
            {{ deletingTemplate.templateKey }}
          </div>
          <div class="mt-1 text-muted-foreground">
            {{ deletingTemplate.version }} / {{ deletingTemplate.environment }} / {{ deletingTemplate.status }}
          </div>
        </div>
        <UiAlertDialogFooter>
          <UiAlertDialogCancel @click="deletingTemplate = null">
            取消
          </UiAlertDialogCancel>
          <UiButton variant="destructive" :disabled="isDeleting" @click="handleDeleteTemplate">
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
