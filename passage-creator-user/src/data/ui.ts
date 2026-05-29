export const inspirationCards = [
  { title: '趋势解读', desc: '从一句现象生成可发布的深度文章。', prompt: '写一篇关于 2026 内容营销趋势的深度文章' },
  { title: '小红书笔记', desc: '检索素材、规划图片、产出笔记一次完成。', prompt: '帮我写一篇夏季露营新手装备小红书笔记，配 3 张图' },
  { title: '知识科普', desc: '把复杂概念拆成清晰结构和例子。', prompt: '解释 AI Agent 如何帮助内容团队提升效率' },
  { title: '产品文案', desc: '围绕目标受众组织卖点和转化叙事。', prompt: '为一款 AI 写作工具写新品上市活动文案' },
]

export const workflowSteps = [
  { title: '一句话输入', desc: '用户只说目标，不填复杂表单。' },
  { title: '灵感扩写', desc: 'AI 生成标题、方向和结构。' },
  { title: '人工确认', desc: '关键节点由用户选择或修正。' },
  { title: '自动生成', desc: '正文、图片和融合结果自动推进。' },
  { title: '作品沉淀', desc: '保存为可继续编辑的创作记录。' },
]

export const statusTone: Record<string, string> = {
  COMPLETED: 'bg-emerald-50 text-emerald-700 border-emerald-100',
  PROCESSING: 'bg-blue-50 text-blue-700 border-blue-100',
  PENDING: 'bg-amber-50 text-amber-700 border-amber-100',
  FAILED: 'bg-rose-50 text-rose-700 border-rose-100',
}
