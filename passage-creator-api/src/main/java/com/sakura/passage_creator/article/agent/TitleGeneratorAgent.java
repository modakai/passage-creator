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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
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

    private final ChatClient chatClient;

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

        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是一位爆款文章标题专家,擅长创作吸引人的标题。
                        
                        能根据用户提供的选题方向,生成 3-5 个爆款文章标题方案:
                        
                        要求:
                        1. 每个方案包含主标题和副标题
                        2. 主标题要包含数字、情绪化词汇,吸引眼球
                        3. 副标题要补充说明,增强吸引力
                        4. 标题要简洁有力,不超过30字
                        5. 不同方案要有不同的切入角度
                        6. 符合新媒体爆款文章的风格
                        
                        请直接返回 JSON 格式,不要有其他内容:
                        [
                          {
                            "mainTitle": "主标题1",
                            "subTitle": "副标题1"
                          },
                          {
                            "mainTitle": "主标题2",
                            "subTitle": "副标题2"
                          },
                          {
                            "mainTitle": "主标题3",
                            "subTitle": "副标题3"
                          }
                        ]
                        """)
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

//        String promptText = PromptConstant.AGENT1_TITLE_PROMPT.replace("{topic}", topic);
//        Prompt prompt = new Prompt(promptText);

//        ChatResponse chatResponse = chatClient.call(prompt);

//        // 转换 todo code可能返回错误，需要校验
//        String response = chatResponse.getResult().getOutput().getText();

        String response = chatClient.prompt()
                .user(u -> u.text(PromptConstant.AGENT1_TITLE_PROMPT)
                        .param("topic", topic))
                .call()
                .content();
        List<ArticleState.TitleOption> optionList = JSONUtil.toBean(response,
                new TypeReference<List<ArticleState.TitleOption>>() {
                }, true);

        state.setTitleOptions(optionList);
        log.info("阶段1：生成标题方案完毕，taskId={}", state.getTaskId());
    }

}
