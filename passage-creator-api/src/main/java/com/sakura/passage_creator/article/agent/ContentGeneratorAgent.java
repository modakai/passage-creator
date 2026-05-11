package com.sakura.passage_creator.article.agent;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.constant.PromptConstant;
import com.sakura.passage_creator.billing.api.AiBillingReservation;
import com.sakura.passage_creator.billing.api.AiBillingService;
import com.sakura.passage_creator.prompt.api.PromptTemplateRenderResult;
import com.sakura.passage_creator.prompt.api.PromptTemplateService;
import com.sakura.passage_creator.prompt.api.PromptUsageLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 正文生成 Agent，负责根据已确认标题和大纲生成 Markdown 正文。
 *
 * @author sakura
 * @create 2026-05
 */
@Component
@Slf4j
public class ContentGeneratorAgent {

    /**
     * Spring AI ChatClient，用于调用通义千问生成正文。
     */
    private final ChatClient chatClient;

    /**
     * Prompt 模板服务，运行时读取 ACTIVE 版本并支持默认模板兜底。
     */
    private final PromptTemplateService promptTemplateService;

    /**
     * Prompt 使用日志服务，用于追溯 Agent 实际使用的模板版本。
     */
    private final PromptUsageLogService promptUsageLogService;

    /**
     * AI 计费服务，负责正文模型调用的预扣和结算。
     */
    private final AiBillingService aiBillingService;

    public ContentGeneratorAgent(DashScopeApi dashScopeApi, PromptTemplateService promptTemplateService,
            PromptUsageLogService promptUsageLogService, AiBillingService aiBillingService) {
        this.promptTemplateService = promptTemplateService;
        this.promptUsageLogService = promptUsageLogService;
        this.aiBillingService = aiBillingService;
        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .model(DashScopeModel.ChatModel.QWEN3_MAX.value)
                        .temperature(0.2)
                        .maxToken(8000)
                        .topP(0.9)
                        .build())
                .build();

        this.chatClient = ChatClient.builder(chatModel).build();
    }

    /**
     * 生成文章正文并写回状态对象。
     *
     * @param state 文章生成状态，必须包含标题和大纲
     */
    public void generatorContent(ArticleState state, Long userId) {
        log.info("阶段3：开始生成正文, taskId={}", state.getTaskId());
        ArticleState.TitleResult title = state.getTitle();
        ArticleState.OutlineResult outline = state.getOutline();

        // 大纲作为 JSON 放入提示词，避免章节和要点在字符串拼接时丢失结构。
        PromptTemplateRenderResult systemPrompt = promptTemplateService.renderActive(
                PromptConstant.AGENT3_CONTENT_SYSTEM_KEY, PromptConstant.AGENT3_CONTENT_SYSTEM_PROMPT, null, Map.of());
        PromptTemplateRenderResult userPrompt = promptTemplateService.renderActive(
                PromptConstant.AGENT3_CONTENT_USER_KEY, PromptConstant.AGENT3_CONTENT_PROMPT,
                PromptConstant.AGENT3_CONTENT_VARIABLE_SCHEMA,
                Map.of(
                        "mainTitle", title.getMainTitle(),
                        "subTitle", title.getSubTitle(),
                        "outline", JSONUtil.toJsonPrettyStr(outline)
                ));
        long startMillis = System.currentTimeMillis();
        AiBillingReservation reservation = aiBillingService.reserveTextCall(userId, state.getTaskId(),
                "ContentGeneratorAgent", "CONTENT_GENERATING", "DASHSCOPE", DashScopeModel.ChatModel.QWEN3_MAX.value);
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

            state.setContent(response);
            recordPromptUsage(systemPrompt, userPrompt, state.getTaskId(), true, null, startMillis);
            log.info("阶段3：生成正文完毕, taskId={}", state.getTaskId());
        }
        catch (RuntimeException e) {
            if (!billed) {
                aiBillingService.releaseReservation(reservation, resolveLatency(startMillis), e.getMessage());
            }
            recordPromptUsage(systemPrompt, userPrompt, state.getTaskId(), false, e.getMessage(), startMillis);
            throw e;
        }
    }

    /**
     * 记录正文 Agent 本次调用使用的系统 Prompt 和用户 Prompt。
     */
    private void recordPromptUsage(PromptTemplateRenderResult systemPrompt, PromptTemplateRenderResult userPrompt,
            String taskId, boolean responseOk, String errorMessage, long startMillis) {
        Integer latencyMs = Math.toIntExact(Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - startMillis));
        promptUsageLogService.recordUsage(systemPrompt, "ContentGeneratorAgent", taskId, responseOk, errorMessage, latencyMs);
        promptUsageLogService.recordUsage(userPrompt, "ContentGeneratorAgent", taskId, responseOk, errorMessage, latencyMs);
    }

    private Integer resolveLatency(long startMillis) {
        return Math.toIntExact(Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - startMillis));
    }
}
