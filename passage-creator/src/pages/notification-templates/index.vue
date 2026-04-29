<script setup lang="ts">
import { FileCode2Icon, RefreshCwIcon } from '@lucide/vue'
import { toast } from 'vue-sonner'

import type { NotificationTemplateForm, NotificationTemplateQuery } from '@/services/types/notification.type'

import { BasicPage } from '@/components/global-layout'
import {
  useCreateNotificationTemplateMutation,
  useGetNotificationTemplatePageQuery,
  useNotificationTemplateEnabledMutation,
  useUpdateNotificationTemplateMutation,
} from '@/services/api/notification.api'

const ALL_SELECT_VALUE = '__all__'

/**
 * 模板分页查询条件。
 */
const query = reactive<NotificationTemplateQuery>({
  page: 1,
  pageSize: 10,
  templateCode: '',
  eventType: '',
  enabled: '',
})

/**
 * 模板表单默认值。
 */
const form = reactive<NotificationTemplateForm>({
  templateCode: 'user_disabled',
  eventType: 'user_disabled',
  titleTemplate: '账户封禁通知',
  contentTemplate: '您好，您的账户被系统封禁，具体原因{reason}，如有疑虑可联系平台',
  variableSchema: '[{"name":"reason","required":true,"label":"封禁原因"}]',
  receiverType: 'app',
  enabled: 1,
  remark: '',
})

const open = ref(false)
const { data, isFetching, refetch } = useGetNotificationTemplatePageQuery(query)
const { mutateAsync: createTemplate, isPending: isCreating } = useCreateNotificationTemplateMutation()
const { mutateAsync: updateTemplate, isPending: isUpdating } = useUpdateNotificationTemplateMutation()
const enableMutation = useNotificationTemplateEnabledMutation('enable')
const disableMutation = useNotificationTemplateEnabledMutation('disable')
const templates = computed(() => data.value?.data?.records ?? [])
const total = computed(() => data.value?.data?.totalRow ?? 0)
const queryEnabledModel = computed({
  // Reka Select 不能使用空字符串选项，使用哨兵值代表全部状态。
  get: () => query.enabled === '' ? ALL_SELECT_VALUE : String(query.enabled),
  set: (value) => {
    query.enabled = value === ALL_SELECT_VALUE ? '' : Number(value)
  },
})

/**
 * 创建模板。
 */
async function handleSubmit() {
  try {
    if (form.id) {
      await updateTemplate({ ...form })
      toast.success('消息模板已更新')
    }
    else {
      await createTemplate({ ...form })
      toast.success('消息模板已保存')
    }
    open.value = false
    resetTemplateForm()
    refetch()
  }
  catch (error: any) {
    toast.error(error?.data?.message ?? error?.message ?? '保存失败')
  }
}

/**
 * 打开模板编辑表单。
 */
function openEdit(item: NotificationTemplateForm & { id: number }) {
  Object.assign(form, item)
  open.value = true
}

/**
 * 重置模板表单。
 */
function resetTemplateForm() {
  Object.assign(form, {
    id: undefined,
    templateCode: 'user_disabled',
    eventType: 'user_disabled',
    titleTemplate: '账户封禁通知',
    contentTemplate: '您好，您的账户被系统封禁，具体原因{reason}，如有疑虑可联系平台',
    variableSchema: '[{"name":"reason","required":true,"label":"封禁原因"}]',
    receiverType: 'app',
    enabled: 1,
    remark: '',
  })
}

/**
 * 切换模板启停状态。
 */
async function toggleEnabled(id: number, enabled: number) {
  if (enabled === 1) {
    await disableMutation.mutateAsync(id)
  }
  else {
    await enableMutation.mutateAsync(id)
  }
  toast.success('状态已更新')
  refetch()
}

/**
 * 更新模板启用状态。
 */
function updateEnabled(value: boolean | 'indeterminate') {
  form.enabled = value === true ? 1 : 0
}
</script>

