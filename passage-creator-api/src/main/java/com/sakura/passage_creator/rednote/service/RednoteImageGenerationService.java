package com.sakura.passage_creator.rednote.service;

import com.sakura.passage_creator.creation.workflow.image.OpenAiImageGenerationClient;
import com.sakura.passage_creator.creation.workflow.image.WorkflowImageData;
import com.sakura.passage_creator.creation.workflow.image.WorkflowImageStorageService;
import com.sakura.passage_creator.billing.api.AiBillingReservation;
import com.sakura.passage_creator.billing.api.AiBillingService;
import com.sakura.passage_creator.rednote.workflow.state.RednoteWorkflowState;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 小红书图片生成服务，统一调用共享 OpenAI 图片模型并上传结果到 OSS。
 */
@Service
@Slf4j
public class RednoteImageGenerationService {

    /**
     * rednote 图片业务目录。
     */
    private static final String REDNOTE_BIZ_TYPE = "rednote";

    /**
     * 共享 OpenAI 图片生成客户端。
     */
    private final OpenAiImageGenerationClient imageGenerationClient;

    /**
     * 图片存储服务，用于把生成结果转存到项目 OSS。
     */
    private final WorkflowImageStorageService imageStorageService;

    /**
     * AI 计费服务，rednote 图片按数据库中的图片模型固定成本逐张结算。
     */
    private final AiBillingService aiBillingService;

    public RednoteImageGenerationService(OpenAiImageGenerationClient imageGenerationClient,
                                         WorkflowImageStorageService imageStorageService,
                                         AiBillingService aiBillingService) {
        this.imageGenerationClient = imageGenerationClient;
        this.imageStorageService = imageStorageService;
        this.aiBillingService = aiBillingService;
    }

    /**
     * 生成单张 rednote 图片并返回可持久化结果。
     */
    public RednoteWorkflowState.RednoteImageResult generate(String taskId, Long userId, String prompt, int position, String type) {
        long startMillis = System.currentTimeMillis();
        AiBillingReservation reservation = aiBillingService.reserveImageCall(userId, taskId,
                "RednoteImageGenerationService", "IMAGE_GENERATING", "OPENAI", imageGenerationClient.getModel());
        try {
            String url = generateAndUpload(taskId, prompt);
            aiBillingService.completeImageCall(reservation, resolveLatency(startMillis), true, null);
            return RednoteWorkflowState.RednoteImageResult.builder()
                    .position(position)
                    .type(type)
                    .url(url)
                    .prompt(prompt)
                    .model(imageGenerationClient.getModel())
                    .status("SUCCESS")
                    .build();
        } catch (RuntimeException e) {
            aiBillingService.releaseReservation(reservation, resolveLatency(startMillis), e.getMessage());
            log.error("异常 ", e);
            return RednoteWorkflowState.RednoteImageResult.builder()
                    .position(position)
                    .type(type)
                    .prompt(prompt)
                    .model(imageGenerationClient.getModel())
                    .status("FAILED")
                    .errorMessage(StringUtils.defaultIfBlank(e.getMessage(), "图片生成失败"))
                    .build();
        }
    }

    /**
     * 调用共享 OpenAI 图片模型并把结果上传到 OSS。
     */
    private String generateAndUpload(String taskId, String prompt) {
        if (StringUtils.isBlank(prompt)) {
            throw new IllegalArgumentException("图片提示词不能为空");
        }
        WorkflowImageData imageData = imageGenerationClient.generateImage(prompt);
        return imageStorageService.uploadGeneratedImage(REDNOTE_BIZ_TYPE, taskId, imageData);
    }

    /**
     * 计算图片模型调用耗时，避免超过 Integer 上限。
     */
    private Integer resolveLatency(long startMillis) {
        return Math.toIntExact(Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - startMillis));
    }

}
