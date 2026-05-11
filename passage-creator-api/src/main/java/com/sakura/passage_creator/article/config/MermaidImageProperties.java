package com.sakura.passage_creator.article.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Mermaid 远程渲染配置，默认使用 Kroki 公共接口。
 */
@Data
@Component
@ConfigurationProperties(prefix = "mermaid")
public class MermaidImageProperties {

    /**
     * Kroki 服务地址，可替换为自建实例。
     */
    private String baseUrl = "https://kroki.io";

    /**
     * 渲染输出格式，文章配图默认使用 png。
     */
    private String outputFormat = "png";
}