<template>
  <BasicPage title="消息模板" description="维护系统事件自动通知模板，支持 {reason} 这类动态变量。" sticky>
    <template #actions>
      <UiDialog v-model:open="open">
        <UiDialogTrigger as-child>
          <UiButton>
            <FileCode2Icon class="mr-1 size-4" />
            新建模板
          </UiButton>
        </UiDialogTrigger>
        <UiDialogContent class="max-w-3xl">
          <UiDialogHeader>
            <UiDialogTitle>{{ form.id ? '编辑消息模板' : '新建消息模板' }}</UiDialogTitle>
            <UiDialogDescription>变量定义使用 JSON 数组，模板文本中使用 {变量名} 引用。</UiDialogDescription>
          </UiDialogHeader>
          <div class="grid gap-4 md:grid-cols-2">
            <div class="space-y-2">
              <UiLabel>模板编码</UiLabel>
              <UiInput v-model="form.templateCode" />
            </div>
            <div class="space-y-2">
              <UiLabel>事件类型</UiLabel>
              <UiInput v-model="form.eventType" />
            </div>
            <div class="space-y-2">
              <UiLabel>接收端</UiLabel>
              <UiSelect v-model="form.receiverType">
                <UiSelectTrigger><UiSelectValue /></UiSelectTrigger>
                <UiSelectContent>
                  <UiSelectItem value="admin">
                    系统后台用户
                  </UiSelectItem>
                  <UiSelectItem value="app">
                    用户端用户
                  </UiSelectItem>
                  <UiSelectItem value="all">
                    全部用户
                  </UiSelectItem>
                </UiSelectContent>
              </UiSelect>
            </div>
            <label class="flex items-end gap-2 text-sm">
              <UiCheckbox :checked="form.enabled === 1" @update:checked="updateEnabled" />
              启用模板
            </label>
            <div class="space-y-2 md:col-span-2">
              <UiLabel>标题模板</UiLabel>
              <UiInput v-model="form.titleTemplate" />
            </div>
            <div class="space-y-2 md:col-span-2">
              <UiLabel>内容模板</UiLabel>
              <UiTextarea v-model="form.contentTemplate" rows="5" />
            </div>
            <div class="space-y-2 md:col-span-2">
              <UiLabel>变量定义 JSON</UiLabel>
              <UiTextarea v-model="form.variableSchema" rows="4" />
            </div>
          </div>
          <UiDialogFooter>
            <UiButton variant="outline" @click="open = false">
              取消
            </UiButton>
            <UiButton :disabled="isCreating || isUpdating" @click="handleSubmit">
              保存模板
            </UiButton>
          </UiDialogFooter>
        </UiDialogContent>
      </UiDialog>
      <UiButton variant="outline" :disabled="isFetching" @click="refetch()">
        <RefreshCwIcon class="mr-1 size-4" :class="{ 'animate-spin': isFetching }" />
        刷新
      </UiButton>
    </template>

    <div class="space-y-4">
      <UiCard>
        <UiCardContent class="grid gap-4 pt-6 md:grid-cols-4">
          <UiInput v-model="query.templateCode" placeholder="模板编码" />
          <UiInput v-model="query.eventType" placeholder="事件类型" />
          <UiSelect v-model="queryEnabledModel">
            <UiSelectTrigger><UiSelectValue placeholder="全部状态" /></UiSelectTrigger>
            <UiSelectContent>
              <UiSelectItem :value="ALL_SELECT_VALUE">
                全部状态
              </UiSelectItem>
              <UiSelectItem value="1">
                启用
              </UiSelectItem>
              <UiSelectItem value="0">
                停用
              </UiSelectItem>
            </UiSelectContent>
          </UiSelect>
          <UiButton @click="query.page = 1; refetch()">
            筛选
          </UiButton>
        </UiCardContent>
      </UiCard>

      <UiCard>
        <UiCardHeader>
          <UiCardTitle>模板列表</UiCardTitle>
          <UiCardDescription>共 {{ total }} 条模板</UiCardDescription>
        </UiCardHeader>
        <UiCardContent>
          <div class="overflow-x-auto rounded-lg border">
            <table class="w-full text-sm">
              <thead class="bg-muted/50">
                <tr class="border-b text-left">
                  <th class="px-4 py-3">
                    模板编码
                  </th>
                  <th class="px-4 py-3">
                    事件类型
                  </th>
                  <th class="px-4 py-3">
                    标题模板
                  </th>
                  <th class="px-4 py-3">
                    接收端
                  </th>
                  <th class="px-4 py-3">
                    状态
                  </th>
                  <th class="px-4 py-3 text-right">
                    操作
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in templates" :key="item.id" class="border-b last:border-b-0">
                  <td class="px-4 py-3 font-medium">
                    {{ item.templateCode }}
                  </td>
                  <td class="px-4 py-3">
                    {{ item.eventType }}
                  </td>
                  <td class="px-4 py-3">
                    {{ item.titleTemplate }}
                  </td>
                  <td class="px-4 py-3">
                    {{ item.receiverType }}
                  </td>
                  <td class="px-4 py-3">
                    <UiBadge :variant="item.enabled === 1 ? 'default' : 'secondary'">
                      {{ item.enabled === 1 ? '启用' : '停用' }}
                    </UiBadge>
                  </td>
                  <td class="px-4 py-3 text-right">
                    <div class="flex justify-end gap-2">
                      <UiButton size="sm" variant="outline" @click="openEdit(item)">
                        编辑
                      </UiButton>
                      <UiButton size="sm" variant="outline" @click="toggleEnabled(item.id, item.enabled)">
                        {{ item.enabled === 1 ? '停用' : '启用' }}
                      </UiButton>
                    </div>
                  </td>
                </tr>
                <tr v-if="templates.length === 0">
                  <td colspan="6" class="px-4 py-10 text-center text-muted-foreground">
                    暂无消息模板
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </UiCardContent>
      </UiCard>
    </div>
  </BasicPage>
</template>

<route lang="yaml">
meta:
  auth: true
</route>
