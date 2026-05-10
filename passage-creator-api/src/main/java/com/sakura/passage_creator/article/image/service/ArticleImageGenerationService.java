package com.sakura.passage_creator.article.image.service;

import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.model.dto.image.ImageData;
import com.sakura.passage_creator.article.model.enums.ImageMethodEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 文章配图生成服务，串行生成图片并上传存储。
 */
@Service
@RequiredArgsConstructor
public class ArticleImageGenerationService {

    private final GptImageService gptImageService;

    private final ArticleImageStorageService imageStorageService;

    /**
     * 生成并上传所有配图。第一阶段保持串行，便于定位 OpenAI 或 OSS 错误。
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
            ImageData imageData = gptImageService.generateImage(requirement);
            String imageUrl = imageStorageService.uploadArticleImage(taskId, imageData);
            ArticleState.ImageResult imageResult = buildImageResult(requirement, imageUrl);
            results.add(imageResult);
            if (imageCompleteConsumer != null) {
                imageCompleteConsumer.accept(imageResult);
            }
        }
        return results;
    }

    /**
     * 将配图需求转换为可持久化的配图结果。
     */
    private ArticleState.ImageResult buildImageResult(ArticleState.ImageRequirement requirement, String imageUrl) {
        ArticleState.ImageResult imageResult = new ArticleState.ImageResult();
        imageResult.setPosition(requirement.getPosition());
        imageResult.setUrl(imageUrl);
        imageResult.setMethod(ImageMethodEnum.GPT_IMAGE.getValue());
        imageResult.setKeywords(requirement.getKeywords());
        imageResult.setSectionTitle(requirement.getSectionTitle());
        imageResult.setDescription(resolveDescription(requirement));
        imageResult.setPlaceholderId(requirement.getPlaceholderId());
        return imageResult;
    }

    /**
     * 生成 Markdown alt 文本，优先使用章节标题。
     */
    private String resolveDescription(ArticleState.ImageRequirement requirement) {
        if ("cover".equalsIgnoreCase(requirement.getType())) {
            return "封面图";
        }
        if (requirement.getSectionTitle() != null && !requirement.getSectionTitle().isBlank()) {
            return requirement.getSectionTitle();
        }
        return "配图";
    }
}
