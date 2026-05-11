import type { IPaginationRequestQuery } from './response.type'

/**
 * 用户积分账户概览。
 */
export interface CreditSummary {
  userId: string
  balance: number
  totalRecharge: number
  totalConsume: number
}

/**
 * 积分流水条目。
 */
export interface CreditTransactionItem {
  id: number
  userId: string
  transactionType: string
  status: string
  amount: number
  balanceAfter: number
  bizType?: string
  bizId?: string
  description?: string
  operator?: string
  createTime?: string
}

/**
 * 积分流水查询参数。
 */
export interface CreditTransactionQuery extends IPaginationRequestQuery {
  page: number
  pageSize: number
  userId?: string
  transactionType?: string
  status?: string
  bizType?: string
  bizId?: string
  startTime?: string
  endTime?: string
}

/**
 * 管理员手动充值表单。
 */
export interface CreditRechargeForm {
  userId?: string
  amount?: number
  description?: string
}
