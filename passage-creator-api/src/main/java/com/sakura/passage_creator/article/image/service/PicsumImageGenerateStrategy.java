package com.sakura.passage_creator.article.image.service;

import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.model.enums.ImageMethodEnum;
import com.sakura.passage_creator.creation.workflow.image.WorkflowImageData;
import com.sakura.passage_creator.creation.workflow.image.WorkflowRemoteImageDownloader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Picsum 降级配图策略，远程随机图不可用时会生成本地 SVG 占位图。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PicsumImageGenerateStrategy implements ImageGenerateStrategy {

    private final WorkflowRemoteImageDownloader remoteImageDownloader;

    @Override
    public ImageMethodEnum getMethod() {
        return ImageMethodEnum.PICSUM;
    }

    /**
     * 获取 Picsum 随机图片；若第三方不可用，返回本地生成的 SVG，避免降级链路再次失败。
     */
    @Override
    public ImageGenerationResult generate(ArticleState.ImageRequirement requirement) {
        try {
            return ImageGenerationResult.builder()
                    .method(getMethod())
                    .imageData(remoteImageDownloader.download(buildPicsumUrl(requirement)))
                    .description("降级配图")
                    .build();
        }
        catch (RuntimeException e) {
            log.warn("Picsum 降级图片下载失败，使用本地 SVG 占位图, position={}", requirement.getPosition(), e);
            return ImageGenerationResult.builder()
                    .method(getMethod())
                    .imageData(buildLocalFallbackSvg(requirement))
                    .description("降级配图")
                    .build();
        }
    }

    /**
     * 使用任务位置作为 seed，保证同一位置有稳定但不同的随机图。
     */
    private String buildPicsumUrl(ArticleState.ImageRequirement requirement) {
        int position = requirement.getPosition() == null ? 1 : requirement.getPosition();
        return "https://picsum.photos/seed/article-%d/1200/800".formatted(position);
    }

    /**
     * 构造无外部依赖的 SVG 占位图，保证最终降级策略可用。
     */
    private WorkflowImageData buildLocalFallbackSvg(ArticleState.ImageRequirement requirement) {
        String title = escapeXml(requirement.getSectionTitle() == null ? "文章配图" : requirement.getSectionTitle());
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="1200" height="800" viewBox="0 0 1200 800">
                  <rect width="1200" height="800" fill="#f3f4f6"/>
                  <rect x="96" y="96" width="1008" height="608" rx="32" fill="#ffffff" stroke="#d1d5db" stroke-width="4"/>
                  <text x="600" y="375" text-anchor="middle" font-family="Arial, sans-serif" font-size="48" fill="#111827">%s</text>
                  <text x="600" y="440" text-anchor="middle" font-family="Arial, sans-serif" font-size="28" fill="#6b7280">Image placeholder</text>
                </svg>
                """.formatted(title);
        return WorkflowImageData.builder()
                .bytes(svg.getBytes(StandardCharsets.UTF_8))
                .mimeType("image/svg+xml")
                .extension(".svg")
                .build();
    }

    /**
     * 转义 SVG 文本节点，避免标题中的特殊字符破坏 XML。
     */
    private String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
