package com.sakura.passage_creator.article.image.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.creation.workflow.image.WorkflowImageData;
import com.sakura.passage_creator.article.model.enums.ImageMethodEnum;
import com.sakura.passage_creator.billing.api.AiBillingReservation;
import com.sakura.passage_creator.billing.api.AiBillingService;
import com.sakura.passage_creator.billing.api.AiChatBillingSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * SVG 概念图策略，使用文本模型生成可直接作为图片上传的 SVG。
 */
@Service
@Slf4j
public class SvgDiagramImageGenerateStrategy implements ImageGenerateStrategy {

    private final ChatClient chatClient;

    /**
     * AI 计费服务，SVG 概念图使用文本模型 Token 计费。
     */
    private final AiBillingService aiBillingService;

    public SvgDiagramImageGenerateStrategy(DashScopeApi dashScopeApi, AiBillingService aiBillingService) {
        this.aiBillingService = aiBillingService;
        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .model(DashScopeModel.ChatModel.QWEN3_MAX.value)
                        .temperature(0.2)
                        .maxToken(3000)
                        .topP(0.8)
                        .build())
                .build();
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    @Override
    public ImageMethodEnum getMethod() {
        return ImageMethodEnum.SVG_DIAGRAM;
    }

    /**
     * 根据配图需求生成 SVG 概念图源码，并转换为 WorkflowImageData 交给 OSS 上传。
     */
    @Override
    public ImageGenerationResult generate(ArticleState.ImageRequirement requirement) {
        String requirementText = ImageRequirementTextResolver.resolve(requirement, getMethod());
        if (StrUtil.isBlank(requirementText)) {
            throw new IllegalArgumentException("SVG 概念图需求为空");
        }

        ChatResponse chatResponse = callModel(requirementText);
        String rawSvg = AiChatBillingSupport.contentOf(chatResponse);
        return buildResult(requirement, rawSvg);
    }

    /**
     * 带计费上下文生成 SVG，并记录文本模型 Token 成本。
     */
    @Override
    public ImageGenerationResult generate(String taskId, Long userId, ArticleState.ImageRequirement requirement) {
        String requirementText = ImageRequirementTextResolver.resolve(requirement, getMethod());
        if (StrUtil.isBlank(requirementText)) {
            throw new IllegalArgumentException("SVG 概念图需求为空");
        }
        long startMillis = System.currentTimeMillis();
        AiBillingReservation reservation = aiBillingService.reserveTextCall(userId, taskId,
                "SvgDiagramImageGenerateStrategy", "IMAGE_GENERATING", "DASHSCOPE",
                DashScopeModel.ChatModel.QWEN3_MAX.value);
        boolean billed = false;
        try {
            ChatResponse chatResponse = callModel(requirementText);
            aiBillingService.completeTextCall(reservation, AiChatBillingSupport.usageOf(chatResponse),
                    resolveLatency(startMillis), true, null);
            billed = true;
            return buildResult(requirement, AiChatBillingSupport.contentOf(chatResponse));
        } catch (RuntimeException e) {
            if (!billed) {
                aiBillingService.releaseReservation(reservation, resolveLatency(startMillis), e.getMessage());
            }
            throw e;
        }
    }

    private ChatResponse callModel(String requirementText) {
        return chatClient.prompt()
                .user(buildPrompt(requirementText))
                .call()
                .chatResponse();
    }

    private ImageGenerationResult buildResult(ArticleState.ImageRequirement requirement, String rawSvg) {
        String svgCode = sanitizeSvg(extractSvgCode(rawSvg));
        if (!isValidSvg(svgCode)) {
            throw new IllegalStateException("模型返回的 SVG 格式无效");
        }
        log.info("SVG 概念图生成成功, position={}, size={} chars", requirement.getPosition(), svgCode.length());
        return ImageGenerationResult.builder()
                .method(getMethod())
                .imageData(WorkflowImageData.builder()
                        .bytes(svgCode.getBytes(StandardCharsets.UTF_8))
                        .mimeType("image/svg+xml")
                        .extension(".svg")
                        .build())
                .description(requirement.getSectionTitle())
                .build();
    }

    /**
     * 约束模型只输出 SVG，避免生成解释性文本导致后续无法作为图片保存。
     */
    private String buildPrompt(String requirementText) {
        return """
                你是资深信息图设计师。请根据需求生成一张可直接保存的 SVG 概念示意图。
                
                需求：
                %s
                
                强约束：
                1. 只输出完整 <svg>...</svg>，不要解释，不要 Markdown 代码块。
                2. 使用 1200x800 画布，风格简洁、清晰、适合公众号文章。
                3. 不要引用外部图片、外部字体、script、foreignObject 或事件属性。
                4. 文字必须少量且可读，避免密集长句。
                """.formatted(requirementText);
    }

    /**
     * 去除模型偶发返回的 Markdown 包装，并截取 SVG 根节点。
     */
    String extractSvgCode(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.replace("```xml", "")
                .replace("```svg", "")
                .replace("```", "")
                .trim();
        int start = cleaned.indexOf("<svg");
        int end = cleaned.lastIndexOf("</svg>");
        if (start < 0 || end < 0) {
            return cleaned;
        }
        return cleaned.substring(start, end + "</svg>".length()).trim();
    }

    /**
     * 移除 SVG 中不适合上传展示的危险标签和事件属性。
     */
    String sanitizeSvg(String svgCode) {
        if (svgCode == null) {
            return "";
        }
        return svgCode
                .replaceAll("(?is)<script.*?</script>", "")
                .replaceAll("(?is)<foreignObject.*?</foreignObject>", "")
                .replaceAll("(?i)\\s+on[a-z]+\\s*=\\s*\"[^\"]*\"", "")
                .replaceAll("(?i)\\s+on[a-z]+\\s*=\\s*'[^']*'", "")
                .trim();
    }

    /**
     * 进行基础 SVG 格式校验，完整 XML 校验留给后续专门的 SVG 安全模块。
     */
    private boolean isValidSvg(String svgCode) {
        return StrUtil.isNotBlank(svgCode) && svgCode.contains("<svg") && svgCode.contains("</svg>");
    }

    private Integer resolveLatency(long startMillis) {
        return Math.toIntExact(Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - startMillis));
    }
}
