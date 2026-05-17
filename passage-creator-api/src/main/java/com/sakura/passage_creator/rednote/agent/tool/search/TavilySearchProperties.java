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
     * Tavily Extract API 地址。
     */
    private String extractApiUrl = "https://api.tavily.com/extract";

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

    /**
     * 网页正文提取深度，basic 成本更低，advanced 适合动态或复杂页面。
     */
    private String extractDepth = "basic";

    /**
     * Tavily Extract 返回格式，markdown 更适合保留标题和结构。
     */
    private String extractFormat = "markdown";

    /**
     * Query-focused extraction 每个来源返回的相关片段数量。
     */
    private Integer chunksPerSource = 3;

    /**
     * Tavily Extract 超时时间，单位秒。
     */
    private Double extractTimeoutSeconds = 8.0;

    /**
     * 单个 URL 返回给 Agent 的最大正文长度。
     */
    private Integer maxExtractContentLength = 6000;
}
