<script setup lang="ts">
import { BellPlusIcon, RefreshCwIcon } from '@lucide/vue'
import { toast } from 'vue-sonner'

import type { NotificationForm, NotificationQuery } from '@/services/types/notification.type'

import { BasicPage } from '@/components/global-layout'
import {
  useCreateNotificationMutation,
  useGetNotificationPageQuery,
  useNotificationActionMutation,
  useUpdateNotificationMutation,
} from '@/services/api/notification.api'

const ALL_SELECT_VALUE = '__all__'

/**
 * 通知公告查询条件。
 */
const query = reactive<NotificationQuery>({
  page: 1,
  pageSize: 10,
  type: '',
  title: '',
  status: '',
  receiverType: '',
  targetType: '',
})

/**
 * 新建通知公告表单。
 */
const form = reactive<NotificationForm>({
  type: 'message',
  title: '',
  summary: '',
  content: '',
  level: 'info',
  receiverType: 'all',
  targetType: 'all',
  targetRoles: [],
  targetUserIds: [],
  pinned: 0,
  popup: 0,
})

const open = ref(false)
const detail = ref<any | null>(null)
const targetRoleText = ref('')
const targetUserText = ref('')
const { data, isFetching, refetch } = useGetNotificationPageQuery(query)
const { mutateAsync: createNotification, isPending: isCreating } = useCreateNotificationMutation()
const { mutateAsync: updateNotification, isPending: isUpdating } = useUpdateNotificationMutation()
const publishMutation = useNotificationActionMutation('publish')
const revokeMutation = useNotificationActionMutation('revoke')
const archiveMutation = useNotificationActionMutation('archive')

const notifications = computed(() => data.value?.data?.records ?? [])
const total = computed(() => data.value?.data?.totalRow ?? 0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / query.pageSize)))

/**
 * Reka Select 不允许空字符串选项，UI 使用非空哨兵值，查询仍保持空字符串表示全部。
 */
function createAllOptionModel<T extends string>(getValue: () => T | '', setValue: (value: T | '') => void) {
  return computed({
    get: () => getValue() || ALL_SELECT_VALUE,
    set: value => setValue(value === ALL_SELECT_VALUE ? '' : value as T),
  })
}

const queryTypeModel = createAllOptionModel(() => query.type ?? '', value => query.type = value)
const queryStatusModel = createAllOptionModel(() => query.status ?? '', value => query.status = value)
const queryReceiverTypeModel = createAllOptionModel(() => query.receiverType ?? '', value => query.receiverType = value)

/**
 * 保存通知草稿。
 */
async function handleSubmit() {
  try {
    const payload = {
      ...form,
      targetRoles: splitValues(targetRoleText.value),
      targetUserIds: splitValues(targetUserText.value).map(Number).filter(Number.isFinite),
    }
    if (form.id) {
      await updateNotification(payload)
      toast.success('通知草稿已更新')
    }
    else {
      await createNotification(payload)
      toast.success('通知草稿已创建')
    }
    open.value = false
    resetForm()
    refetch()
  }
  catch (error: any) {
    toast.error(error?.data?.message ?? error?.message ?? '创建失败')
  }
}

/**
 * 打开编辑表单。
 */
function openEdit(item: any) {
  Object.assign(form, {
    id: item.id,
    type: item.type,
    title: item.title,
    summary: item.summary || '',
    content: item.content,
    level: item.level || 'info',
    receiverType: item.receiverType,
    targetType: item.targetType,
    targetRoles: item.targetRoles ?? [],
    targetUserIds: item.targetUserIds ?? [],
    pinned: item.pinned ?? 0,
    popup: item.popup ?? 0,
    linkUrl: item.linkUrl,
    effectiveTime: item.effectiveTime,
    expireTime: item.expireTime,
  })
  targetRoleText.value = (item.targetRoles ?? []).join(',')
  targetUserText.value = (item.targetUserIds ?? []).join(',')
  open.value = true
}

/**
 * 执行发布、撤回、归档动作。
 */
async function handleAction(id: number, action: 'publish' | 'revoke' | 'archive') {
  const actionMap = {
    publish: publishMutation,
    revoke: revokeMutation,
    archive: archiveMutation,
  }
  await actionMap[action].mutateAsync(id)
  toast.success('操作成功')
  refetch()
}

/**
 * 切换分页。
 */
function changePage(page: number) {
  query.page = Math.min(Math.max(page, 1), totalPages.value)
  refetch()
}

/**
 * 拆分逗号分隔的目标值。
 */
function splitValues(value: string) {
  return value.split(',').map(item => item.trim()).filter(Boolean)
}

/**
 * 重置新增表单。
 */
function resetForm() {
  Object.assign(form, {
    type: 'message',
    id: undefined,
    title: '',
    summary: '',
    content: '',
    level: 'info',
    receiverType: 'all',
    targetType: 'all',
    targetRoles: [],
    targetUserIds: [],
    pinned: 0,
    popup: 0,
  })
  targetRoleText.value = ''
  targetUserText.value = ''
}

