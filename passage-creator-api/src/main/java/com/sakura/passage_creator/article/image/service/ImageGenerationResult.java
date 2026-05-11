package com.sakura.passage_creator.article.image.service;

import com.sakura.passage_creator.article.model.dto.image.ImageData;
import com.sakura.passage_creator.article.model.enums.ImageMethodEnum;
import lombok.Builder;
import lombok.Data;

/**
 * 单张配图生成结果，作为策略层和编排层之间的统一返回对象。
 */
@Data
@Builder
public class ImageGenerationResult {

    /**
     * 实际产出图片的策略方法，降级时可能不同于用户请求的方法。
     */
    private ImageMethodEnum method;

    /**
     * 可上传到 OSS 的图片二进制数据。
     */
    private ImageData imageData;

    /**
     * 可选描述，策略可以覆盖 Markdown alt 文本。
     */
    private String description;
}
