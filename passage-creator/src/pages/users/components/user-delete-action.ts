import type { UserEntityId } from '@/services/types/user.type'

type DeleteUserRequest = (id: UserEntityId) => Promise<unknown>

interface DeleteSelectedUserOptions {
  user: {
    id?: UserEntityId | null
  } | null | undefined
  deleteUser: DeleteUserRequest
}

/**
 * 统一解析删除用户的目标 id，字符串长整型必须原样保留，避免 JS Number 精度丢失。
 */
function normalizeDeleteUserId(id: UserEntityId | null | undefined): UserEntityId | null {
  if (id === null || id === undefined) {
    return null
  }

  if (typeof id === 'string') {
    const trimmedId = id.trim()

    return trimmedId.length > 0 ? trimmedId : null
  }

  return Number.isFinite(id) && id > 0 ? id : null
}

/**
 * 执行删除用户请求，并返回是否真正发起了删除。
 */
export async function deleteSelectedUser(options: DeleteSelectedUserOptions) {
  const userId = normalizeDeleteUserId(options.user?.id)

  if (userId === null) {
    return false
  }

  await options.deleteUser(userId)

  return true
}
