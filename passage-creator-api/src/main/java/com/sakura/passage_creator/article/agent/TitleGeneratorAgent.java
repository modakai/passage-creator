package com.sakura.passage_creator.article.agent;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.constant.PromptConstant;
import com.sakura.passage_creator.billing.api.AiBillingReservation;
import com.sakura.passage_creator.billing.api.AiBillingService;
import com.sakura.passage_creator.billing.api.AiChatBillingSupport;
import com.sakura.passage_creator.prompt.api.PromptTemplateRenderResult;
import com.sakura.passage_creator.prompt.api.PromptTemplateService;
import com.sakura.passage_creator.prompt.api.PromptUsageLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 标题生成Agent
 *
 * @author sakura
 * @create 2026-04
 */
@Component
@Slf4j
public class TitleGeneratorAgent {

    /**
     * DashScope 聊天模型。
     */
    private final ChatModel chatModel;

    private final ChatClient chatClient;

    /**
     * Prompt 模板服务，运行时读取 ACTIVE 版本并支持默认模板兜底。
     */
    private final PromptTemplateService promptTemplateService;

    /**
     * Prompt 使用日志服务，用于追溯 Agent 实际使用的模板版本。
     */
    private final PromptUsageLogService promptUsageLogService;

    public TitleGeneratorAgent(DashScopeApi dashScopeApi, PromptTemplateService promptTemplateService,
                               PromptUsageLogService promptUsageLogService, AiBillingService aiBillingService) {
        this.promptTemplateService = promptTemplateService;
        this.promptUsageLogService = promptUsageLogService;
        this.aiBillingService = aiBillingService;
        this.chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .model(DashScopeModel.ChatModel.QWEN3_MAX.value)
                        .temperature(0.1)
                        .maxToken(2000)
                        .topP(0.9)
                        .build())
                .build();

        this.chatClient = ChatClient.builder(chatModel).build();
    }

    /**
     * AI 计费服务，负责调用前预扣和调用后按 Token 结算。
     */
    private final AiBillingService aiBillingService;

    /**
     * 生成标题
     *
     * @param state 状态
     */
    public void generatorTitle(ArticleState state, Long userId) {
        log.info("阶段1：开始生成标题方案, taskId={}", state.getTaskId());
        String topic = state.getTopic();

        PromptTemplateRenderResult systemPrompt = promptTemplateService.renderActive(
                PromptConstant.AGENT1_TITLE_SYSTEM_KEY, PromptConstant.AGENT1_TITLE_SYSTEM_PROMPT, null, Map.of());
        PromptTemplateRenderResult userPrompt = promptTemplateService.renderActive(
                PromptConstant.AGENT1_TITLE_USER_KEY, PromptConstant.AGENT1_TITLE_PROMPT,
                PromptConstant.AGENT1_TITLE_VARIABLE_SCHEMA, Map.of("topic", topic));
        long startMillis = System.currentTimeMillis();
        AiBillingReservation reservation = aiBillingService.reserveTextCall(userId, state.getTaskId(),
                "TitleGeneratorAgent", "TITLE_GENERATING", "DASHSCOPE", DashScopeModel.ChatModel.QWEN3_MAX.value);
        boolean billed = false;
        try {
            ChatResponse chatResponse = chatClient.prompt()
                    .system(s -> s.text(systemPrompt.content()))
                    .user(u -> u.text(userPrompt.content()))
                    .call()
                    .chatResponse();
            String response = AiChatBillingSupport.contentOf(chatResponse);
            aiBillingService.completeTextCall(reservation, AiChatBillingSupport.usageOf(chatResponse),
                    resolveLatency(startMillis), true, null);
            billed = true;
            List<ArticleState.TitleOption> optionList = JSONUtil.toBean(response,
                    new TypeReference<List<ArticleState.TitleOption>>() {
                    }, true);

            state.setTitleOptions(optionList);
            recordPromptUsage(systemPrompt, userPrompt, state.getTaskId(), true, null, startMillis);
            log.info("阶段1：生成标题方案完毕，taskId={}", state.getTaskId());
        } catch (RuntimeException e) {
            if (!billed) {
                aiBillingService.releaseReservation(reservation, resolveLatency(startMillis), e.getMessage());
            }
            recordPromptUsage(systemPrompt, userPrompt, state.getTaskId(), false, e.getMessage(), startMillis);
            throw e;
        }
    }

    /**
     * 记录标题 Agent 本次调用使用的系统 Prompt 和用户 Prompt。
     */
    private void recordPromptUsage(PromptTemplateRenderResult systemPrompt, PromptTemplateRenderResult userPrompt,
                                   String taskId, boolean responseOk, String errorMessage, long startMillis) {
        Integer latencyMs = Math.toIntExact(Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - startMillis));
        promptUsageLogService.recordUsage(systemPrompt, "TitleGeneratorAgent", taskId, responseOk, errorMessage, latencyMs);
        promptUsageLogService.recordUsage(userPrompt, "TitleGeneratorAgent", taskId, responseOk, errorMessage, latencyMs);
    }

    private Integer resolveLatency(long startMillis) {
        return Math.toIntExact(Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - startMillis));
    }
}
