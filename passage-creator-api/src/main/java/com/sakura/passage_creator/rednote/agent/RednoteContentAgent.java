package com.sakura.passage_creator.rednote.agent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.internal.node.Node;
import com.sakura.passage_creator.prompt.api.PromptTemplateRenderResult;
import com.sakura.passage_creator.prompt.api.PromptTemplateService;
import com.sakura.passage_creator.prompt.api.PromptUsageLogService;
import com.sakura.passage_creator.rednote.agent.billing.RednoteBillingModelInterceptorFactory;
import com.sakura.passage_creator.rednote.agent.billing.RednotePromptUsageModelInterceptor;
import com.sakura.passage_creator.rednote.agent.hook.RednoteContentAgentHook;
import com.sakura.passage_creator.rednote.constant.RednotePromptConstant;
import com.sakura.passage_creator.rednote.constant.UniversalConstant;
import com.sakura.passage_creator.rednote.model.enums.RednotePhaseEnum;
import com.sakura.passage_creator.rednote.workflow.state.RednoteWorkflowState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 小红书文案 Agent，基于 RednoteBrief 生成结构化正文和标签。
 */
@Slf4j
@Component
public class RednoteContentAgent {

    /**
     * ContentAgent 的默认系统提示词，运行时未配置 ACTIVE 模板时作为兜底内容。
     */
    public static final String SYSTEM_PROMPT = """
            # Role：小红书爆款内容创作者
            
            # Profile:
            - 你是一位深耕小红书平台的资深内容创作者，精通小红书用户的阅读偏好和爆款笔记的创作秘诀。
            - 你擅长使用生动、口语化的语言，结合吸睛的emoji表情和热门标签，打造高互动率的种草内容。
            - 你了解如何通过“二极管标题法”（或类似的强对比、反差、悬念、利益点突出等技巧）吸引用户点击。
            - 你的职责只到文案生成：不要调用搜索工具，不要生成图片提示词，不要生成封面标题字段。
            
            # Workflow:
            1. **接收创作背景**：你将收到写作主题 `subject`、目标字数 `targetWordCount`、相关背景信息 `context`、爆款关键词 `keywords`、标签数量 `tagCount` 和搜索摘要 `searchResults`。
            2. **标题创作（5个）**：
               * **核心要求**：强吸引力，引发好奇，突出亮点/痛点/价值。
               * **技巧**：运用“二极管标题法”或类似技巧（如：数字、提问、反差、稀缺性、实用性、情绪共鸣）。
               * **关键词**：优先从 `keywords` 中挑选1-2个融入标题；若关键词不足，则自行使用与主题相关热词。
               * **字数**：严格控制在20字以内。
               * **Emoji**：每个标题必须包含1-2个与主题和情绪贴合的emoji。
            3. **正文创作（1篇）**：
               * **风格**：小红书风格--口语化、接地气、真诚分享、避免官方腔调。句子力求简短、易读。
               * **开篇**：黄金三秒原则，开头直奔主题或设置悬念，迅速抓住用户。
               * **结构**：段落清晰，逻辑连贯。可采用痛点分析+解决方案、经验分享、好物推荐、教程步骤等结构。
               * **内容**：围绕 `subject` 展开，从 `context` 和 `searchResults` 中筛选对用户最有价值、最能引发共鸣的信息进行阐述。内容要真实、具体、有细节。
               * **Emoji**：每段开头、结尾及关键信息点穿插emoji，增强表现力和阅读趣味性。
               * **互动引导**：在文末或段落间巧妙引导用户进行点赞、收藏、评论、关注等互动。
               * **爆款词/网络热梗**：适当融入与主题相关的爆款词或网络热梗，增加趣味性和传播力。
               * **字数**：尽量贴近 `targetWordCount`，允许上下浮动 15%。
            4. **标签生成**：
               * 从生成的正文中提炼3-6个核心SEO关键词。
               * 基于这些关键词，生成 `tagCount` 个标签（若未指定，则默认为5个）。
               * 标签格式为 `#标签名`。
            
            # 输出约束:
            - 最终只返回 JSON，不要 Markdown，不要解释文字。
            - JSON 字段名使用 `bodyContent`，它会被系统保存到 `rednote_note.body_content`。
            - `bodyContent` 保存“小红书内容”，包含 5 个备选标题和 1 篇正文，但不要包含标签。
            - `tags` 保存标签数组，数量尽量等于 `tagCount`，每个标签必须以 `#` 开头。
            
            # 输出 JSON 示例:
            {
              "bodyContent": "## 备选标题\\n1. ✨ 标题文字\\n2. 🔥 标题文字\\n3. 💡 标题文字\\n4. ✅ 标题文字\\n5. 📌 标题文字\\n\\n## 正文\\n✨ 段落内容...\\n\\n📌 段落内容...\\n\\n姐妹们，觉得有用记得点赞收藏，评论区告诉我你还想看什么！",
              "tags": ["#标签1", "#标签2", "#标签3", "#标签4", "#标签5"]
            }
            """;

