package com.sakura.passage_creator.rednote.service;

import com.alibaba.cloud.ai.dashscope.api.DashScopeImageApi;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageModel;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageOptions;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import com.mybatisflex.core.query.QueryWrapper;
import com.sakura.passage_creator.billing.api.AiBillingService;
import com.sakura.passage_creator.creation.workflow.image.WorkflowImageData;
import com.sakura.passage_creator.creation.workflow.image.WorkflowImageStorageService;
import com.sakura.passage_creator.creation.workflow.image.WorkflowRemoteImageDownloader;
import com.sakura.passage_creator.rednote.model.entity.RednoteNote;
import com.sakura.passage_creator.rednote.repository.RednoteNoteMapper;
import com.sakura.passage_creator.rednote.workflow.state.RednoteWorkflowState;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;

import static com.sakura.passage_creator.rednote.model.entity.table.RednoteNoteTableDef.REDNOTE_NOTE;

/**
 * 小红书图片生成服务，统一调用阿里云百练 z-image-turbo 并上传结果到 OSS。
 */
@Service
@Slf4j
public class RednoteImageGenerationService {

    /**
     * rednote 图片业务目录。
     */
    private static final String REDNOTE_BIZ_TYPE = "rednote";

    /**
     * DashScope 图片模型客户端。
     */
    private final DashScopeImageModel imageModel;

    /**
     * 远程图片下载器，用于处理 DashScope 返回的临时 URL。
     */
    private final WorkflowRemoteImageDownloader remoteImageDownloader;

    /**
     * 图片存储服务，用于把生成结果转存到项目 OSS。
     */
    private final WorkflowImageStorageService imageStorageService;

    /**
     * AI 计费服务，百练图片生成按固定图片成本计费。
     */
    private final AiBillingService aiBillingService;

    /**
     * 小红书任务 Mapper，用于读取 userId 作为计费归属。
     */
    private final RednoteNoteMapper rednoteNoteMapper;

    public RednoteImageGenerationService(@Value("${spring.ai.dashscope.api-key}") String apiKey,
                                         WorkflowRemoteImageDownloader remoteImageDownloader,
                                         WorkflowImageStorageService imageStorageService,
                                         AiBillingService aiBillingService,
                                         RednoteNoteMapper rednoteNoteMapper) {
        this.remoteImageDownloader = remoteImageDownloader;
        this.imageStorageService = imageStorageService;
        this.aiBillingService = aiBillingService;
        this.rednoteNoteMapper = rednoteNoteMapper;
        this.imageModel = DashScopeImageModel.builder()
                .dashScopeApi(DashScopeImageApi.builder().apiKey(apiKey).build())
                .defaultOptions(DashScopeImageOptions.builder()
                        .model(DashScopeModel.ImageModel.QWEN_IMAGE.name())
                        .n(1)
                        .width(1024)
                        .height(1024)
                        .responseFormat("url")
                        .promptExtend(true)
                        .watermark(false)
                        .build())
                .build();


    }

    /**
     * 生成单张 rednote 图片并返回可持久化结果。
     */
    public RednoteWorkflowState.RednoteImageResult generate(String taskId, String prompt, int position, String type) {
        long startMillis = System.currentTimeMillis();
//        AiBillingReservation reservation = aiBillingService.reserveImageCall(resolveUserId(taskId), taskId,
//                "RednoteImageGenerationService", "IMAGE_GENERATING", "DASHSCOPE", DashScopeModel.ImageModel.QWEN_IMAGE_PLUS.name());
        try {
            String url = generateAndUpload(taskId, prompt);
//            aiBillingService.completeImageCall(reservation, resolveLatency(startMillis), true, null);
            return RednoteWorkflowState.RednoteImageResult.builder()
                    .position(position)
                    .type(type)
                    .url(url)
                    .prompt(prompt)
                    .model(DashScopeModel.ImageModel.QWEN_IMAGE_PLUS.name())
                    .status("SUCCESS")
                    .build();
        } catch (RuntimeException e) {
//            aiBillingService.releaseReservation(reservation, resolveLatency(startMillis), e.getMessage());
            log.error("异常 ", e);
            return RednoteWorkflowState.RednoteImageResult.builder()
                    .position(position)
                    .type(type)
                    .prompt(prompt)
                    .model(DashScopeModel.ImageModel.QWEN_IMAGE_PLUS.name())
                    .status("FAILED")
                    .errorMessage(StringUtils.defaultIfBlank(e.getMessage(), "图片生成失败"))
                    .build();
        }
    }

    /**
     * 调用 DashScope 图片模型并把结果上传到 OSS。
     */
    private String generateAndUpload(String taskId, String prompt) {
        if (StringUtils.isBlank(prompt)) {
            throw new IllegalArgumentException("图片提示词不能为空");
        }
        ImageResponse response = imageModel.call(new ImagePrompt(prompt, DashScopeImageOptions.builder()
                .model(DashScopeModel.ImageModel.QWEN_IMAGE.getValue())
                .n(1)
                .width(1024)
                .height(1024)
                .responseFormat("url")
                .promptExtend(true)
                .watermark(false)
                .build()));
        
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new IllegalStateException("DashScope 图片模型未返回图片");
        }
        Image image = response.getResult().getOutput();
        WorkflowImageData imageData = resolveImageData(image);
        return imageStorageService.uploadGeneratedImage(REDNOTE_BIZ_TYPE, taskId, imageData);
    }

    /**
     * 兼容 URL 和 b64_json 两种图片返回方式。
     */
    private WorkflowImageData resolveImageData(Image image) {
        if (StringUtils.isNotBlank(image.getUrl())) {
            return remoteImageDownloader.download(image.getUrl());
        }
        if (StringUtils.isNotBlank(image.getB64Json())) {
            String b64Json = stripDataUrlPrefix(image.getB64Json());
            return WorkflowImageData.builder()
                    .bytes(Base64.getDecoder().decode(b64Json))
                    .mimeType("image/png")
                    .extension(".png")
                    .build();
        }
        throw new IllegalStateException("DashScope 图片结果缺少 URL 和 b64_json");
    }

    /**
     * 去掉 data URL 前缀，避免 Base64 解码失败。
     */
    private String stripDataUrlPrefix(String value) {
        int commaIndex = value.indexOf(',');
        return commaIndex >= 0 ? value.substring(commaIndex + 1) : value;
    }

    /**
     * 根据 taskId 读取创建用户，保证图片计费归属到实际任务所有者。
     */
    private Long resolveUserId(String taskId) {
        RednoteNote note = rednoteNoteMapper.selectOneByQuery(QueryWrapper.create()
                .select(REDNOTE_NOTE.USER_ID)
                .where(REDNOTE_NOTE.TASK_ID.eq(taskId)));
        if (note == null || note.getUserId() == null) {
            throw new IllegalStateException("小红书任务不存在，无法记录图片用量");
        }
        return note.getUserId();
    }

    /**
     * 计算模型调用耗时，避免超过 Integer 上限。
     */
    private Integer resolveLatency(long startMillis) {
        return Math.toIntExact(Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - startMillis));
    }
}
