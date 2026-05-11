package com.sakura.passage_creator.article.agent;

import com.sakura.passage_creator.billing.api.AiTokenUsageSnapshot;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

/**
 * AI 聊天响应工具，集中提取正文和 Token 用量。
 */
public final class AiChatBillingSupport {

    private AiChatBillingSupport() {
    }

    /**
     * 从 ChatResponse 中提取模型文本输出。
     */
    public static String contentOf(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        return response.getResult().getOutput().getText();
    }

    /**
     * 从 Spring AI Usage 转换为账本模块的用量快照。
     */
    public static AiTokenUsageSnapshot usageOf(ChatResponse response) {
        if (response == null || response.getMetadata() == null || response.getMetadata().getUsage() == null) {
            return new AiTokenUsageSnapshot(0L, 0L, 0L);
        }
        Usage usage = response.getMetadata().getUsage();
        return AiTokenUsageSnapshot.of(usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
    }
}
