package com.sakura.passage_creator.rednote.agent.tool.search;

import java.util.List;

/**
 * Rednote SearchAgent 使用的网页搜索工具边界。
 */
public interface WebSearchTool {

    /**
     * 工具名称，用于日志和故障定位。
     */
    String name();

    /**
     * 搜索网页并返回已清洗的结果摘要。
     *
     * @param query 搜索关键词
     * @return 搜索结果摘要
     */
    List<RednoteSearchDocument> search(String query);

    /**
     * 判断工具当前是否满足运行条件，例如 API Key 是否已配置。
     */
    default boolean isAvailable() {
        return true;
    }
}
