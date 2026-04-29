/**
 * 清理审计日志查询参数，避免空字符串参与后端筛选。
 */
export function normalizeAuditLogQuery<T extends Record<string, any>>(query: T) {
  return Object.fromEntries(Object.entries(query).map(([key, value]) => {
    if (typeof value === 'string') {
      const trimmed = value.trim()
      return [key, trimmed === '' ? undefined : trimmed]
    }
    return [key, value === '' ? undefined : value]
  }))
}
