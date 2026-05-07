package com.sakura.passage_creator.article.agent;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeResponseFormat;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.constant.PromptConstant;
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
     * Spring AI 结构化输出转换器，用于生成 JSON Schema 提示并解析模型输出。
     */
    private final BeanOutputConverter<ArticleState.OutlineResult> outlineOutputConverter =
            new BeanOutputConverter<>(ArticleState.OutlineResult.class);

    public OutlineGeneratorAgent(DashScopeApi dashScopeApi) {
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

        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是一位专业的文章策划师,擅长设计文章结构。
                        
                        根据提供的主标题、副标题和补充描述[可选，用户提供就用，没提供就不管]，生成文章的大纲
                        
                        要求:
                        1. 大纲要有清晰的逻辑结构
                        2. 包含开头引入、核心观点(3-5个)、结尾升华
                        3. 每个章节要有明确的标题和核心要点(2-3个)
                        4. 适合2000字左右，但不要超过3000字的文章
                        5. 所有 JSON 字符串值必须使用英文双引号包裹，不能省略引号
                        
                        请直接返回 JSON 格式,不要有其他内容:
                        {
                          "sections": [
                            {
                              "section": 1,
                              "title": "章节标题",
                              "points": ["要点1", "要点2"]
                            }
                          ]
                        }
                        """)
                .build();
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

        String response = chatClient.prompt()
                .user(u -> u.text(PromptConstant.AGENT2_OUTLINE_PROMPT + "\n{format}")
                        .params(Map.of(
                                "mainTitle", mainTitle,
                                "subTitle", subTitle,
                                "descriptionSection", userDescription,
                                "format", outlineOutputConverter.getFormat()
                        ))
                )
                .call()
                .content();
        log.info("阶段2：生成大纲内容：{}", response);
        ArticleState.OutlineResult optionList = parseStructuredOutline(response);

        state.setOutline(optionList);
        log.info("阶段2：生成大纲完毕，taskId={}", state.getTaskId());
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

}
