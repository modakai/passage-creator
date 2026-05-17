package com.sakura.passage_creator.rednote.agent.tool.search;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.sakura.passage_creator.rednote.constant.UniversalConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 暴露给 ReActAgent 的小红书网页搜索工具集合。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RednoteWebSearchTools {

    private final List<WebSearchTool> webSearchTools;

    /**
     * 搜索公开网页并返回适合 RednoteBrief 的摘要 JSON。
     */
    @Tool(name = UniversalConstant.SEARCH_TOOL_NAME, description = "搜索公开网页，返回标题、摘要、来源名称和来源链接，用于整理小红书创作简报。")
    public String searchWeb(@ToolParam(description = "搜索关键词，应包含用户主题和需要调研的内容方向。") String query) {
        if (StrUtil.isBlank(query)) {
            return JSONUtil.toJsonStr(Map.of(
                    "status", "EMPTY_QUERY",
                    "message", "搜索关键词为空",
                    "results", List.of()
            ));
        }
        Optional<WebSearchTool> searchTool = webSearchTools.stream()
                .filter(WebSearchTool::isAvailable)
                .findFirst();
        if (searchTool.isEmpty()) {
            return JSONUtil.toJsonStr(Map.of(
                    "status", "UNAVAILABLE",
                    "message", "未配置可用网页搜索工具",
                    "results", List.of()
            ));
        }
        try {
            List<RednoteSearchDocument> documents = searchTool.get().search(query);
            return JSONUtil.toJsonStr(Map.of(
                    "status", CollUtil.isEmpty(documents) ? "EMPTY" : "OK",
                    "message", "使用 " + searchTool.get().name() + " 完成网页搜索",
                    "results", documents
            ));
        } catch (RuntimeException e) {
            log.warn("rednote_web_search 调用失败, query={}", query, e);
            return JSONUtil.toJsonStr(Map.of(
                    "status", "ERROR",
                    "message", "网页搜索失败：" + e.getMessage(),
                    "results", List.of()
            ));
        }
    }
}
