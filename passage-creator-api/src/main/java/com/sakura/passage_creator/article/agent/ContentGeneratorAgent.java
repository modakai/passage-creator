package com.sakura.passage_creator.article.agent;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.constant.PromptConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
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

    public ContentGeneratorAgent(DashScopeApi dashScopeApi) {
        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .model(DashScopeModel.ChatModel.QWEN3_MAX.value)
                        .temperature(0.2)
                        .maxToken(8000)
                        .topP(0.9)
                        .build())
                .build();

        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是一位资深的内容创作者,擅长撰写优质文章。
                        
                        根据用户提供的大纲、标题,创作文章正文，具体有：
                        主标题、副标题、大纲
                        
                        要求:
                        1. 内容要充实,每个章节300-400字
                        2. 语言流畅,富有感染力
                        3. 适当使用金句,增强可读性
                        4. 添加过渡句,确保逻辑连贯
                        5. 使用 Markdown 格式,章节使用 ## 标题
                        
                        请直接返回 Markdown 格式的正文内容,不要有其他内容。
                        """)
                .build();
    }

    /**
     * 生成文章正文并写回状态对象。
     *
     * @param state 文章生成状态，必须包含标题和大纲
     */
    public void generatorContent(ArticleState state) {
        log.info("阶段3：开始生成正文, taskId={}", state.getTaskId());
        ArticleState.TitleResult title = state.getTitle();
        ArticleState.OutlineResult outline = state.getOutline();

        // 大纲作为 JSON 放入提示词，避免章节和要点在字符串拼接时丢失结构。
        String response = chatClient.prompt()
                .user(u -> u.text(PromptConstant.AGENT3_CONTENT_PROMPT)
                        .params(Map.of(
                                "mainTitle", title.getMainTitle(),
                                "subTitle", title.getSubTitle(),
                                "outline", JSONUtil.toJsonPrettyStr(outline)
                        ))
                )
                .call()
                .content();

        state.setContent(response);
        log.info("阶段3：生成正文完毕, taskId={}", state.getTaskId());
    }
}
