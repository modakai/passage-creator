import type { IPaginationRequestQuery } from './response.type'

/**
 * 人工充值套餐。
 */
export interface ManualRechargePackage {
  packageId: string
  name: string
  amount: number
  credits: number
  sortOrder?: number
}

/**
 * 人工充值收款信息。
 */
export interface ManualRechargePayment {
  wechatQrCodeUrl?: string
  alipayQrCodeUrl?: string
  paymentRemarkTip?: string
  auditTip?: string
}

/**
 * 人工充值申请。
 */
export interface ManualRechargeApplication {
  id: string
  rechargeNo: string
  userId: string
  packageId: string
  amount: number
  credits: number
  payMethod: string
  status: string
  userRemark?: string
  adminRemark?: string
  auditTime?: string
  auditor?: string
  createTime?: string
  updateTime?: string
  payment?: ManualRechargePayment
}

/**
 * 用户创建人工充值申请表单。
 */
export interface ManualRechargeCreateForm {
  packageId: string
  payMethod?: 'WECHAT' | 'ALIPAY' | 'UNKNOWN'
  userRemark?: string
}

/**
 * 人工充值申请分页查询参数。
 */
export interface ManualRechargeQuery extends IPaginationRequestQuery {
  page: number
  pageSize: number
  userId?: string
  status?: string
  rechargeNo?: string
}

/**
 * 管理员审核人工充值申请表单。
 */
export interface ManualRechargeReviewForm {
  id: string
  adminRemark?: string
}

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

const MANUAL_RECHARGE_STATUS_LABELS: Record<string, string> = {
  PENDING: '待审核',
  APPROVED: '已到账',
  REJECTED: '已拒绝',
}

const MANUAL_RECHARGE_PAY_METHOD_LABELS: Record<string, string> = {
  WECHAT: '微信',
  ALIPAY: '支付宝',
  UNKNOWN: '未知',
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

/**
 * 将人工充值申请状态转换为用户可理解的中文描述。
 */
export function getManualRechargeStatusLabel(value?: string) {
  return value ? MANUAL_RECHARGE_STATUS_LABELS[value] ?? value : '-'
}

/**
 * 将人工充值付款方式转换为用户可理解的中文描述。
 */
export function getManualRechargePayMethodLabel(value?: string) {
  return value ? MANUAL_RECHARGE_PAY_METHOD_LABELS[value] ?? value : '-'
}
