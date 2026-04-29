/**
 * 清理通知公告接口查询参数中的空字符串。
 */
export function normalizeNotificationQuery<T extends Record<string, any>>(query: T) {
  return Object.fromEntries(Object.entries(query).map(([key, value]) => [key, value === '' ? undefined : value]))
}
