package com.sakura.passage_creator.billing.service;

import java.math.BigDecimal;

/**
 * 积分账本计算器，只负责纯金额计算，避免业务服务里散落多退少补规则。
 */
public class CreditLedgerCalculator {

    private final AiTokenCostCalculator amountNormalizer = new AiTokenCostCalculator();

    /**
     * 根据预扣金额和真实成本计算结算后的余额、退款金额和补扣金额。
     */
    public CreditLedgerSettlement settleReserved(BigDecimal currentBalance, BigDecimal reservedAmount,
            BigDecimal actualCost) {
        BigDecimal balance = normalize(currentBalance);
        BigDecimal reserved = normalize(reservedAmount);
        BigDecimal cost = normalize(actualCost);
        BigDecimal refund = normalize(reserved.subtract(cost).max(BigDecimal.ZERO));
        BigDecimal extraDebit = normalize(cost.subtract(reserved).max(BigDecimal.ZERO));
        BigDecimal balanceAfter = normalize(balance.add(refund).subtract(extraDebit));
        return new CreditLedgerSettlement(balanceAfter, refund, extraDebit);
    }

    private BigDecimal normalize(BigDecimal value) {
        return amountNormalizer.normalize(value);
    }
}
