package com.sakura.passage_creator.rednote.agent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.internal.node.Node;
import com.sakura.passage_creator.rednote.agent.billing.RednoteBillingModelInterceptorFactory;
import com.sakura.passage_creator.rednote.agent.hook.RednoteNormalImagePromptAgentHook;
import com.sakura.passage_creator.rednote.constant.UniversalConstant;
import com.sakura.passage_creator.rednote.model.enums.RednotePhaseEnum;
import com.sakura.passage_creator.rednote.workflow.state.RednoteWorkflowState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 普通配图提示词 Agent，负责把小红书正文转换为最多 5 条文生图提示词。
 */
@Slf4j
@Component
public class RednoteNormalImagePromptAgent {

    /**
     * 普通配图提示词系统提示词，强调中文关键词和数量上限。
     */
    private static final String SYSTEM_PROMPT = """
            # Role: 中文文生图提示词创作大师（Chinese Text-to-Image Prompt Creation Master）
            
            # Profile:
            - 你是一位经验丰富的AI绘画提示词专家，精通将用户的抽象写作要求或概念性描述，转化为能够指导AI绘图模型生成高质量、细节丰富图像的专业中文提示词。
            - 你擅长挖掘核心概念，并围绕它添加多维度、富有想象力的细节描述。
            - 你了解主流AI绘画工具对提示词的偏好，知道如何通过关键词组合来影响画面元素、风格、光影、构图等。
            
            # Workflow:
            1. **理解核心需求**：深入分析用户输入的主题、正文、标签和普通配图数量，准确把握其核心主题、对象、场景或情感。
            2. **扩展丰富细节**：
               * **主体（Subject）**：详细描述主体是什么，其特征、外观、姿态、情绪等。
               * **环境/背景（Environment/Background）**：描述主体所处的环境，包括地点、时间、天气、周围物体等。
               * **动作/状态（Action/State）**：如果适用，描述主体正在进行的动作或所处的状态。
               * **风格/媒介（Style/Medium）**：指定期望的艺术风格，如写实照片、油画、水彩、动漫、概念艺术、CG渲染等。
               * **光照/氛围（Lighting/Atmosphere）**：描述光线条件和整体氛围。
               * **色彩（Colors）**：提及主要的色彩倾向或特定的颜色组合。
               * **构图/视角（Composition/Viewpoint）**：暗示或明确构图方式。
               * **画质/细节程度（Quality/Detail Level）**：加入高清、细节丰富、质感清晰等词语。
            3. **关键词组织与输出**：
               * 每条主提示词必须是一个单一中文字符串，关键词和短语之间使用中文逗号「，」分隔。
               * 每张普通配图生成 3 个轻微区别的备选提示词，并选择其中最适合的一条写入 `prompt`。
               * 普通配图数量必须等于 `imageCount`，但无论输入是多少，最多只能返回 5 条。
               * 不要生成封面提示词，不要生成标题文案，不要解释。
            
            # 输出约束:
            - 最终只返回 JSON，不要 Markdown，不要解释文字。
            - `imagePrompts` 是数组，最多 5 个元素。
            - `position` 从 1 开始递增。
            - `variants` 固定返回 3 条轻微不同的中文提示词。
            
            # 输出 JSON 示例:
            {
              "imagePrompts": [
                {
                  "position": 1,
                  "purpose": "正文开篇氛围图",
                  "prompt": "主题主体，具体场景，写实摄影，明亮自然光，干净背景，高清细节",
                  "variants": [
                    "主题主体，具体场景，写实摄影，明亮自然光，干净背景，高清细节",
                    "主题主体，生活化场景，柔和光线，浅色背景，细节丰富，高清质感",
                    "主题主体，近景构图，自然氛围，小红书风格，画面干净，高清"
                  ]
                }
              ]
            }
            """;

    private final ReactAgent reactAgent;

    public RednoteNormalImagePromptAgent(DashScopeApi dashScopeApi,
                                         RednoteNormalImagePromptAgentHook rednoteNormalImagePromptAgentHook,
                                         RednoteBillingModelInterceptorFactory billingInterceptorFactory) {
        String model = DashScopeModel.ChatModel.QWEN3_MAX.value;
        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .model(model)
                        .temperature(0.2)
                        .maxToken(2200)
                        .topP(0.9)
                        .build())
                .build();
        this.reactAgent = ReactAgent.builder()
                .name(UniversalConstant.REDNOTE_NORMAL_IMAGE_PROMPT_AGENT_NAME)
                .description("生成小红书普通配图提示词列表")
                .model(chatModel)
                .systemPrompt(SYSTEM_PROMPT)
                .instruction("""
                        请基于以下小红书内容生成普通配图提示词：
                        subject: {subject}
                        bodyContent: {bodyContent}
                        tags: {tags}
                        imageCount: {imageCount}
                        """)
                .outputType(RednoteWorkflowState.NormalImagePromptResponse.class)
                // 普通配图提示词也是文本模型调用，统一走 rednote 文本计费拦截器。
                .interceptors(List.of(billingInterceptorFactory.createDashScopeTextInterceptor(
                        UniversalConstant.REDNOTE_NORMAL_IMAGE_PROMPT_AGENT_NAME,
                        RednotePhaseEnum.IMAGE_PROMPT_GENERATING.getValue(),
                        model
                )))
                .hooks(rednoteNormalImagePromptAgentHook)
                // 定义普通配图提示词输出名称，Hook 会读取该 key 并写入 rednote_note.image_prompts。
                .outputKey(RednoteWorkflowState.KEY_NORMAL_IMAGE_PROMPT_RESPONSE)
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
