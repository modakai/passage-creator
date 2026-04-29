<script setup lang="ts">
import { LoaderCircleIcon, LogOutIcon, RefreshCwIcon, SearchIcon } from '@lucide/vue'
import { toast } from 'vue-sonner'

import type { OnlineUserItem, OnlineUserQuery } from '@/services/types/online-user.type'

import { BasicPage } from '@/components/global-layout'
import { useForceLogoutOnlineUserMutation, useGetOnlineUserPageQuery } from '@/services/api/online-user.api'

/**
 * 在线用户筛选条件。
 */
const query = reactive<OnlineUserQuery>({
  page: 1,
  pageSize: 10,
  userId: '',
  userAccount: '',
  userName: '',
  userRole: '',
  loginIp: '',
  loginStartTime: '',
  loginEndTime: '',
})

const { data, isFetching, refetch } = useGetOnlineUserPageQuery(query)
const { mutateAsync: forceLogout, isPending: isForcingLogout } = useForceLogoutOnlineUserMutation()
const forcingUser = ref<OnlineUserItem | null>(null)

const onlineUsers = computed(() => data.value?.data?.records ?? [])
const total = computed(() => data.value?.data?.totalRow ?? 0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / query.pageSize)))

/**
 * 查询第一页在线用户。
 */
function handleSearch() {
  query.page = 1
  refetch()
}

/**
 * 重置筛选条件。
 */
function handleReset() {
  Object.assign(query, {
    page: 1,
    pageSize: 10,
    userId: '',
    userAccount: '',
    userName: '',
    userRole: '',
    loginIp: '',
    loginStartTime: '',
    loginEndTime: '',
  })
  refetch()
}

/**
 * 切换分页页码。
 */
function changePage(page: number) {
  query.page = Math.min(Math.max(page, 1), totalPages.value)
  refetch()
}

/**
 * 格式化时间展示。
 */
function formatTime(value?: string) {
  return value ? new Date(value).toLocaleString() : '-'
}

/**
 * 强制目标在线会话下线。
 */
async function handleForceLogout() {
  if (!forcingUser.value?.sessionId) {
    return
  }
  try {
    await forceLogout({ sessionId: forcingUser.value.sessionId })
    toast.success('已强制该用户下线')
    forcingUser.value = null
    refetch()
  }
  catch (error: any) {
    toast.error(error?.data?.message ?? error?.message ?? '强制下线失败')
  }
}
</script>

<template>
  <BasicPage title="在线用户" description="查看当前登录用户，并在安全需要时强制指定会话下线。" sticky>
    <template #actions>
      <UiButton variant="outline" :disabled="isFetching" @click="refetch()">
        <RefreshCwIcon class="mr-1 size-4" :class="{ 'animate-spin': isFetching }" />
        刷新
      </UiButton>
    </template>

    <div class="space-y-4">
      <UiCard class="border-border/70">
        <UiCardHeader>
          <UiCardTitle>筛选条件</UiCardTitle>
          <UiCardDescription>按用户、角色、IP 和登录时间定位在线会话。</UiCardDescription>
        </UiCardHeader>
        <UiCardContent class="grid gap-3 md:grid-cols-4 xl:grid-cols-6">
          <UiInput v-model="query.userId" placeholder="用户 ID" />
          <UiInput v-model="query.userAccount" placeholder="账号" />
          <UiInput v-model="query.userName" placeholder="昵称" />
          <UiInput v-model="query.userRole" placeholder="角色" />
          <UiInput v-model="query.loginIp" placeholder="登录 IP" />
          <UiInput v-model="query.loginStartTime" type="datetime-local" />
          <UiInput v-model="query.loginEndTime" type="datetime-local" />
          <div class="flex gap-2 md:col-span-2 xl:col-span-6">
            <UiButton @click="handleSearch">
              <SearchIcon class="mr-1 size-4" />
              查询
            </UiButton>
            <UiButton variant="outline" @click="handleReset">
              重置
            </UiButton>
          </div>
        </UiCardContent>
      </UiCard>

      <UiCard class="border-border/70">
        <UiCardHeader>
          <UiCardTitle>在线会话</UiCardTitle>
          <UiCardDescription>当前共 {{ total }} 个在线会话。</UiCardDescription>
        </UiCardHeader>
        <UiCardContent>
          <div v-if="isFetching" class="flex items-center justify-center py-12 text-sm text-muted-foreground">
            <LoaderCircleIcon class="mr-2 size-4 animate-spin" />
            正在加载在线用户...
          </div>

          <div v-else class="overflow-x-auto rounded-lg border">
            <table class="w-full text-sm">
              <thead class="bg-muted/50">
                <tr class="border-b text-left">
                  <th class="px-4 py-3">
                    账号
                  </th>
                  <th class="px-4 py-3">
                    昵称
                  </th>
                  <th class="px-4 py-3">
                    角色
                  </th>
                  <th class="px-4 py-3">
                    登录 IP
                  </th>
                  <th class="px-4 py-3">
                    客户端
                  </th>
                  <th class="px-4 py-3">
                    登录时间
                  </th>
                  <th class="px-4 py-3">
                    最近访问
                  </th>
                  <th class="px-4 py-3 text-right">
                    操作
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in onlineUsers" :key="item.sessionId" class="border-b last:border-b-0">
                  <td class="px-4 py-3">
                    <div class="font-medium">
                      {{ item.userAccount || '-' }}
                    </div>
                    <div class="text-xs text-muted-foreground">
                      {{ item.userId || '-' }}
                    </div>
                  </td>
                  <td class="px-4 py-3">
                    {{ item.userName || '-' }}
                  </td>
                  <td class="px-4 py-3">
                    {{ item.userRole || '-' }}
                  </td>
                  <td class="px-4 py-3">
                    {{ item.loginIp || '-' }}
                  </td>
                  <td class="max-w-[260px] truncate px-4 py-3 text-muted-foreground">
                    {{ item.clientInfo || '-' }}
                  </td>
                  <td class="px-4 py-3 text-muted-foreground">
                    {{ formatTime(item.loginTime) }}
                  </td>
                  <td class="px-4 py-3 text-muted-foreground">
                    {{ formatTime(item.lastAccessTime) }}
                  </td>
                  <td class="px-4 py-3 text-right">
                    <UiButton variant="outline" size="sm" :disabled="isForcingLogout" @click="forcingUser = item">
                      <LogOutIcon class="mr-1 size-4" />
                      强制下线
                    </UiButton>
                  </td>
                </tr>
                <tr v-if="onlineUsers.length === 0">
                  <td colspan="8" class="px-4 py-10 text-center text-muted-foreground">
                    暂无在线用户
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

    <UiAlertDialog :open="forcingUser !== null" @update:open="value => !value ? forcingUser = null : undefined">
      <UiAlertDialogContent>
        <UiAlertDialogHeader>
          <UiAlertDialogTitle>确认强制下线</UiAlertDialogTitle>
          <UiAlertDialogDescription>
            该用户后续访问受保护接口时需要重新登录。
          </UiAlertDialogDescription>
        </UiAlertDialogHeader>
        <UiAlertDialogFooter>
          <UiAlertDialogCancel @click="forcingUser = null">
            取消
          </UiAlertDialogCancel>
          <UiButton variant="destructive" :disabled="isForcingLogout" @click="handleForceLogout">
            确认下线
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
