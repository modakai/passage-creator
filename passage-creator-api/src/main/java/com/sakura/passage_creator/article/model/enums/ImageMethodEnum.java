package com.sakura.passage_creator.article.model.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * 文章配图方式枚举。新增图片来源时，需要同步提供对应的 ImageGenerateStrategy 实现。
 */
@Getter
public enum ImageMethodEnum {

    /**
     * Pexels 图库检索。
     */
    PEXELS("PEXELS", "Pexels 图库", false, false),

    /**
     * Mermaid 流程图生成。
     */
    MERMAID("MERMAID", "Mermaid 流程图", true, false),

    /**
     * Iconify 图标库检索。
     */
    ICONIFY("ICONIFY", "Iconify 图标", false, false),

    /**
     * AI 生成 SVG 概念示意图。
     */
    SVG_DIAGRAM("SVG_DIAGRAM", "SVG 概念图", true, false),

    /**
     * OpenAI GPT Image 2 文生图。
     */
    GPT_IMAGE("GPT_IMAGE", "GPT Image 2 配图", true, false),

    /**
     * Picsum 随机图，作为其他策略失败后的降级来源。
     */
    PICSUM("PICSUM", "Picsum 降级图片", false, true);

    /**
     * 配图方式值，持久化和前后端传输均使用该值。
     */
    private final String value;

    /**
     * 配图方式说明。
     */
    private final String description;

    /**
     * 是否更适合使用 prompt 生成内容，false 时优先使用 keywords 检索。
     */
    private final boolean aiGenerated;

    /**
     * 是否为降级方案，降级方案不会主动交给配图分析 Agent 选择。
     */
    private final boolean fallback;

    ImageMethodEnum(String value, String description, boolean aiGenerated, boolean fallback) {
        this.value = value;
        this.description = description;
        this.aiGenerated = aiGenerated;
        this.fallback = fallback;
    }

    /**
     * 根据前后端传输值查找枚举，未知值返回 null，交给策略层兜底。
     */
    public static ImageMethodEnum getByValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (ImageMethodEnum method : values()) {
            if (method.value.equalsIgnoreCase(value)) {
                return method;
            }
        }
        return null;
    }

    /**
     * 返回默认 AI 生图方式，用于未知来源降级。
     */
    public static ImageMethodEnum getDefaultAiMethod() {
        return GPT_IMAGE;
    }

    /**
     * 返回默认最终降级方式。
     */
    public static ImageMethodEnum getFallbackMethod() {
        return PICSUM;
    }

    /**
     * 返回可由用户主动选择的配图方式，降级方式不暴露给用户选择。
     */
    public static List<ImageMethodEnum> userSelectableMethods() {
        return Arrays.stream(values())
                .filter(method -> !method.isFallback())
                .toList();
    }
}
