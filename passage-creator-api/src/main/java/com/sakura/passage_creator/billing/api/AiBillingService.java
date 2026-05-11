package com.sakura.passage_creator.billing.api;

/**
 * AI 计费门面，供文章 Agent 在调用模型前预扣、调用后结算。
 */
public interface AiBillingService {

    /**
     * 文本模型调用前预扣积分。
     */
    AiBillingReservation reserveTextCall(Long userId, String taskId, String agentName, String phase,
            String provider, String model);

    /**
     * 图片模型调用前预扣积分。
     */
    AiBillingReservation reserveImageCall(Long userId, String taskId, String agentName, String phase,
            String provider, String model);

    /**
     * 文本模型调用完成后按真实 Token 用量结算。
     */
    void completeTextCall(AiBillingReservation reservation, AiTokenUsageSnapshot usage,
            Integer latencyMs, boolean responseOk, String errorMessage);

    /**
     * 图片模型调用完成后按固定成本结算。
     */
    void completeImageCall(AiBillingReservation reservation, Integer latencyMs, boolean responseOk, String errorMessage);

    /**
     * 模型调用失败且没有可计费结果时释放预扣积分。
     */
    void releaseReservation(AiBillingReservation reservation, Integer latencyMs, String errorMessage);
}
