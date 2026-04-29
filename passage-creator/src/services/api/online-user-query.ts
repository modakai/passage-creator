import type { OnlineUserQuery } from '@/services/types/online-user.type'

/**
 * 归一化在线用户查询条件，避免空字符串污染后端筛选。
 */
export function normalizeOnlineUserQuery(query: OnlineUserQuery) {
  const normalized: Record<string, any> = {
    page: query.page ?? 1,
    pageSize: query.pageSize ?? 10,
  }

  // 只提交有意义的筛选条件，空字符串和空白字符会被忽略。
  for (const key of ['userId', 'userAccount', 'userName', 'userRole', 'loginIp', 'loginStartTime', 'loginEndTime'] as const) {
    const value = query[key]
    if (typeof value === 'string') {
      const trimmed = value.trim()
      if (trimmed) {
        normalized[key] = trimmed
      }
      continue
    }
    if (value !== undefined && value !== null) {
      normalized[key] = value
    }
  }

  return normalized
}
