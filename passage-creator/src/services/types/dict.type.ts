import type { IPaginationRequestQuery } from '@/services/types/response.type'

/**
 * 统一的实体主键类型。
 */
export type DictEntityId = string | number

/**
 * 字典选项。
 */
export interface DictOption {
  label: string
  value: string
}

/**
 * 字典类型列表项。
 */
export interface DictTypeItem {
  id: DictEntityId
  dictCode: string
  dictName: string
  status: number
  remark?: string
  createTime?: string
  updateTime?: string
}

/**
 * 字典明细列表项。
 */
export interface DictItemItem {
  id: DictEntityId
  dictTypeId: DictEntityId
  dictLabel: string
  dictValue: string
  sortOrder: number
  status: number
  tagType?: string
  remark?: string
  extJson?: string
  createTime?: string
  updateTime?: string
}

/**
 * 字典类型查询参数。
 */
export interface DictTypeQuery extends IPaginationRequestQuery {
  page: number
  pageSize: number
  dictCode?: string
  dictName?: string
  status?: number | ''
}

/**
 * 字典明细查询参数。
 */
export interface DictItemQuery extends IPaginationRequestQuery {
  page: number
  pageSize: number
  dictTypeId?: DictEntityId
  dictLabel?: string
  dictValue?: string
  status?: number | ''
}

/**
 * 字典类型表单参数。
 */
export interface DictTypeForm {
  id?: DictEntityId
  dictCode: string
  dictName: string
  status: number
  remark?: string
}

/**
 * 字典明细表单参数。
 */
export interface DictItemForm {
  id?: DictEntityId
  dictTypeId: DictEntityId
  dictLabel: string
  dictValue: string
  sortOrder: number
  status: number
  tagType?: string
  remark?: string
  extJson?: string
}
