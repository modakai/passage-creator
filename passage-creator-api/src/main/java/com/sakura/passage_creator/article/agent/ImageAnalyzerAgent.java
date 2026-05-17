package com.sakura.passage_creator.article.agent;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeResponseFormat;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.config.OpenAiImageProperties;
import com.sakura.passage_creator.article.image.service.ImageRequirementPolicy;
import com.sakura.passage_creator.article.model.enums.ImageMethodEnum;
import com.sakura.passage_creator.billing.api.AiBillingReservation;
import com.sakura.passage_creator.billing.api.AiBillingService;
import com.sakura.passage_creator.billing.api.AiChatBillingSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 配图需求分析 Agent，负责在正文中插入占位符并生成多来源配图需求。
 */
@Component
@Slf4j
public class ImageAnalyzerAgent {

    /**
     * 结构化输出转换器，用于约束模型返回 Agent4Result JSON。
     */
    private final BeanOutputConverter<ArticleState.Agent4Result> outputConverter =
            new BeanOutputConverter<>(ArticleState.Agent4Result.class);

    /**
     * 聊天客户端，用于分析正文中的配图点位。
     */
    private final ChatClient chatClient;

    /**
     * AI 计费服务，用于记录配图分析阶段的 Token 成本。
     */
    private final AiBillingService aiBillingService;

    /**
     * 除封面外最多生成的章节配图数量。
     */
    private final int maxSectionImages;

