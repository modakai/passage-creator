package com.sakura.passage_creator.article.agent;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.constant.PromptConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;

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

    public TitleGeneratorAgent(DashScopeApi dashScopeApi) {
        this.chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .model(DashScopeModel.ChatModel.QWEN3_MAX.value)
                        .temperature(0.1)
                        .maxToken(2000)
                        .topP(0.9)
                        .build())
                .build();
    }

    /**
     * 生成标题
     *
     * @param state 状态
     */
    public void generatorTitle(ArticleState state) {
        log.info("阶段1：开始生成标题方案, taskId={}", state.getTaskId());
        String topic = state.getTopic();
       
        String promptText = PromptConstant.AGENT1_TITLE_PROMPT.replace("{topic}", topic);
        Prompt prompt = new Prompt(promptText);

        ChatResponse chatResponse = chatModel.call(prompt);
        // 转换 todo code可能返回错误，需要校验
        String response = chatResponse.getResult().getOutput().getText();
        List<ArticleState.TitleOption> optionList = JSONUtil.toBean(response, new TypeReference<List<ArticleState.TitleOption>>() {
        }, true);

        state.setTitleOptions(optionList);
        log.info("阶段1：生成标题方案完毕，taskId={}", state.getTaskId());
    }

}
