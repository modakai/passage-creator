import { createApp } from 'vue'

import App from './App.vue'
import { router } from './router'
import './styles.css'

// 独立用户端只挂载 Vue Router，避免继承后台管理端插件和全局布局。
createApp(App).use(router).mount('#app')
