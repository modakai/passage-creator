/**
 * 格式化后端时间字段，空值使用短横线保持卡片高度稳定。
 */
export function formatTime(value?: string) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/**
 * 解析后端 JSON 字符串数组，坏数据返回空数组避免页面崩溃。
 */
export function parseJsonArray<T>(value?: string) {
  if (!value) {
    return [] as T[]
  }
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed as T[] : []
  }
  catch {
    return [] as T[]
  }
}

/**
 * 将状态转换为用户能理解的中文标签。
 */
export function getStatusLabel(status?: string) {
  const labels: Record<string, string> = {
    PENDING: '等待处理',
    PROCESSING: '生成中',
    COMPLETED: '已完成',
    FAILED: '失败',
  }
  return status ? labels[status] ?? status : '未开始'
}
