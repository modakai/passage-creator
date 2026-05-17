package com.sakura.passage_creator.rednote.agent.tool.search;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.sakura.passage_creator.rednote.constant.UniversalConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 暴露给 SearchAgent 的 Tavily Extract 工具，用于把搜索结果 URL 压缩成可创作素材。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RednoteUrlFetchTools {

    private final TavilySearchProperties properties;

    private final RestClient restClient = RestClient.builder().build();

    /**
     * 使用 Tavily Extract 提取公开 URL 的正文片段，并返回适合 SearchAgent 摘要的 JSON。
     */
    @Tool(name = UniversalConstant.URL_FETCH_TOOL_NAME, description = "使用 Tavily Extract 抓取公开网页 URL 的正文片段，用于补充小红书创作素材。")
    public String fetchUrl(@ToolParam(description = "来自 rednote_web_search 结果的公开网页 URL。") String url,
                           @ToolParam(description = "可选。用户主题或搜索关键词，用于 Tavily 返回最相关的正文片段。") String query) {
        if (StrUtil.isBlank(url)) {
            return JSONUtil.toJsonStr(Map.of(
                    "status", "EMPTY_URL",
                    "message", "URL 为空",
                    "url", ""
            ));
        }
        if (StrUtil.isBlank(properties.getApiKey())) {
            return JSONUtil.toJsonStr(Map.of(
                    "status", "UNAVAILABLE",
                    "message", "未配置 Tavily API Key，无法使用网页正文提取",
                    "url", url
            ));
        }
        try {
            String responseBody = restClient.post()
                    .uri(properties.getExtractApiUrl())
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .body(buildRequestBody(url, query))
                    .retrieve()
                    .body(String.class);
            return parseExtractResponse(responseBody, url);
        } catch (RuntimeException e) {
            log.warn("rednote_url_fetch 调用失败, url={}", url, e);
            return JSONUtil.toJsonStr(Map.of(
                    "status", "ERROR",
                    "message", "Tavily Extract 调用失败：" + e.getMessage(),
                    "url", url
            ));
        }
    }

    /**
     * 构建 Tavily Extract 请求体；提供 query 时启用相关片段提取，降低上下文噪声。
     */
    private Map<String, Object> buildRequestBody(String url, String query) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("urls", url);
        body.put("extract_depth", properties.getExtractDepth());
        body.put("format", properties.getExtractFormat());
        body.put("timeout", properties.getExtractTimeoutSeconds());
        body.put("include_images", false);
        body.put("include_favicon", false);
        body.put("include_usage", true);
        if (StrUtil.isNotBlank(query)) {
            body.put("query", query);
            body.put("chunks_per_source", properties.getChunksPerSource());
        }
        return body;
    }

    /**
     * 解析 Tavily Extract 响应，统一成 Agent 消费的工具输出结构。
     */
    private String parseExtractResponse(String responseBody, String fallbackUrl) {
        if (StrUtil.isBlank(responseBody)) {
            return JSONUtil.toJsonStr(Map.of(
                    "status", "EMPTY",
                    "message", "Tavily Extract 未返回内容",
                    "url", fallbackUrl
            ));
        }
        JSONObject root = JSONUtil.parseObj(responseBody);
        JSONArray results = root.getJSONArray("results");
        if (results == null || results.isEmpty()) {
            return JSONUtil.toJsonStr(Map.of(
                    "status", "EMPTY",
                    "message", failedMessage(root),
                    "url", fallbackUrl
            ));
        }
        JSONObject result = JSONUtil.parseObj(results.get(0));
        String rawContent = StringUtils.defaultString(result.getStr("raw_content"));
        String content = truncate(rawContent);
        return JSONUtil.toJsonStr(Map.of(
                "status", StrUtil.isBlank(content) ? "EMPTY" : "OK",
                "message", StrUtil.isBlank(content) ? "未提取到有效正文" : "Tavily Extract 网页正文提取完成",
                "url", StringUtils.defaultIfBlank(result.getStr("url"), fallbackUrl),
                "title", resolveTitle(content),
                "content", content,
                "contentLength", content.length(),
                "truncated", rawContent.length() > content.length()
        ));
    }

    /**
     * 截断过长正文，控制工具输出体积。
     */
    private String truncate(String content) {
        int maxContentLength = properties.getMaxExtractContentLength() == null ? 6000 : properties.getMaxExtractContentLength();
        if (content.length() <= maxContentLength) {
            return content;
        }
        return content.substring(0, maxContentLength);
    }

    /**
     * 从 markdown 第一行标题中尽量提取页面标题。
     */
    private String resolveTitle(String content) {
        if (StrUtil.isBlank(content)) {
            return "";
        }
        String firstLine = content.lines()
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse("");
        return firstLine.replaceFirst("^#+\\s*", "").trim();
    }

    /**
     * 从 failed_results 中提取 Tavily 失败原因。
     */
    private String failedMessage(JSONObject root) {
        JSONArray failedResults = root.getJSONArray("failed_results");
        if (failedResults == null || failedResults.isEmpty()) {
            return "Tavily Extract 未提取到正文";
        }
        JSONObject failedResult = JSONUtil.parseObj(failedResults.get(0));
        return "Tavily Extract 提取失败：" + StringUtils.defaultString(failedResult.getStr("error"));
    }
}
