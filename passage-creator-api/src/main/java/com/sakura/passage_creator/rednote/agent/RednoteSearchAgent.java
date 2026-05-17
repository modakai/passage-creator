package com.sakura.passage_creator.rednote.agent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolretry.ToolRetryInterceptor;
import com.alibaba.cloud.ai.graph.internal.node.Node;
import com.sakura.passage_creator.rednote.agent.hook.RednoteSearchAgentHook;
import com.sakura.passage_creator.rednote.agent.tool.search.RednoteUrlFetchTools;
import com.sakura.passage_creator.rednote.agent.tool.search.RednoteWebSearchTools;
import com.sakura.passage_creator.rednote.constant.UniversalConstant;
import com.sakura.passage_creator.rednote.workflow.state.RednoteWorkflowState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * 小红书 SearchAgent，使用 ReActAgent 主动调用网页搜索工具并整理 RednoteBrief。
 */
@Slf4j
@Component
public class RednoteSearchAgent {

    /**
     * SearchAgent 的固定系统提示词；动态 Prompt 和模板版本管理后续再接入。
     */
    private static final String SYSTEM_PROMPT = """
            你是小红书爆款创作流程中的 SearchAgent。
            你的第一职责是必须调用 rednote_web_search 工具做网页搜索，然后把用户需求和搜索结果整理成 RednoteBrief。
            在调用 rednote_web_search 工具前，请先理解用户的语义。
            例如："我需要创作居家胸部健身的爆文，字数要求200字。要求：训练动作数量5个；分析每个动作；"
            就可以理解成：query：居家胸部健身动作等等。
            
            规则：
            1. 必须优先调用 rednote_web_search，搜索 query 直接来自用户 content，可适当补充“小红书 爆款 经验 攻略”等检索词。
            2. 搜索结果中如果存在高价值 sourceUrl，可以选择 1-3 个调用 rednote_url_fetch 抓取正文片段；不要抓取无关、重复或明显低质量 URL。
            3. rednote_url_fetch 的正文只用于素材清洗和摘要，不要把原文大段塞进最终输出。
            4. 你不是最终文案 Agent，不要输出完整小红书正文。
            5. subject 是核心主体、产品或场景。
            6. context 要整合用户需求、网页搜索摘要、URL 正文素材、受众痛点、可用卖点和创作角度。
            7. contentLength 只能是 SHORT、MEDIUM、LONG；无法判断时使用 MEDIUM。
            8. targetWordCount 按用户要求推断；无法判断时 SHORT=300、MEDIUM=600、LONG=1000。
            9. tagCount 未指定时使用 5。
            10. imageCount 是普通配图数量，不含封面；未指定时使用 3；最大不能超过 5。
            11. searchResults 每条只包含 title、summary、sourceName、sourceUrl；summary 应该是搜索摘要和 URL 正文抓取结果清洗后的创作素材摘要。
            12. 如果搜索工具或 URL 抓取工具不可用，也要基于用户 content 输出 RednoteBrief，并让 searchResults 为空或记录工具返回的错误摘要。
            13. 最终只返回 JSON，不要 Markdown，不要解释文字。
            
            json格式参考：
            ```json
            {
               "subject": "AI创作工具",
               "context": "结合用户需求与搜索结果，生成关于AI创作工具的专业内容",
               "contentLength": "MEDIUM",
               "targetWordCount": 800,
               "keywords": [
                 "AI",
                 "创作",
                 "工具"
               ],
               "tagCount": 5,
               "imageCount": 3,
               "searchResults": [
                 {
                   "title": "主流AI创作工具对比",
                   "summary": "当前市场主流AI创作工具涵盖文案、图像、视频等多类型，满足不同创作需求",
                   "sourceName": "科技资讯网",
                   "sourceUrl": "https://demo.com/ai-tool"
                 },
                 {
                   "title": "AI工具使用技巧",
                   "summary": "使用AI创作工具时，清晰的指令描述能大幅提升内容生成质量",
                   "sourceName": "技术博客",
                   "sourceUrl": "https://demo.com/ai-tips"
                 }
               ]
             }
            ```
            """;

    private final ReactAgent reactAgent;

    public RednoteSearchAgent(DashScopeApi dashScopeApi,
                              RednoteWebSearchTools rednoteWebSearchTools,
                              RednoteUrlFetchTools rednoteUrlFetchTools,
                              RednoteSearchAgentHook rednoteSearchAgentHook) {
        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .model(DashScopeModel.ChatModel.QWEN3_MAX.value)
                        .temperature(0.1)
                        .maxToken(3000)
                        .topP(0.9)
                        .build())
                .build();
        this.reactAgent = ReactAgent.builder()
                .name(UniversalConstant.SEARCH_AGENT_NAME)
                .description("搜索网页并整理小红书创作简报")
                .model(chatModel)
                .systemPrompt(SYSTEM_PROMPT)
                .instruction("用户需求：{content}")
                .outputType(RednoteWorkflowState.SearchResponse.class)
                .methodTools(rednoteWebSearchTools, rednoteUrlFetchTools)
                // 搜索工具属于外部 I/O，使用 Alibaba 内置 ToolRetryInterceptor 做短暂重试和错误消息回传。
                .interceptors(ToolRetryInterceptor.builder()
                        .toolName(UniversalConstant.SEARCH_TOOL_NAME)
                        .maxRetries(2)
                        .initialDelay(500)
                        .maxDelay(3000)
                        .onFailure(ToolRetryInterceptor.OnFailureBehavior.RETURN_MESSAGE)
                        .build())
                .hooks(rednoteSearchAgentHook)
                // 定义Agent的输出名称，可以在state中读取
                .outputKey(RednoteWorkflowState.SEARCH_OUTPUT_KEY)
                .enableLogging(true)
                .build();
    }

    public String name() {
        return reactAgent.name();
    }

    public Node asNode() {
        return reactAgent.asNode(true, false);
    }
}
