package com.sakura.passage_creator.article.image.service;

import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.model.enums.ImageMethodEnum;
import com.sakura.passage_creator.creation.workflow.image.WorkflowImageStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 文章配图生成服务，负责按 imageSource 分发到不同策略并统一上传 OSS。
 */
@Service
@Slf4j
public class ArticleImageGenerationService {

    private final Map<ImageMethodEnum, ImageGenerateStrategy> strategyMap = new EnumMap<>(ImageMethodEnum.class);

    private final WorkflowImageStorageService imageStorageService;

    public ArticleImageGenerationService(List<ImageGenerateStrategy> strategies,
            WorkflowImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
        for (ImageGenerateStrategy strategy : strategies) {
            strategyMap.put(strategy.getMethod(), strategy);
        }
    }

    /**
     * 生成并上传所有配图。当前保持串行，便于定位第三方接口或 OSS 错误。
     *
     * @param taskId              文章任务 id
     * @param imageRequirements   配图需求列表
     * @param imageCompleteConsumer 单张图片完成回调
     * @return 配图结果列表
     */
    public List<ArticleState.ImageResult> generateImages(String taskId,
            List<ArticleState.ImageRequirement> imageRequirements,
            Consumer<ArticleState.ImageResult> imageCompleteConsumer) {
        if (imageRequirements == null || imageRequirements.isEmpty()) {
            return List.of();
        }

        List<ArticleState.ImageResult> results = new ArrayList<>();
        for (ArticleState.ImageRequirement requirement : imageRequirements) {
            ImageGenerationResult generationResult = generateWithFallback(requirement);
            String imageUrl = imageStorageService.uploadArticleImage(taskId, generationResult.getImageData());
            ArticleState.ImageResult imageResult = buildImageResult(requirement, generationResult, imageUrl);
            results.add(imageResult);
            if (imageCompleteConsumer != null) {
                imageCompleteConsumer.accept(imageResult);
            }
        }
        return results;
    }

    /**
     * 优先使用需求中的策略，失败时使用 Picsum 降级，保证正文合成不被单张图拖垮。
     */
    private ImageGenerationResult generateWithFallback(ArticleState.ImageRequirement requirement) {
        ImageMethodEnum method = resolveMethod(requirement);
        try {
            return generateByMethod(method, requirement);
        }
        catch (RuntimeException e) {
            log.warn("配图策略执行失败，尝试降级, method={}, position={}, reason={}",
                    method, requirement.getPosition(), e.getMessage());
            log.debug("配图策略失败详情", e);
            return generateByMethod(ImageMethodEnum.getFallbackMethod(), requirement);
        }
    }

    /**
     * 根据指定策略生成图片，策略缺失或不可用时抛出异常交给上层降级。
     */
    private ImageGenerationResult generateByMethod(ImageMethodEnum method, ArticleState.ImageRequirement requirement) {
        ImageGenerateStrategy strategy = strategyMap.get(method);
        if (strategy == null || !strategy.isAvailable()) {
            throw new IllegalStateException("配图策略不可用: " + method.getValue());
        }
        ImageGenerationResult result = strategy.generate(requirement);
        if (result == null || result.getImageData() == null || !result.getImageData().isValid()) {
            throw new IllegalStateException("配图策略返回无效图片数据: " + method.getValue());
        }
        return result;
    }

    /**
     * 解析需求中的图片来源，未知来源先回到 GPT_IMAGE。
     */
    private ImageMethodEnum resolveMethod(ArticleState.ImageRequirement requirement) {
        ImageMethodEnum method = ImageMethodEnum.getByValue(requirement.getImageSource());
        return method == null ? ImageMethodEnum.getDefaultAiMethod() : method;
    }

    /**
     * 将配图需求和策略产出转换为可持久化的配图结果。
     */
    private ArticleState.ImageResult buildImageResult(
            ArticleState.ImageRequirement requirement,
            ImageGenerationResult generationResult,
            String imageUrl) {
        ArticleState.ImageResult imageResult = new ArticleState.ImageResult();
        imageResult.setPosition(requirement.getPosition());
        imageResult.setUrl(imageUrl);
        imageResult.setMethod(generationResult.getMethod().getValue());
        imageResult.setKeywords(requirement.getKeywords());
        imageResult.setSectionTitle(requirement.getSectionTitle());
        imageResult.setDescription(resolveDescription(requirement, generationResult));
        imageResult.setPlaceholderId(requirement.getPlaceholderId());
        return imageResult;
    }

    /**
     * 生成 Markdown alt 文本，优先使用章节标题。
     */
    private String resolveDescription(ArticleState.ImageRequirement requirement, ImageGenerationResult generationResult) {
        if (generationResult.getDescription() != null && !generationResult.getDescription().isBlank()) {
            return generationResult.getDescription();
        }
        if ("cover".equalsIgnoreCase(requirement.getType())) {
            return "封面图";
        }
        if (requirement.getSectionTitle() != null && !requirement.getSectionTitle().isBlank()) {
            return requirement.getSectionTitle();
        }
        return "配图";
    }
}
