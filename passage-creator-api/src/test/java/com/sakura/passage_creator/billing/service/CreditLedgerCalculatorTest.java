package com.sakura.passage_creator.billing.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 积分预扣结算测试，覆盖多退少补的余额计算规则。
 */
class CreditLedgerCalculatorTest {

    private final CreditLedgerCalculator calculator = new CreditLedgerCalculator();

    @Test
    void settleReservedShouldRefundUnusedCredits() {
        CreditLedgerSettlement settlement = calculator.settleReserved(
                new BigDecimal("90.0000"),
                new BigDecimal("10.0000"),
                new BigDecimal("7.3456"));

        assertEquals(new BigDecimal("92.6544"), settlement.balanceAfter());
        assertEquals(new BigDecimal("2.6544"), settlement.refundAmount());
        assertEquals(new BigDecimal("0.0000"), settlement.extraDebitAmount());
    }

    @Test
    void settleReservedShouldDebitExtraCreditsWhenActualCostIsHigher() {
        CreditLedgerSettlement settlement = calculator.settleReserved(
                new BigDecimal("90.0000"),
                new BigDecimal("10.0000"),
                new BigDecimal("12.0000"));

        assertEquals(new BigDecimal("88.0000"), settlement.balanceAfter());
        assertEquals(new BigDecimal("0.0000"), settlement.refundAmount());
        assertEquals(new BigDecimal("2.0000"), settlement.extraDebitAmount());
    }
}
