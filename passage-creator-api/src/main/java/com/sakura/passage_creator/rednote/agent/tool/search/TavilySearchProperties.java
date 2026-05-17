package com.sakura.passage_creator.rednote.agent.tool.search;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Tavily 搜索工具配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "rednote.search.tavily")
public class TavilySearchProperties {

    /**
     * Tavily API Key；为空时搜索工具自动不可用。
     */
    private String apiKey;

    /**
     * Tavily Search API 地址。
     */
    private String apiUrl = "https://api.tavily.com/search";

    /**
     * 搜索深度，默认 basic 控制成本。
     */
    private String searchDepth = "basic";

    /**
     * 最大返回条数。
     */
    private Integer maxResults = 5;

    /**
     * 搜索主题分类。
     */
    private String topic = "general";

    /**
     * 是否要求 Tavily 返回答案摘要。
     */
    private Boolean includeAnswer = false;
}
