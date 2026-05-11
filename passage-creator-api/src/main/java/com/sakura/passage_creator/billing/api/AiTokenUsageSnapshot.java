package com.sakura.passage_creator.billing.api;

/**
 * AI Token 用量快照，避免账本模块直接依赖具体模型 SDK 的 Usage 类型。
 */
public record AiTokenUsageSnapshot(Long promptTokens, Long completionTokens, Long totalTokens) {

    /**
     * 从 Spring AI Usage 返回的数字对象安全转换为 Long。
     */
    public static AiTokenUsageSnapshot of(Number promptTokens, Number completionTokens, Number totalTokens) {
        return new AiTokenUsageSnapshot(toLong(promptTokens), toLong(completionTokens), toLong(totalTokens));
    }

    private static Long toLong(Number value) {
        return value == null ? null : value.longValue();
    }
}