    private final DashScopeApi dashScopeApi;

    private final RednoteContentAgentHook rednoteContentAgentHook;

    private final RednoteBillingModelInterceptorFactory billingInterceptorFactory;

    private final PromptTemplateService promptTemplateService;

    private final PromptUsageLogService promptUsageLogService;

    public RednoteContentAgent(DashScopeApi dashScopeApi,
                               RednoteContentAgentHook rednoteContentAgentHook,
                               RednoteBillingModelInterceptorFactory billingInterceptorFactory,
                               PromptTemplateService promptTemplateService,
                               PromptUsageLogService promptUsageLogService) {
        this.dashScopeApi = dashScopeApi;
        this.rednoteContentAgentHook = rednoteContentAgentHook;
        this.billingInterceptorFactory = billingInterceptorFactory;
        this.promptTemplateService = promptTemplateService;
        this.promptUsageLogService = promptUsageLogService;
    }

    private ReactAgent buildAgent() {
        String model = DashScopeModel.ChatModel.QWEN3_MAX.value;
        PromptTemplateRenderResult systemPrompt = promptTemplateService.resolveActiveRaw(
                RednotePromptConstant.CONTENT_SYSTEM_KEY, SYSTEM_PROMPT);
        PromptTemplateRenderResult userPrompt = promptTemplateService.resolveActiveRaw(
                RednotePromptConstant.CONTENT_USER_KEY, RednotePromptConstant.CONTENT_USER_PROMPT);
        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .model(model)
                        .temperature(0.1)
                        .maxToken(3000)
                        .topP(0.9)
                        .build())
                .build();
        return ReactAgent.builder()
                .name(UniversalConstant.REDNOTE_CONTENT_AGENT_NAME)
                .description("根据 RednoteBrief 生成小红书正文和标签")
                .model(chatModel)
                .systemPrompt(systemPrompt.content())
                .instruction(userPrompt.content())
                .outputType(RednoteWorkflowState.ContentResponse.class)
                // 文案生成按真实 token 用量结算，失败时释放预扣积分。
                .interceptors(List.of(
                        billingInterceptorFactory.createDashScopeTextInterceptor(
                                UniversalConstant.REDNOTE_CONTENT_AGENT_NAME,
                                RednotePhaseEnum.COPY_GENERATING.getValue(),
                                model
                        ),
                        new RednotePromptUsageModelInterceptor(promptUsageLogService,
                                UniversalConstant.REDNOTE_CONTENT_AGENT_NAME, List.of(systemPrompt, userPrompt))
                ))
                .hooks(rednoteContentAgentHook)
                // 定义 ContentAgent 的输出名称，Hook 会读取该 key 并写入 rednote_note。
                .outputKey(RednoteWorkflowState.KEY_COPYWRITING)
                .enableLogging(true)
                .build();
    }

    public String name() {
        return UniversalConstant.REDNOTE_CONTENT_AGENT_NAME;
    }

    public Node asNode() {
        return buildAgent().asNode(true, false);
    }
}
