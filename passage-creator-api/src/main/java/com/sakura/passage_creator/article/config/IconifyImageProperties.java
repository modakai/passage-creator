package com.sakura.passage_creator.article.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Iconify 图标检索配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "iconify")
public class IconifyImageProperties {

    /**
     * Iconify 公共 API 地址。
     */
    private String apiUrl = "https://api.iconify.design";

    /**
     * 搜索返回数量，策略默认取第一枚图标。
     */
    private int searchLimit = 10;

    /**
     * 生成 SVG 的默认高度。
     */
    private int defaultHeight = 96;

    /**
     * 图标颜色，留空时使用 Iconify 默认颜色。
     */
    private String defaultColor = "";
}
