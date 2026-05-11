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
 * 管理端用户积分账户。
 */
export interface CreditAccountItem extends CreditSummary {
  id: string
  createTime?: string
  updateTime?: string
}

/**
 * 管理端用户积分账户查询参数。
 */
export interface CreditAccountQuery extends IPaginationRequestQuery {
  page: number
  pageSize: number
  userId?: string
  positiveBalanceOnly?: boolean
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

const CREDIT_TRANSACTION_TYPE_LABELS: Record<string, string> = {
  RECHARGE: '充值',
  RESERVE: '预扣',
  CONSUME: '消费',
  REFUND: '退款',
  ADJUST: '人工调整',
}

const CREDIT_TRANSACTION_STATUS_LABELS: Record<string, string> = {
  RESERVED: '预扣中',
  COMPLETED: '已完成',
  RELEASED: '已释放',
}

/**
 * 将积分流水类型枚举转换为用户可理解的中文描述。
 */
export function getCreditTransactionTypeLabel(value?: string) {
  return value ? CREDIT_TRANSACTION_TYPE_LABELS[value] ?? value : '-'
}

/**
 * 将积分流水状态枚举转换为用户可理解的中文描述。
 */
export function getCreditTransactionStatusLabel(value?: string) {
  return value ? CREDIT_TRANSACTION_STATUS_LABELS[value] ?? value : '-'
}
