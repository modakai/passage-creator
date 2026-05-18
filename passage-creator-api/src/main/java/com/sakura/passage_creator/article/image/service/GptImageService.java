package com.sakura.passage_creator.article.image.service;

import cn.hutool.core.util.StrUtil;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.model.enums.ImageMethodEnum;
import com.sakura.passage_creator.billing.api.AiBillingReservation;
import com.sakura.passage_creator.billing.api.AiBillingService;
import com.sakura.passage_creator.creation.workflow.image.OpenAiImageGenerationClient;
import com.sakura.passage_creator.creation.workflow.image.OpenAiImageProperties;
import com.sakura.passage_creator.creation.workflow.image.WorkflowImageData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * GPT Image 2 图片生成服务，封装 OpenAI Image API 调用。
 */
@Service
@Slf4j
public class GptImageService implements ImageGenerateStrategy {

    /**
     * OpenAI 图片配置。
     */
    private final OpenAiImageProperties properties;

    /**
     * 共享 OpenAI 图片生成客户端。
     */
    private final OpenAiImageGenerationClient imageGenerationClient;

    /**
     * AI 计费服务，GPT_IMAGE 按固定图片成本计费。
     */
    private final AiBillingService aiBillingService;

    public GptImageService(OpenAiImageProperties properties, OpenAiImageGenerationClient imageGenerationClient,
            AiBillingService aiBillingService) {
        this.properties = properties;
        this.imageGenerationClient = imageGenerationClient;
        this.aiBillingService = aiBillingService;
    }

    /**
     * 根据配图需求生成一张图片。
     *
     * @param requirement 配图需求
     * @return 图片二进制数据
     */
    public WorkflowImageData generateImage(ArticleState.ImageRequirement requirement) {
        String prompt = buildPrompt(requirement);
        log.info("开始生成 GPT 图片, model={}, position={}", properties.getModel(), requirement.getPosition());
        return imageGenerationClient.generateImage(prompt);
    }

    /**
     * 策略入口，复用已验证的 gpt-image-2 生成和远程下载逻辑。
     */
    @Override
    public ImageGenerationResult generate(ArticleState.ImageRequirement requirement) {
        return ImageGenerationResult.builder()
                .method(getMethod())
                .imageData(generateImage(requirement))
                .build();
    }

    /**
     * 带计费上下文生成图片，调用前预扣，成功后按图片固定成本结算。
     */
    @Override
    public ImageGenerationResult generate(String taskId, Long userId, ArticleState.ImageRequirement requirement) {
        long startMillis = System.currentTimeMillis();
        AiBillingReservation reservation = aiBillingService.reserveImageCall(userId, taskId,
                "GptImageService", "IMAGE_GENERATING", "OPENAI", properties.getModel());
        try {
            ImageGenerationResult result = generate(requirement);
            aiBillingService.completeImageCall(reservation, resolveLatency(startMillis), true, null);
            return result;
        }
        catch (RuntimeException e) {
            aiBillingService.releaseReservation(reservation, resolveLatency(startMillis), e.getMessage());
            throw e;
        }
    }

    /**
     * 当前策略对应 GPT Image 2。
     */
    @Override
    public ImageMethodEnum getMethod() {
        return ImageMethodEnum.GPT_IMAGE;
    }

    /**
     * 优先使用模型分析出的 prompt，缺失时用章节标题和关键词兜底。
     */
    private String buildPrompt(ArticleState.ImageRequirement requirement) {
        if (StrUtil.isNotBlank(requirement.getPrompt())) {
            return requirement.getPrompt();
        }
        String sectionTitle = StrUtil.blankToDefault(requirement.getSectionTitle(), "article illustration");
        String keywords = StrUtil.blankToDefault(requirement.getKeywords(), sectionTitle);
        return "Create a clean editorial illustration for an article section titled \"%s\". Keywords: %s. No watermark."
                .formatted(sectionTitle, keywords);
    }

    private Integer resolveLatency(long startMillis) {
        return Math.toIntExact(Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - startMillis));
    }
}
