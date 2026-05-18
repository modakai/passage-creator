package com.sakura.passage_creator.article.image.service;

import cn.hutool.core.util.StrUtil;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.config.MermaidImageProperties;
import com.sakura.passage_creator.creation.workflow.image.WorkflowImageData;
import com.sakura.passage_creator.article.model.enums.ImageMethodEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Mermaid 图表策略，通过 Kroki 把 Mermaid 文本渲染为图片。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MermaidImageGenerateStrategy implements ImageGenerateStrategy {

    private final MermaidImageProperties properties;

    private final RestClient restClient = RestClient.builder().build();

    @Override
    public ImageMethodEnum getMethod() {
        return ImageMethodEnum.MERMAID;
    }

    /**
     * 将 prompt 中的 Mermaid 代码发送到 Kroki，返回可上传 OSS 的图片字节。
     */
    @Override
    public ImageGenerationResult generate(ArticleState.ImageRequirement requirement) {
        String mermaidCode = stripMarkdownFence(ImageRequirementTextResolver.resolve(requirement, getMethod()));
        if (StrUtil.isBlank(mermaidCode)) {
            throw new IllegalArgumentException("Mermaid 配图代码为空");
        }

        ResponseEntity<byte[]> response = restClient.post()
                .uri(buildRenderUrl())
                .contentType(MediaType.TEXT_PLAIN)
                .body(mermaidCode)
                .retrieve()
                .toEntity(byte[].class);
        byte[] bytes = response.getBody();
        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("Mermaid 渲染结果为空");
        }
        log.info("Mermaid 图表渲染成功, position={}, size={} bytes", requirement.getPosition(), bytes.length);
        return ImageGenerationResult.builder()
                .method(getMethod())
                .imageData(WorkflowImageData.builder()
                        .bytes(bytes)
                        .mimeType(resolveMimeType())
                        .extension(resolveExtension())
                        .build())
                .description(requirement.getSectionTitle())
                .build();
    }

    /**
     * 构造 Kroki Mermaid 渲染地址，例如 /mermaid/png。
     */
    private String buildRenderUrl() {
        return properties.getBaseUrl().replaceAll("/+$", "") + "/mermaid/" + properties.getOutputFormat();
    }

    /**
     * 去除模型可能返回的 Markdown 代码块包装。
     */
    private String stripMarkdownFence(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceFirst("^\\s*```mermaid\\s*", "")
                .replaceFirst("^\\s*```\\s*", "")
                .replaceFirst("\\s*```\\s*$", "")
                .trim();
    }

    /**
     * 根据输出格式推导 MIME 类型。
     */
    private String resolveMimeType() {
        if ("svg".equalsIgnoreCase(properties.getOutputFormat())) {
            return "image/svg+xml";
        }
        if ("jpeg".equalsIgnoreCase(properties.getOutputFormat()) || "jpg".equalsIgnoreCase(properties.getOutputFormat())) {
            return "image/jpeg";
        }
        return "image/png";
    }

    /**
     * 根据输出格式推导文件扩展名。
     */
    private String resolveExtension() {
        if ("svg".equalsIgnoreCase(properties.getOutputFormat())) {
            return ".svg";
        }
        if ("jpeg".equalsIgnoreCase(properties.getOutputFormat()) || "jpg".equalsIgnoreCase(properties.getOutputFormat())) {
            return ".jpg";
        }
        return ".png";
    }
}
