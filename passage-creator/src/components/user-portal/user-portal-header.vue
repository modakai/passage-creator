<script setup lang="ts">
import { LogOutIcon, MenuIcon, UserIcon } from '@lucide/vue'

import { NotificationBell } from '@/components/notification-center'
import { useAuth } from '@/composables/use-auth'
import { userNavItems } from '@/constants/user-portal'

const route = useRoute()
const router = useRouter()
const { hasAdminAccess, isLogin, logout, session } = useAuth()

// 当前路由用于统一处理首页、创作页和历史页的导航高亮。
const activePath = computed(() => route.path)

// 后台相关入口只对管理员展示，普通用户不会看到不可访问的导航项。
const visibleNavItems = computed(() =>
  userNavItems.filter(item => !item.adminOnly || hasAdminAccess.value),
)

// 首页需要精确匹配，其他路由允许子页面继承父导航的高亮状态。
function isNavActive(path: string) {
  if (path === '/') {
    return activePath.value === '/'
  }

  return activePath.value === path || activePath.value.startsWith(`${path}/`)
}
</script>

<template>
  <header class="sticky top-0 z-40 border-b border-emerald-100 bg-white/95 backdrop-blur">
    <div class="mx-auto flex h-[74px] max-w-[1360px] items-center px-5 lg:px-8">
      <RouterLink to="/" class="flex shrink-0 items-center gap-3">
        <div class="grid size-10 place-items-center rounded-xl bg-emerald-100 text-emerald-600">
          <WandSparklesIcon class="size-6" />
        </div>
        <span class="whitespace-nowrap text-xl font-bold tracking-tight">AI文章创作器</span>
      </RouterLink>

      <nav class="mx-auto hidden h-full items-center md:flex">
        <RouterLink
          v-for="item in visibleNavItems"
          :key="item.to"
          :to="item.to"
          class="flex h-full items-center gap-2 px-6 font-medium transition-colors"
          :class="isNavActive(item.to) ? 'bg-emerald-50 font-semibold text-emerald-600' : 'text-slate-600 hover:bg-slate-50 hover:text-slate-950'"
        >
          <component :is="item.icon" class="size-5" />
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="ml-auto hidden items-center gap-4 md:flex">
        <template v-if="isLogin">
          <NotificationBell receiver-type="app" />
          <RouterLink to="/profile" class="flex items-center gap-3 rounded-full px-2 py-1 transition-colors hover:bg-slate-50">
            <div class="grid size-10 place-items-center rounded-full border border-slate-200 bg-white text-sm font-semibold">
              {{ session.user?.name?.slice(0, 1) || '用' }}
            </div>
            <span class="font-semibold">{{ session.user?.name }}</span>
          </RouterLink>
          <UiButton variant="ghost" size="sm" class="text-slate-500" @click="logout">
            <LogOutIcon class="mr-1 size-4" />
            退出
          </UiButton>
        </template>

        <UiButton
          v-else
          class="h-11 rounded-lg bg-emerald-500 px-8 font-semibold text-white shadow-[0_12px_28px_rgba(34,197,94,0.24)] hover:bg-emerald-600"
          @click="router.push('/auth/sign-in')"
        >
          登录
        </UiButton>
      </div>

      <UiDropdownMenu>
        <UiDropdownMenuTrigger as-child class="md:hidden">
          <UiButton variant="outline" size="icon">
            <MenuIcon class="size-4" />
          </UiButton>
        </UiDropdownMenuTrigger>
        <UiDropdownMenuContent align="end" class="w-56">
          <UiDropdownMenuItem
            v-for="item in visibleNavItems"
            :key="item.to"
            @click="router.push(item.to)"
          >
            <component :is="item.icon" class="mr-2 size-4" />
            {{ item.label }}
          </UiDropdownMenuItem>
          <UiDropdownMenuSeparator />
          <UiDropdownMenuItem v-if="isLogin" @click="router.push('/profile')">
            <UserIcon class="mr-2 size-4" />
            个人中心
          </UiDropdownMenuItem>
          <UiDropdownMenuItem v-if="isLogin" @click="logout">
            <LogOutIcon class="mr-2 size-4" />
            退出登录
          </UiDropdownMenuItem>
          <UiDropdownMenuItem v-else @click="router.push('/auth/sign-in')">
            用户登录
          </UiDropdownMenuItem>
        </UiDropdownMenuContent>
      </UiDropdownMenu>
    </div>
  </header>
</template>
