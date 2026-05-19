package com.sakura.passage_creator.rednote.agent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.internal.node.Node;
import com.sakura.passage_creator.rednote.agent.billing.RednoteBillingModelInterceptorFactory;
import com.sakura.passage_creator.rednote.agent.hook.RednoteCoverImagePromptAgentHook;
import com.sakura.passage_creator.rednote.constant.UniversalConstant;
import com.sakura.passage_creator.rednote.model.enums.RednotePhaseEnum;
import com.sakura.passage_creator.rednote.workflow.state.RednoteWorkflowState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 封面图提示词 Agent，负责生成封面短文案和最终封面图片提示词。
 */
@Slf4j
@Component
public class RednoteCoverImagePromptAgent {

    /**
     * 封面图文案和提示词系统提示词，基于用户给定模板改造成结构化 JSON 输出。
     */
    private static final String SYSTEM_PROMPT = """
            # Role：封面图文案提取与优化专家
            
            # Profile:
            - 你是一位经验丰富的封面图文案专家，擅长从给定的内容创作主题和详细文案中，精准提取并优化用于绘制封面图的关键词文字信息。
            - 你深谙如何用最凝练的文字抓住眼球，并确保所有文案元素都紧密围绕核心内容，信息量足且不空洞。
            
            # Workflow:
            1. **理解与分析**：仔细阅读并理解用户提供的“内容创作主题”`subject` 和“小红书正文”`bodyContent`。你的目标是从中提炼出最能代表内容精华的词汇和短句。
            2. **生成封面图主标题（title）**：
               * 目的：准确、醒目地揭示核心写作主题 `subject`。
               * 要求：严格不超过12个字。必须基于 `subject` 和 `bodyContent` 的核心内容。
            3. **生成封面图副标题（subtitle）**：
               * 目的：对主标题进行补充说明，增加吸引力或点明价值。
               * 要求：10-20个字。从 `bodyContent` 中提炼关键信息或亮点。
            4. **生成点缀文案（decorativeText）**：
               * 目的：增加封面图的趣味性、悬念感或引导性，使其更具设计感和吸引力。
               * 要求：10-20个字。从 `bodyContent` 中提炼有趣短句、强烈观点或行动号召。
            5. **生成相关标签（tags）**：
               * 目的：概括内容亮点，便于用户快速了解主题，也可用于设计元素。
               * 要求：4-5个标签。格式为 `#标签1`，从 `bodyContent`、`subject` 和已有 `tags` 中提取核心关键词生成。
            6. **生成封面图片提示词（coverPrompt）**：
               * 目的：给图片生成模型直接使用，必须包含主标题、副标题、点缀文案、标签、画面主体、构图、色彩、光影和风格。
               * 要求：使用中文逗号「，」分隔关键词或短语，适合小红书竖版封面，文字排版醒目但不要拥挤。
            7. **核心要求**：
               * 你生成的所有文案都必须尽可能利用 `bodyContent` 中的关键词和积极观点，避免空洞无物。
            
            # 输出约束:
            - 最终只返回 JSON，不要 Markdown，不要解释文字。
            - 字段名固定为 `title`、`subtitle`、`decorativeText`、`tags`、`coverPrompt`。
            
            # 输出 JSON 示例:
            {
              "title": "封面主标题",
              "subtitle": "补充核心价值的副标题",
              "decorativeText": "制造兴趣的点缀文案",
              "tags": ["#标签1", "#标签2", "#标签3", "#标签4", "#标签5"],
              "coverPrompt": "小红书封面图，主标题：封面主标题，副标题：补充核心价值的副标题，点缀文案：制造兴趣的点缀文案，竖版构图，醒目中文排版，明亮自然光，高清细节"
            }
            """;

    private final ReactAgent reactAgent;

    public RednoteCoverImagePromptAgent(DashScopeApi dashScopeApi,
                                        RednoteCoverImagePromptAgentHook rednoteCoverImagePromptAgentHook,
                                        RednoteBillingModelInterceptorFactory billingInterceptorFactory) {
        String model = DashScopeModel.ChatModel.QWEN3_MAX.value;
        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .model(model)
                        .temperature(0.2)
                        .maxToken(1800)
                        .topP(0.9)
                        .build())
                .build();
        this.reactAgent = ReactAgent.builder()
                .name(UniversalConstant.REDNOTE_COVER_IMAGE_PROMPT_AGENT_NAME)
                .description("生成小红书封面文案和封面图片提示词")
                .model(chatModel)
                .systemPrompt(SYSTEM_PROMPT)
                .instruction("""
                        请基于以下小红书内容生成封面图文案和封面图片提示词：
                        subject: {subject}
                        bodyContent: {bodyContent}
                        tags: {tags}
                        """)
                .outputType(RednoteWorkflowState.CoverImagePromptResponse.class)
                // 封面提示词文本调用独立计费，便于后台按 Agent 阶段追踪成本。
                .interceptors(List.of(billingInterceptorFactory.createDashScopeTextInterceptor(
                        UniversalConstant.REDNOTE_COVER_IMAGE_PROMPT_AGENT_NAME,
                        RednotePhaseEnum.IMAGE_PROMPT_GENERATING.getValue(),
                        model
                )))
                .hooks(rednoteCoverImagePromptAgentHook)
                // 定义封面提示词输出名称，Hook 会读取该 key 并写入 rednote_note.cover_title 和 cover_prompt。
                .outputKey(RednoteWorkflowState.KEY_COVER_IMAGE_PROMPT_RESPONSE)
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
