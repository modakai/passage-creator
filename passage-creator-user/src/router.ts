import { createRouter, createWebHistory } from 'vue-router'

import ArticleCreatorView from '@/views/ArticleCreatorView.vue'
import CreditsView from '@/views/CreditsView.vue'
import HomeView from '@/views/HomeView.vue'
import ProfileView from '@/views/ProfileView.vue'
import RednoteCreatorView from '@/views/RednoteCreatorView.vue'
import TasksView from '@/views/TasksView.vue'
import WorksView from '@/views/WorksView.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/article-creator', name: 'article-creator', component: ArticleCreatorView },
    { path: '/rednote-creator', name: 'rednote-creator', component: RednoteCreatorView },
    { path: '/works', name: 'works', component: WorksView },
    { path: '/tasks', name: 'tasks', component: TasksView },
    { path: '/credits', name: 'credits', component: CreditsView },
    { path: '/profile', name: 'profile', component: ProfileView },
  ],
})
