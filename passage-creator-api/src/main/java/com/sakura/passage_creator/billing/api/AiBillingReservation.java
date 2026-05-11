package com.sakura.passage_creator.billing.api;

import java.math.BigDecimal;

/**
 * AI 调用预扣记录，Agent 调用完成后用它结算或释放积分。
 */
public record AiBillingReservation(
        Long transactionId,
        Long userId,
        String taskId,
        String agentName,
        String phase,
        String provider,
        String model,
        String requestType,
        BigDecimal reservedCredits
) {
}
