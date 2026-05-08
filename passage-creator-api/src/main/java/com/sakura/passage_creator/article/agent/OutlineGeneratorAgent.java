package com.sakura.passage_creator.article.agent;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeResponseFormat;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.constant.PromptConstant;
import com.sakura.passage_creator.prompt.service.PromptTemplateRenderResult;
import com.sakura.passage_creator.prompt.service.PromptTemplateService;
import com.sakura.passage_creator.prompt.service.PromptUsageLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 大纲生成Agent
 *
 * @author sakura
 * @create 2026-04
 */
@Component
@Slf4j
public class OutlineGeneratorAgent {

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
     * Spring AI 结构化输出转换器，用于生成 JSON Schema 提示并解析模型输出。
     */
    private final BeanOutputConverter<ArticleState.OutlineResult> outlineOutputConverter =
            new BeanOutputConverter<>(ArticleState.OutlineResult.class);

    public OutlineGeneratorAgent(DashScopeApi dashScopeApi, PromptTemplateService promptTemplateService,
            PromptUsageLogService promptUsageLogService) {
        this.promptTemplateService = promptTemplateService;
        this.promptUsageLogService = promptUsageLogService;
        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .model(DashScopeModel.ChatModel.QWEN3_MAX.value)
                        .temperature(0.1)
                        .maxToken(2000)
                        .topP(0.9)
                        // 让 DashScope 在模型服务端启用 JSON 对象输出，降低漏引号、夹杂解释文本的概率。
                        .responseFormat(DashScopeResponseFormat.builder()
                                .type(DashScopeResponseFormat.Type.JSON_OBJECT)
                                .build())
                        .build())
                .build();

        this.chatClient = ChatClient.builder(chatModel).build();
    }

    /**
     * 生成大纲
     *
     * @param state 状态
     */
    public void generatorOutline(ArticleState state) {
        log.info("阶段2：开始生成大纲, taskId={}", state.getTaskId());
        ArticleState.TitleResult title = state.getTitle();
        String mainTitle = title.getMainTitle();
        String subTitle = title.getSubTitle();
        String userDescription = StrUtil.isBlank(state.getUserDescription()) ? "无" : state.getUserDescription();

        PromptTemplateRenderResult systemPrompt = promptTemplateService.renderActive(
                PromptConstant.AGENT2_OUTLINE_SYSTEM_KEY, PromptConstant.AGENT2_OUTLINE_SYSTEM_PROMPT, null, Map.of());
        PromptTemplateRenderResult userPrompt = promptTemplateService.renderActive(
                PromptConstant.AGENT2_OUTLINE_USER_KEY, PromptConstant.AGENT2_OUTLINE_PROMPT + "\n{format}",
                PromptConstant.AGENT2_OUTLINE_VARIABLE_SCHEMA,
                Map.of(
                        "mainTitle", mainTitle,
                        "subTitle", subTitle,
                        "descriptionSection", userDescription,
                        "format", outlineOutputConverter.getFormat()
                ));
        long startMillis = System.currentTimeMillis();
        try {
            String response = chatClient.prompt()
                    .system(s -> s.text(systemPrompt.content()))
                    .user(u -> u.text(userPrompt.content()))
                    .call()
                    .content();
            log.info("阶段2：生成大纲内容：{}", response);
            ArticleState.OutlineResult optionList = parseStructuredOutline(response);

            state.setOutline(optionList);
            recordPromptUsage(systemPrompt, userPrompt, state.getTaskId(), true, null, startMillis);
            log.info("阶段2：生成大纲完毕，taskId={}", state.getTaskId());
        }
        catch (RuntimeException e) {
            recordPromptUsage(systemPrompt, userPrompt, state.getTaskId(), false, e.getMessage(), startMillis);
            throw e;
        }
    }

    /**
     * 优先使用结构化输出转换器解析；模型仍违规时再走本地兜底修复。
     */
    private ArticleState.OutlineResult parseStructuredOutline(String response) {
        try {
            ArticleState.OutlineResult outline = outlineOutputConverter.convert(response);
            OutlineJsonParser.validateOutline(outline);
            return outline;
        }
        catch (RuntimeException e) {
            log.warn("阶段2：结构化输出解析失败，尝试使用本地 JSON 修复兜底", e);
            return OutlineJsonParser.parse(response);
        }
    }

    /**
     * 记录大纲 Agent 本次调用使用的系统 Prompt 和用户 Prompt。
     */
    private void recordPromptUsage(PromptTemplateRenderResult systemPrompt, PromptTemplateRenderResult userPrompt,
            String taskId, boolean responseOk, String errorMessage, long startMillis) {
        Integer latencyMs = Math.toIntExact(Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - startMillis));
        promptUsageLogService.recordUsage(systemPrompt, "OutlineGeneratorAgent", taskId, responseOk, errorMessage, latencyMs);
        promptUsageLogService.recordUsage(userPrompt, "OutlineGeneratorAgent", taskId, responseOk, errorMessage, latencyMs);
    }
}
