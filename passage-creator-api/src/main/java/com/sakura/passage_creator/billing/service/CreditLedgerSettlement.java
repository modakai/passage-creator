package com.sakura.passage_creator.billing.service;

import java.math.BigDecimal;

/**
 * 积分预扣结算结果。
 */
public record CreditLedgerSettlement(BigDecimal balanceAfter, BigDecimal refundAmount, BigDecimal extraDebitAmount) {
}
