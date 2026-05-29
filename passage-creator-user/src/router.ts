import { createRouter, createWebHistory } from 'vue-router'

import ArticleCreatorView from '@/views/ArticleCreatorView.vue'
import AuthView from '@/views/AuthView.vue'
import CreditsView from '@/views/CreditsView.vue'
import HomeView from '@/views/HomeView.vue'
import ProfileView from '@/views/ProfileView.vue'
import RednoteCreatorView from '@/views/RednoteCreatorView.vue'
import TasksView from '@/views/TasksView.vue'
import WorksView from '@/views/WorksView.vue'
import { isLoggedIn } from '@/services/session'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/auth', name: 'auth', component: AuthView, meta: { guestOnly: true } },
    { path: '/article-creator', name: 'article-creator', component: ArticleCreatorView, meta: { auth: true } },
    { path: '/rednote-creator', name: 'rednote-creator', component: RednoteCreatorView, meta: { auth: true } },
    { path: '/works', name: 'works', component: WorksView, meta: { auth: true } },
    { path: '/tasks', name: 'tasks', component: TasksView, meta: { auth: true } },
    { path: '/credits', name: 'credits', component: CreditsView, meta: { auth: true } },
    { path: '/profile', name: 'profile', component: ProfileView, meta: { auth: true } },
  ],
})

router.beforeEach((to) => {
  // 创作和个人数据页面必须先登录，首页保持公开以承接一句话创作入口。
  if (to.meta.auth && !isLoggedIn.value) {
    return {
      path: '/auth',
      query: {
        mode: 'login',
        return: to.fullPath,
      },
    }
  }

  // 已登录用户再次进入登录页时回首页，避免重复提交登录表单。
  if (to.meta.guestOnly && isLoggedIn.value) {
    return '/'
  }
})
