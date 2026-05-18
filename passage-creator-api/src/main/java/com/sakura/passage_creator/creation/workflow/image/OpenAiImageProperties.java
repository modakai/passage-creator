package com.sakura.passage_creator.creation.workflow.image;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OpenAI 图片生成配置，集中控制 GPT 图片模型、尺寸和输出质量。
 */
@Data
@Component
@ConfigurationProperties(prefix = "openai.image")
public class OpenAiImageProperties {

    /**
     * OpenAI API Key，通过环境变量注入，禁止写死真实密钥。
     */
    private String apiKey;

    /**
     * OpenAI API 基础地址。
     */
    private String baseUrl = "https://api.openai.com/v1";

    /**
     * 图片生成模型。可通过 OPENAI_IMAGE_MODEL 切换中转站模型。
     */
    private String model = "gpt-image-2";

    /**
     * 生成图片尺寸，文章配图默认使用横图。
     */
    private String size = "1536x1024";

    /**
     * 图片质量，第一阶段使用 medium 平衡质量和成本。
     */
    private String quality = "medium";

    /**
     * 输出图片格式。
     */
    private String outputFormat = "png";

    /**
     * 除封面外最多生成的章节配图数量。
     */
    private int maxSectionImages = 2;
}
