package com.sakura.passage_creator.rednote.agent.tool.search;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.sakura.passage_creator.rednote.workflow.state.RednoteWorkflowState;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.converter.BeanOutputConverter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SearchAgent 输出解析和状态映射工具，保证 Agent 返回值与 Hook 落库使用同一套规则。
 */
@Slf4j
public final class RednoteSearchResponseSupport {

    /**
     * 默认普通配图数量，不包含封面图。
     */
    private static final int DEFAULT_IMAGE_COUNT = 3;

    /**
     * 默认标签数量。
     */
    private static final int DEFAULT_TAG_COUNT = 5;

    /**
     * 普通配图数量上限。
     */
    private static final int MAX_IMAGE_COUNT = 5;

    /**
     * SearchAgent 结构化输出转换器，用于约束和解析 RednoteBrief JSON。
     */
    private static final BeanOutputConverter<RednoteWorkflowState.SearchResponse> OUTPUT_CONVERTER =
            new BeanOutputConverter<>(RednoteWorkflowState.SearchResponse.class);

    private RednoteSearchResponseSupport() {
    }

    /**
     * 获取 RednoteBrief 结构化输出格式说明。
     */
    public static String format() {
        return OUTPUT_CONVERTER.getFormat();
    }

    /**
     * 解析模型最终文本并补齐默认值。
     */
    public static RednoteWorkflowState.SearchResponse parseAndNormalize(String response, String content) {
        RednoteWorkflowState.SearchResponse brief = parseResponse(response);
        normalizeBrief(brief, content);
        return brief;
    }

    /**
     * 将 SearchAgent 输出转换为 Graph 后续节点可直接读取的状态更新。
     */
    public static Map<String, Object> toStateUpdates(RednoteWorkflowState.SearchResponse searchResponse) {
        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put(RednoteWorkflowState.KEY_SEARCH_RESPONSE, searchResponse);
        updates.put(RednoteWorkflowState.KEY_SUBJECT, searchResponse.getSubject());
        updates.put(RednoteWorkflowState.KEY_CONTEXT, searchResponse.getContext());
        updates.put(RednoteWorkflowState.KEY_CONTENT_LENGTH, searchResponse.getContentLength());
        updates.put(RednoteWorkflowState.KEY_TARGET_WORD_COUNT, searchResponse.getTargetWordCount());
        updates.put(RednoteWorkflowState.KEY_KEYWORDS, searchResponse.getKeywords());
        updates.put(RednoteWorkflowState.KEY_TAG_COUNT, searchResponse.getTagCount());
        updates.put(RednoteWorkflowState.KEY_IMAGE_COUNT, searchResponse.getImageCount());
        updates.put(RednoteWorkflowState.KEY_SEARCH_RESULTS, searchResponse.getSearchResults());
        return updates;
    }

    /**
     * 优先使用 Spring AI 结构化输出解析，失败时抽取 JSON 对象兜底。
     */
    private static RednoteWorkflowState.SearchResponse parseResponse(String response) {
        try {
            return OUTPUT_CONVERTER.convert(response);
        } catch (RuntimeException e) {
            log.warn("Rednote SearchAgent 结构化解析失败，尝试抽取 JSON 兜底", e);
            return JSONUtil.toBean(extractJsonObject(response), RednoteWorkflowState.SearchResponse.class);
        }
    }

    /**
     * 从模型输出中截取 JSON 对象，避免少量解释文本导致解析失败。
     */
    private static String extractJsonObject(String response) {
        if (StrUtil.isBlank(response)) {
            throw new IllegalStateException("SearchAgent 未返回内容");
        }
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("SearchAgent 未返回合法 JSON");
        }
        return response.substring(start, end + 1);
    }

    /**
     * 统一补齐默认值和后端强制限制，避免后续节点处理空值或越界数量。
     */
    private static void normalizeBrief(RednoteWorkflowState.SearchResponse brief, String content) {
        if (brief == null) {
            throw new IllegalStateException("SearchAgent 未返回 RednoteBrief");
        }
        brief.setSubject(StringUtils.defaultIfBlank(brief.getSubject(), content));
        brief.setContext(StringUtils.defaultIfBlank(brief.getContext(), "基于用户原始需求和网页搜索结果整理生成创作上下文。"));
        brief.setContentLength(normalizeContentLength(brief.getContentLength()));
        brief.setTargetWordCount(normalizeTargetWordCount(brief.getContentLength(), brief.getTargetWordCount()));
        if (CollUtil.isEmpty(brief.getKeywords())) {
            brief.setKeywords(List.of(brief.getSubject()));
        }
        brief.setTagCount(brief.getTagCount() == null || brief.getTagCount() <= 0 ? DEFAULT_TAG_COUNT : brief.getTagCount());
        brief.setImageCount(normalizeImageCount(brief.getImageCount()));
        if (brief.getSearchResults() == null) {
            brief.setSearchResults(List.of());
        }
    }

    /**
     * 标准化篇幅档位。
     */
    private static String normalizeContentLength(String contentLength) {
        String value = StringUtils.defaultIfBlank(contentLength, "MEDIUM").toUpperCase();
        if (!List.of("SHORT", "MEDIUM", "LONG").contains(value)) {
            return "MEDIUM";
        }
        return value;
    }

    /**
     * 根据篇幅档位补齐目标字数。
     */
    private static Integer normalizeTargetWordCount(String contentLength, Integer targetWordCount) {
        if (targetWordCount != null && targetWordCount > 0) {
            return targetWordCount;
        }
        return switch (contentLength) {
            case "SHORT" -> 300;
            case "LONG" -> 1000;
            default -> 600;
        };
    }

    /**
     * 普通配图数量必须在 1 到 5 之间。
     */
    private static Integer normalizeImageCount(Integer imageCount) {
        if (imageCount == null || imageCount <= 0) {
            return DEFAULT_IMAGE_COUNT;
        }
        return Math.min(imageCount, MAX_IMAGE_COUNT);
    }
}
