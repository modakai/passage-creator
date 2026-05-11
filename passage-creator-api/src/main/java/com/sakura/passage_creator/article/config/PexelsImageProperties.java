package com.sakura.passage_creator.article.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Pexels 图库配置，用于文章配图检索。
 */
@Data
@Component
@ConfigurationProperties(prefix = "pexels")
public class PexelsImageProperties {

    /**
     * Pexels API Key，通过环境变量注入。
     */
    private String apiKey;

    /**
     * Pexels 搜索接口地址。
     */
    private String apiUrl = "https://api.pexels.com/v1/search";

    /**
     * 每次检索图片数量，首阶段只取最匹配的一张。
     */
    private int perPage = 5;

    /**
     * 图片方向，公众号文章默认更适合横图。
     */
    private String orientation = "landscape";
}