    public ImageAnalyzerAgent(DashScopeApi dashScopeApi, OpenAiImageProperties imageProperties,
                              AiBillingService aiBillingService) {
        this.aiBillingService = aiBillingService;
        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .model(DashScopeModel.ChatModel.QWEN3_MAX.value)
                        .temperature(0.1)
                        .maxToken(3000)
                        .topP(0.9)
                        // 配图需求需要稳定 JSON，避免夹杂解释文本导致后续无法解析。
                        .responseFormat(DashScopeResponseFormat.builder()
                                .type(DashScopeResponseFormat.Type.JSON_OBJECT)
                                .build())
                        .build())
                .build();
        this.chatClient = ChatClient.builder(chatModel).build();
        this.maxSectionImages = imageProperties.getMaxSectionImages();
    }

    /**
     * 分析正文配图需求并写回文章状态。
     *
     * @param state 文章生成状态
     */
    public void analyze(ArticleState state, Long userId) {
        log.info("阶段4：开始分析配图需求, taskId={}", state.getTaskId());
        String mainTitle = state.getTitle().getMainTitle();
        String content = state.getContent();
        String prompt = buildPrompt(mainTitle, content, state.getEnabledImageMethods());

        long startMillis = System.currentTimeMillis();
        AiBillingReservation reservation = aiBillingService.reserveTextCall(userId, state.getTaskId(),
                "ImageAnalyzerAgent", "IMAGE_ANALYZING", "DASHSCOPE", DashScopeModel.ChatModel.QWEN3_MAX.value);
        boolean billed = false;
        String response;
        try {
            ChatResponse chatResponse = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .chatResponse();
            response = AiChatBillingSupport.contentOf(chatResponse);
            aiBillingService.completeTextCall(reservation, AiChatBillingSupport.usageOf(chatResponse),
                    resolveLatency(startMillis), true, null);
            billed = true;
        } catch (RuntimeException e) {
            if (!billed) {
                aiBillingService.releaseReservation(reservation, resolveLatency(startMillis), e.getMessage());
            }
            throw e;
        }

        ArticleState.Agent4Result result = parseResult(response);
        List<ArticleState.ImageRequirement> imageRequirements =
                new ImageRequirementPolicy(maxSectionImages, state.getEnabledImageMethods())
                        .apply(result.getImageRequirements());

        state.setContent(StrUtil.blankToDefault(result.getContentWithPlaceholders(), content));
        state.setImageRequirements(imageRequirements);
        log.info("阶段4：配图需求分析完成, taskId={}, count={}", state.getTaskId(), imageRequirements.size());
    }

    private Integer resolveLatency(long startMillis) {
        return Math.toIntExact(Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - startMillis));
    }

    /**
     * 构建配图分析提示词，要求模型在后端已支持的来源中选择最适合的配图方式。
     */
    private String buildPrompt(String mainTitle, String content, List<String> enabledImageMethods) {
        return """
                你是一位专业的新媒体编辑，正在为文章生成配图需求。
                
                主标题：%s
                
                正文：
                %s
                
                可用配图方式：
                %s
                
                配图方式填写要求：
                %s
                
                任务要求：
                1. 生成 1 张封面图，position 必须为 1，type 必须为 cover，placeholderId 留空。
                2. 最多再生成 2 张章节配图，position 从 2 开始，type 使用 section。
                3. 章节配图必须在 contentWithPlaceholders 中插入 {{IMAGE_PLACEHOLDER_N}} 占位符，且 placeholderId 必须完全一致。
                4. imageSource 必须从可用配图方式中选择，禁止输出 NANO_BANANA、PICSUM 或其他未列出的值。
                5. GPT_IMAGE 的 prompt 必须使用英文，适合直接交给 gpt-image-2 生成，避免水印、避免侵权角色、避免过多文字。
                6. contentWithPlaceholders 必须保留原正文内容，只添加图片占位符。
                
                输出格式要求：
                """
                .formatted(mainTitle, content,
                        buildAvailableMethodsDescription(enabledImageMethods),
                        buildMethodUsageGuide(enabledImageMethods))
                + outputConverter.getFormat();
    }

    /**
     * 返回当前阶段可供模型选择的配图方式说明，PICSUM 只做系统降级不暴露给模型。
     */
    private String buildAvailableMethodsDescription(List<String> enabledImageMethods) {
        return resolveAllowedMethods(enabledImageMethods).stream()
                .map(this::describeMethod)
                .toList()
                .stream()
                .reduce((left, right) -> left + "\n" + right)
                .orElse(describeMethod(ImageMethodEnum.getDefaultAiMethod()));
    }

    /**
     * 返回不同配图方式的字段填写规则，减少模型把 prompt/keywords 填反。
     */
    private String buildMethodUsageGuide(List<String> enabledImageMethods) {
        return resolveAllowedMethods(enabledImageMethods).stream()
                .map(this::describeUsage)
                .toList()
                .stream()
                .reduce((left, right) -> left + "\n" + right)
                .orElse(describeUsage(ImageMethodEnum.getDefaultAiMethod()));
    }

    /**
     * 解析用户允许的方式，空值默认开放所有非降级方式。
     */
    private List<ImageMethodEnum> resolveAllowedMethods(List<String> enabledImageMethods) {
        if (enabledImageMethods == null || enabledImageMethods.isEmpty()) {
            return ImageMethodEnum.userSelectableMethods();
        }
        List<ImageMethodEnum> methods = enabledImageMethods.stream()
                .map(ImageMethodEnum::getByValue)
                .filter(method -> method != null && !method.isFallback())
                .distinct()
                .toList();
        return methods.isEmpty() ? List.of(ImageMethodEnum.getDefaultAiMethod()) : methods;
    }

    /**
     * 生成单个配图方式的描述。
     */
    private String describeMethod(ImageMethodEnum method) {
        return switch (method) {
            case PEXELS -> "- PEXELS: 图库检索，适合真实场景、人物、办公、生活、产品氛围图。";
            case MERMAID -> "- MERMAID: 流程图，适合步骤、流程、系统架构、因果关系，prompt 必须填写 Mermaid 代码。";
            case ICONIFY -> "- ICONIFY: 图标，适合工具列表、概念标签、轻量点缀，keywords 填英文关键词。";
            case SVG_DIAGRAM -> "- SVG_DIAGRAM: SVG 概念图，适合抽象概念、对比关系、知识结构图，prompt 填中文或英文需求。";
            case GPT_IMAGE -> "- GPT_IMAGE: 原创 AI 生图，适合封面、复杂概念、没有现成素材的视觉表达。";
            case PICSUM -> "";
        };
    }

    /**
     * 生成单个配图方式的字段填写规则。
     */
    private String describeUsage(ImageMethodEnum method) {
        return switch (method) {
            case PEXELS -> "- PEXELS: keywords 必填，使用 2-5 个英文检索词；prompt 可留空。";
            case MERMAID -> "- MERMAID: prompt 必填，只填写 Mermaid 代码，例如 flowchart TD；keywords 填流程主题。";
            case ICONIFY -> "- ICONIFY: keywords 必填，使用英文名词，例如 productivity、database、calendar；prompt 可留空。";
            case SVG_DIAGRAM -> "- SVG_DIAGRAM: prompt 必填，描述概念图结构、节点、关系和视觉风格；keywords 可填主题词。";
            case GPT_IMAGE ->
                    "- GPT_IMAGE: prompt 必填，使用详细英文提示词，描述画面主体、风格、构图、色彩和用途；keywords 可留空。";
            case PICSUM -> "";
        };
    }

    /**
     * 解析模型返回，结构化转换失败时使用本地 JSON 解析兜底。
     */
    private ArticleState.Agent4Result parseResult(String response) {
        try {
            return outputConverter.convert(response);
        } catch (RuntimeException e) {
            log.warn("阶段4：结构化配图需求解析失败，尝试使用 JSON 兜底解析", e);
            String jsonText = stripMarkdownFence(response);
            return JSONUtil.toBean(jsonText, ArticleState.Agent4Result.class);
        }
    }

    /**
     * 去除模型偶发返回的 Markdown 代码块包装。
     */
    private String stripMarkdownFence(String response) {
        if (StrUtil.isBlank(response)) {
            throw new IllegalArgumentException("配图需求响应为空");
        }
        return response
                .replaceFirst("^\\s*```json\\s*", "")
                .replaceFirst("^\\s*```\\s*", "")
                .replaceFirst("\\s*```\\s*$", "")
                .trim();
    }
}
