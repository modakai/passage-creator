package com.sakura.passage_creator.rednote.agent.tool.search;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tavily 网页搜索工具实现，面向 SearchAgent 返回轻量摘要。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TavilyWebSearchTool implements WebSearchTool {

    private final TavilySearchProperties properties;

    private final RestClient restClient = RestClient.builder().build();

    @Override
    public String name() {
        return "tavily";
    }

    @Override
    public boolean isAvailable() {
        return StrUtil.isNotBlank(properties.getApiKey());
    }

    /**
     * 调用 Tavily Search API，并把响应结果压缩成 SearchAgent 需要的摘要列表。
     */
    @Override
    public List<RednoteSearchDocument> search(String query) {
        if (StrUtil.isBlank(query)) {
            return List.of();
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", query);
        body.put("search_depth", properties.getSearchDepth());
        body.put("max_results", properties.getMaxResults());
        body.put("topic", properties.getTopic());
        body.put("include_answer", properties.getIncludeAnswer());
        body.put("include_raw_content", false);
        body.put("include_images", false);

        String responseBody = restClient.post()
                .uri(properties.getApiUrl())
                .header("Authorization", "Bearer " + properties.getApiKey())
                .body(body)
                .retrieve()
                .body(String.class);
        List<RednoteSearchDocument> documents = parseResults(responseBody);
        log.info("Tavily 搜索完成, query={}, resultCount={}", query, documents.size());
        return documents;
    }

    /**
     * 从 Tavily JSON 响应中提取标题、摘要和来源信息。
     */
    private List<RednoteSearchDocument> parseResults(String responseBody) {
        if (StrUtil.isBlank(responseBody)) {
            return List.of();
        }
        JSONObject root = JSONUtil.parseObj(responseBody);
        JSONArray results = root.getJSONArray("results");
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        List<RednoteSearchDocument> documents = new ArrayList<>();
        for (Object item : results) {
            JSONObject result = JSONUtil.parseObj(item);
            String url = result.getStr("url");
            documents.add(new RednoteSearchDocument(
                    result.getStr("title"),
                    result.getStr("content"),
                    resolveSourceName(url),
                    url
            ));
        }
        return documents;
    }

    /**
     * 使用 URL 主机名作为来源名称，解析失败时返回通用来源名。
     */
    private String resolveSourceName(String url) {
        if (StrUtil.isBlank(url)) {
            return "unknown";
        }
        try {
            return URI.create(url).getHost();
        } catch (RuntimeException e) {
            return "unknown";
        }
    }
}
