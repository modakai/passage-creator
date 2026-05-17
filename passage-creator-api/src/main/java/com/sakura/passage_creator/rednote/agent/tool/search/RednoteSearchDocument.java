package com.sakura.passage_creator.rednote.agent.tool.search;

/**
 * 搜索工具返回的已清洗文档摘要。
 *
 * @param title      标题
 * @param summary    摘要
 * @param sourceName 来源名称
 * @param sourceUrl  来源链接
 */
public record RednoteSearchDocument(String title, String summary, String sourceName, String sourceUrl) {
}
