/**
 * 管理端用户查询参数。
 */
interface UserQueryLike {
  page: number
  pageSize: number
  userName?: string
  userRole?: string
  status?: number | ''
}

/**
 * 管理端字典类型查询参数。
 */
interface DictTypeQueryLike {
  page: number
  pageSize: number
  dictCode?: string
  dictName?: string
  status?: number | ''
}

/**
 * 管理端字典明细查询参数。
 */
interface DictItemQueryLike {
  page: number
  pageSize: number
  dictTypeId?: string | number
  dictLabel?: string
  dictValue?: string
  status?: number | ''
}

/**
 * 统一将实体主键转换为字符串，避免长整型 id 在前端被 number 精度截断。
 */
export function normalizeEntityId(id: string | number) {
  return String(id)
}

/**
 * 仅当存在有效主键且页面明确允许时，才启用详情查询。
 */
export function isDetailQueryEnabled(id?: string | number | null, enabled = true) {
  return Boolean(id) && enabled
}

/**
 * 将空字符串筛选值转换为 undefined，避免后端按空值误筛。
 */
export function normalizeUserQuery(query: UserQueryLike) {
  return {
    ...query,
    userName: query.userName?.trim() || undefined,
    userRole: query.userRole?.trim() || undefined,
    status: query.status === '' ? undefined : query.status,
  }
}

/**
 * 归一化字典类型筛选条件。
 */
export function normalizeDictTypeQuery(query: DictTypeQueryLike) {
  return {
    ...query,
    dictCode: query.dictCode?.trim() || undefined,
    dictName: query.dictName?.trim() || undefined,
    status: query.status === '' ? undefined : query.status,
  }
}

/**
 * 归一化字典明细筛选条件。
 */
export function normalizeDictItemQuery(query: DictItemQueryLike) {
  return {
    ...query,
    dictLabel: query.dictLabel?.trim() || undefined,
    dictValue: query.dictValue?.trim() || undefined,
    status: query.status === '' ? undefined : query.status,
  }
}