/**
 * 更新置顶状态。
 */
function updatePinned(value: boolean | 'indeterminate') {
  form.pinned = value === true ? 1 : 0
}

/**
 * 更新弹窗状态。
 */
function updatePopup(value: boolean | 'indeterminate') {
  form.popup = value === true ? 1 : 0
}

/**
 * 状态标签样式。
 */
function statusVariant(status: string) {
  return status === 'published' ? 'default' : status === 'draft' ? 'secondary' : 'outline'
}
</script>

<template>
  <BasicPage title="通知公告" description="发布后台、用户端或全站范围内的通知消息与公告。" sticky>
    <template #actions>
      <UiDialog v-model:open="open">
        <UiDialogTrigger as-child>
          <UiButton>
            <BellPlusIcon class="mr-1 size-4" />
            新建通知
          </UiButton>
        </UiDialogTrigger>
        <UiDialogContent class="max-w-3xl">
          <UiDialogHeader>
            <UiDialogTitle>{{ form.id ? '编辑通知公告' : '新建通知公告' }}</UiDialogTitle>
            <UiDialogDescription>先保存为草稿，确认内容和范围后再发布。</UiDialogDescription>
          </UiDialogHeader>
          <div class="grid gap-4 md:grid-cols-2">
            <div class="space-y-2">
              <UiLabel>类型</UiLabel>
              <UiSelect v-model="form.type">
                <UiSelectTrigger><UiSelectValue /></UiSelectTrigger>
                <UiSelectContent>
                  <UiSelectItem value="message">
                    通知消息
                  </UiSelectItem>
                  <UiSelectItem value="announcement">
                    公告
                  </UiSelectItem>
                </UiSelectContent>
              </UiSelect>
            </div>
            <div class="space-y-2">
              <UiLabel>接收端范围</UiLabel>
              <UiSelect v-model="form.receiverType">
                <UiSelectTrigger><UiSelectValue /></UiSelectTrigger>
                <UiSelectContent>
                  <UiSelectItem value="all">
                    全部用户
                  </UiSelectItem>
                  <UiSelectItem value="admin">
                    系统后台用户
                  </UiSelectItem>
                  <UiSelectItem value="app">
                    用户端用户
                  </UiSelectItem>
                </UiSelectContent>
              </UiSelect>
            </div>
            <div class="space-y-2">
              <UiLabel>标题</UiLabel>
              <UiInput v-model="form.title" placeholder="例如：账户安全提醒" />
            </div>
            <div class="space-y-2">
              <UiLabel>目标范围</UiLabel>
              <UiSelect v-model="form.targetType">
                <UiSelectTrigger><UiSelectValue /></UiSelectTrigger>
                <UiSelectContent>
                  <UiSelectItem value="all">
                    范围内全部用户
                  </UiSelectItem>
                  <UiSelectItem value="role">
                    指定角色
                  </UiSelectItem>
                  <UiSelectItem value="user">
                    指定用户
                  </UiSelectItem>
                </UiSelectContent>
              </UiSelect>
            </div>
            <div v-if="form.targetType === 'role'" class="space-y-2">
              <UiLabel>目标角色</UiLabel>
              <UiInput v-model="targetRoleText" placeholder="admin,user，多个用英文逗号分隔" />
            </div>
            <div v-if="form.targetType === 'user'" class="space-y-2">
              <UiLabel>目标用户 ID</UiLabel>
              <UiInput v-model="targetUserText" placeholder="1001,1002，多个用英文逗号分隔" />
            </div>
            <div class="space-y-2 md:col-span-2">
              <UiLabel>摘要</UiLabel>
              <UiInput v-model="form.summary" placeholder="用于列表中快速预览" />
            </div>
            <div class="space-y-2 md:col-span-2">
              <UiLabel>内容</UiLabel>
              <UiTextarea v-model="form.content" rows="6" placeholder="填写通知正文" />
            </div>
            <div class="flex items-center gap-6 md:col-span-2">
              <label class="flex items-center gap-2 text-sm">
                <UiCheckbox :checked="form.pinned === 1" @update:checked="updatePinned" />
                置顶
              </label>
              <label class="flex items-center gap-2 text-sm">
                <UiCheckbox :checked="form.popup === 1" @update:checked="updatePopup" />
                弹窗公告
              </label>
            </div>
          </div>
          <UiDialogFooter>
            <UiButton variant="outline" @click="open = false">
              取消
            </UiButton>
            <UiButton :disabled="isCreating || isUpdating" @click="handleSubmit">
              保存草稿
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
        <UiCardContent class="grid gap-4 pt-6 md:grid-cols-5">
          <UiInput v-model="query.title" placeholder="搜索标题" @keyup.enter="refetch()" />
          <UiSelect v-model="queryTypeModel">
            <UiSelectTrigger><UiSelectValue placeholder="全部类型" /></UiSelectTrigger>
            <UiSelectContent>
              <UiSelectItem :value="ALL_SELECT_VALUE">
                全部类型
              </UiSelectItem>
              <UiSelectItem value="message">
                通知消息
              </UiSelectItem>
              <UiSelectItem value="announcement">
                公告
              </UiSelectItem>
            </UiSelectContent>
          </UiSelect>
          <UiSelect v-model="queryStatusModel">
            <UiSelectTrigger><UiSelectValue placeholder="全部状态" /></UiSelectTrigger>
            <UiSelectContent>
              <UiSelectItem :value="ALL_SELECT_VALUE">
                全部状态
              </UiSelectItem>
              <UiSelectItem value="draft">
                草稿
              </UiSelectItem>
              <UiSelectItem value="published">
                已发布
              </UiSelectItem>
              <UiSelectItem value="revoked">
                已撤回
              </UiSelectItem>
              <UiSelectItem value="archived">
                已归档
              </UiSelectItem>
            </UiSelectContent>
          </UiSelect>
          <UiSelect v-model="queryReceiverTypeModel">
            <UiSelectTrigger><UiSelectValue placeholder="全部接收端" /></UiSelectTrigger>
            <UiSelectContent>
              <UiSelectItem :value="ALL_SELECT_VALUE">
                全部接收端
              </UiSelectItem>
              <UiSelectItem value="all">
                全部用户
              </UiSelectItem>
              <UiSelectItem value="admin">
                后台用户
              </UiSelectItem>
              <UiSelectItem value="app">
                用户端用户
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
          <UiCardTitle>通知列表</UiCardTitle>
          <UiCardDescription>共 {{ total }} 条记录</UiCardDescription>
        </UiCardHeader>
        <UiCardContent>
          <div class="overflow-x-auto rounded-lg border">
            <table class="w-full text-sm">
              <thead class="bg-muted/50">
                <tr class="border-b text-left">
                  <th class="px-4 py-3">
                    标题
                  </th>
                  <th class="px-4 py-3">
                    类型
                  </th>
                  <th class="px-4 py-3">
                    接收端
                  </th>
                  <th class="px-4 py-3">
                    状态
                  </th>
                  <th class="px-4 py-3">
                    发布时间
                  </th>
                  <th class="px-4 py-3 text-right">
                    操作
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in notifications" :key="item.id" class="border-b last:border-b-0">
                  <td class="px-4 py-3">
                    <div class="font-medium">
                      {{ item.title }}
                    </div>
                    <div class="mt-1 line-clamp-1 text-xs text-muted-foreground">
                      {{ item.summary || item.content }}
                    </div>
                  </td>
                  <td class="px-4 py-3">
                    {{ item.type === 'message' ? '通知' : '公告' }}
                  </td>
                  <td class="px-4 py-3">
                    {{ item.receiverType }}
                  </td>
                  <td class="px-4 py-3">
                    <UiBadge :variant="statusVariant(item.status)">
                      {{ item.status }}
                    </UiBadge>
                  </td>
                  <td class="px-4 py-3 text-muted-foreground">
                    {{ item.publishTime ? new Date(item.publishTime).toLocaleString() : '-' }}
                  </td>
                  <td class="px-4 py-3">
                    <div class="flex justify-end gap-2">
                      <UiButton size="sm" variant="outline" @click="detail = item">
                        查看
                      </UiButton>
                      <UiButton size="sm" variant="outline" :disabled="item.status !== 'draft'" @click="openEdit(item)">
                        编辑
                      </UiButton>
                      <UiButton size="sm" variant="outline" :disabled="item.status !== 'draft'" @click="handleAction(item.id, 'publish')">
                        发布
                      </UiButton>
                      <UiButton size="sm" variant="outline" :disabled="item.status !== 'published'" @click="handleAction(item.id, 'revoke')">
                        撤回
                      </UiButton>
                      <UiButton size="sm" variant="outline" @click="handleAction(item.id, 'archive')">
                        归档
                      </UiButton>
                    </div>
                  </td>
                </tr>
                <tr v-if="notifications.length === 0">
                  <td colspan="6" class="px-4 py-10 text-center text-muted-foreground">
                    暂无通知公告
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

    <UiDialog :open="detail !== null" @update:open="value => !value ? detail = null : undefined">
      <UiDialogContent>
        <UiDialogHeader>
          <UiDialogTitle>{{ detail?.title }}</UiDialogTitle>
          <UiDialogDescription>{{ detail?.receiverType }} / {{ detail?.status }}</UiDialogDescription>
        </UiDialogHeader>
        <div class="whitespace-pre-wrap text-sm leading-7">
          {{ detail?.content }}
        </div>
      </UiDialogContent>
    </UiDialog>
  </BasicPage>
</template>

<route lang="yaml">
meta:
  auth: true
</route>
