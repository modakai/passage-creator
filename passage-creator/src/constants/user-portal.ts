import {
  BarChart3Icon,
  ChartColumnBigIcon,
  HomeIcon,
  ImageIcon,
  Layers3Icon,
  ListIcon,
  PenLineIcon,
  SettingsIcon,
  ShieldCheckIcon,
  SparklesIcon,
  WandSparklesIcon,
} from '@lucide/vue'

// 用户端顶部导航配置，同时用于移动端菜单和页面高亮。
export const userNavItems = [
  { label: '首页', to: '/', icon: HomeIcon },
  { label: '创作', to: '/article-creator', icon: PenLineIcon },
  { label: '文章管理', to: '/my-articles', icon: ListIcon },
  { label: '管理', to: '/dashboard', icon: SettingsIcon, adminOnly: true },
  { label: '数据', to: '/observability/system-status', icon: BarChart3Icon, adminOnly: true },
]

// 首页指标卡演示数据，突出 AI 图文创作平台的核心能力。
export const userMetrics = [
  { title: '智能体流程', value: '多节点', hint: '支持任务拆解、协作与自动编排', icon: Layers3Icon },
  { title: '配图方式', value: '7 种', hint: '覆盖多模型与 Nano Banana Pro', icon: ImageIcon },
  { title: '内容生成', value: '图文一体', hint: '文章、提示词与图片统一管理', icon: ChartColumnBigIcon },
]

export const userFeatures = [
  { title: 'AI 智能体协作', description: '围绕主题策划、内容生成、润色和配图形成可编排工作流。', icon: SparklesIcon },
  { title: '多模态配图', description: '预留 7 种配图方式，覆盖文生图、图生图和 Nano Banana Pro。', icon: WandSparklesIcon },
  { title: '后台协同', description: '具备后台角色时，可直接切换到管理端维护创作素材和平台配置。', icon: ShieldCheckIcon },
]

export const userHighlights = [
  { title: '智能体工作流教程', content: '演示从选题、成文到配图的完整协作链路。', time: '10 分钟前' },
  { title: '多模态生成能力', content: '逐步接入 7 种配图方式，方便对比不同生成策略。', time: '今天 09:20' },
  { title: '后台配置入口', content: '管理员可维护系统配置、用户和后续创作资源。', time: '今天 08:00' },
]
