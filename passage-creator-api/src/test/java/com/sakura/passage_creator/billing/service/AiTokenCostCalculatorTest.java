package com.sakura.passage_creator.billing.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * AI Token 成本计算测试，确保积分折算口径稳定。
 */
class AiTokenCostCalculatorTest {

    private final AiTokenCostCalculator calculator = new AiTokenCostCalculator();

    @Test
    void calculateTextCostShouldSeparatePromptAndCompletionPrices() {
        BigDecimal cost = calculator.calculateTextCost(
                new BigDecimal("0.0020"),
                new BigDecimal("0.0060"),
                1200L,
                345L);

        assertEquals(new BigDecimal("0.0045"), cost);
    }

    @Test
    void calculateTextCostShouldReturnZeroWhenUsageMissing() {
        BigDecimal cost = calculator.calculateTextCost(
                new BigDecimal("0.0020"),
                new BigDecimal("0.0060"),
                null,
                null);

        assertEquals(new BigDecimal("0.0000"), cost);
    }
}
