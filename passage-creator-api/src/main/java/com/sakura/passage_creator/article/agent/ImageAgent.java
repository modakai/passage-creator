package com.sakura.passage_creator.article.agent;

import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.creation.workflow.image.WorkflowImageStorageService;
import com.sakura.passage_creator.article.image.service.ImageGenerationResult;
import com.sakura.passage_creator.article.image.service.ImageToolRegistry;
import com.sakura.passage_creator.article.model.enums.ImageMethodEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 配图 Agent，负责按用户允许的配图方式分析需求，并调用对应工具完成配图。
 */
@Component
@Slf4j
public class ImageAgent {

    private final ImageAnalyzerAgent imageAnalyzerAgent;

    private final ImageToolRegistry imageToolRegistry;

    private final WorkflowImageStorageService imageStorageService;

    public ImageAgent(ImageAnalyzerAgent imageAnalyzerAgent,
            ImageToolRegistry imageToolRegistry,
            WorkflowImageStorageService imageStorageService) {
        this.imageAnalyzerAgent = imageAnalyzerAgent;
        this.imageToolRegistry = imageToolRegistry;
        this.imageStorageService = imageStorageService;
    }

    /**
     * 使用配图分析 Agent 生成配图计划，分析阶段会尊重 state.enabledImageMethods。
     */
    public void analyze(ArticleState state) {
        imageAnalyzerAgent.analyze(state, state.getUserId());
    }

    /**
     * 根据配图需求调用对应工具，工具失败时允许系统级降级到 Picsum。
     *
     * @param taskId                文章任务 id
     * @param imageRequirements     配图需求列表
     * @param imageCompleteConsumer 单张图片完成回调
     * @return 配图结果列表
     */
    public List<ArticleState.ImageResult> generateImages(String taskId,
            List<ArticleState.ImageRequirement> imageRequirements,
            Consumer<ArticleState.ImageResult> imageCompleteConsumer) {
        return generateImages(taskId, null, imageRequirements, imageCompleteConsumer);
    }

    /**
     * 根据配图需求调用对应工具，并把用户 id 传递给付费图片策略用于计费。
     *
     * @param taskId                文章任务 id
     * @param userId                用户 id
     * @param imageRequirements     配图需求列表
     * @param imageCompleteConsumer 单张图片完成回调
     * @return 配图结果列表
     */
    public List<ArticleState.ImageResult> generateImages(String taskId,
            Long userId,
            List<ArticleState.ImageRequirement> imageRequirements,
            Consumer<ArticleState.ImageResult> imageCompleteConsumer) {
        if (imageRequirements == null || imageRequirements.isEmpty()) {
            return List.of();
        }

        List<ArticleState.ImageResult> results = new ArrayList<>();
        for (ArticleState.ImageRequirement requirement : imageRequirements) {
            ImageGenerationResult generationResult = generateWithFallback(taskId, userId, requirement);
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
     * 优先调用需求指定的工具，失败后降级到 Picsum，保证文章不会因单图失败而整体中断。
     */
    private ImageGenerationResult generateWithFallback(String taskId, Long userId, ArticleState.ImageRequirement requirement) {
        ImageMethodEnum method = resolveMethod(requirement);
        try {
            return generateByMethod(taskId, userId, method, requirement);
        }
        catch (RuntimeException e) {
            log.warn("配图工具执行失败，尝试降级, method={}, position={}, reason={}",
                    method, requirement.getPosition(), e.getMessage());
            log.debug("配图工具失败详情", e);
            return generateByMethod(taskId, userId, ImageMethodEnum.getFallbackMethod(), requirement);
        }
    }

    /**
     * 调用指定工具并校验返回数据，防止无效图片进入 OSS 上传。
     */
    private ImageGenerationResult generateByMethod(String taskId, Long userId, ImageMethodEnum method,
            ArticleState.ImageRequirement requirement) {
        ImageGenerationResult result = imageToolRegistry.getRequiredTool(method).generate(taskId, userId, requirement);
        if (result == null || result.getImageData() == null || !result.getImageData().isValid()) {
            throw new IllegalStateException("配图工具返回无效图片数据: " + method.getValue());
        }
        return result;
    }

    /**
     * 解析需求中的图片来源，未知来源回到默认 AI 生图工具。
     */
    private ImageMethodEnum resolveMethod(ArticleState.ImageRequirement requirement) {
        ImageMethodEnum method = ImageMethodEnum.getByValue(requirement.getImageSource());
        return method == null ? ImageMethodEnum.getDefaultAiMethod() : method;
    }

    /**
     * 将工具结果转换为文章可持久化的配图结果。
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
     * 生成 Markdown alt 文本，优先使用工具返回的描述。
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
