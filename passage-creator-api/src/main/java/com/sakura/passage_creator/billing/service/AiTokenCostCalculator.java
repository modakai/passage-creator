package com.sakura.passage_creator.billing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * AI Token 成本计算器，统一处理千 Token 单价和四位小数精度。
 */
public class AiTokenCostCalculator {

    private static final BigDecimal ONE_THOUSAND = new BigDecimal("1000");

    /**
     * 按输入 Token 和输出 Token 分别计价，缺失用量按 0 处理。
     */
    public BigDecimal calculateTextCost(BigDecimal promptTokenPricePer1K, BigDecimal completionTokenPricePer1K,
            Long promptTokens, Long completionTokens) {
        BigDecimal promptCost = safePrice(promptTokenPricePer1K)
                .multiply(BigDecimal.valueOf(safeToken(promptTokens)))
                .divide(ONE_THOUSAND, 8, RoundingMode.HALF_UP);
        BigDecimal completionCost = safePrice(completionTokenPricePer1K)
                .multiply(BigDecimal.valueOf(safeToken(completionTokens)))
                .divide(ONE_THOUSAND, 8, RoundingMode.HALF_UP);
        return normalize(promptCost.add(completionCost));
    }

    /**
     * 统一积分金额精度，数据库和前端都按四位小数展示。
     */
    public BigDecimal normalize(BigDecimal value) {
        return safePrice(value).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal safePrice(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private long safeToken(Long value) {
        return value == null ? 0L : Math.max(0L, value);
    }
}
