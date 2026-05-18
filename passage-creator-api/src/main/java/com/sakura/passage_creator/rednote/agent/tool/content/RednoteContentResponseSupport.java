package com.sakura.passage_creator.rednote.agent.tool.content;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import com.sakura.passage_creator.rednote.workflow.state.RednoteWorkflowState;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.converter.BeanOutputConverter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文案 Agent 输出解析和状态映射工具，保证正文与标签落库前完成基础清洗。
 */
@Slf4j
public final class RednoteContentResponseSupport {

    /**
     * 默认标签数量。
     */
    private static final int DEFAULT_TAG_COUNT = 5;

    /**
     * 模型没有返回标签时使用的兜底标签池，避免写入重复标签。
     */
    private static final List<String> DEFAULT_TAGS = List.of("#小红书", "#干货", "#经验分享", "#避坑", "#收藏");

    /**
     * 文案 Agent 结构化输出转换器，用于约束 JSON 结果。
     */
    private static final BeanOutputConverter<RednoteWorkflowState.ContentResponse> OUTPUT_CONVERTER =
            new BeanOutputConverter<>(RednoteWorkflowState.ContentResponse.class);

    private RednoteContentResponseSupport() {
    }

    /**
     * 获取 ContentResponse 结构化输出格式说明。
     */
    public static String format() {
        return OUTPUT_CONVERTER.getFormat();
    }

    /**
     * 解析模型最终文本并补齐正文与标签默认值。
     */
    public static RednoteWorkflowState.ContentResponse parseAndNormalize(String response, Integer tagCount) {
        RednoteWorkflowState.ContentResponse contentResponse = parseResponse(response);
        normalizeContent(contentResponse, tagCount);
        return contentResponse;
    }

    /**
     * 将文案 Agent 输出转换为 Graph 后续节点可读取的状态更新。
     */
    public static Map<String, Object> toStateUpdates(RednoteWorkflowState.ContentResponse contentResponse) {
        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put(RednoteWorkflowState.KEY_COPYWRITING, contentResponse);
        updates.put(RednoteWorkflowState.KEY_BODY_CONTENT, contentResponse.getBodyContent());
        // tags 会继续进入图片提示词 Agent 的模板，转成 JSON 字符串避免集合变量渲染失败。
        updates.put(RednoteWorkflowState.KEY_TAGS, JSONUtil.toJsonStr(contentResponse.getTags()));
        return updates;
    }

    /**
     * 优先使用 Spring AI 结构化输出解析，失败时抽取 JSON 对象兜底。
     */
    private static RednoteWorkflowState.ContentResponse parseResponse(String response) {
        try {
            RednoteWorkflowState.ContentResponse contentResponse = OUTPUT_CONVERTER.convert(response);
            fillBodyContentAlias(contentResponse, extractJsonObject(response));
            return contentResponse;
        } catch (RuntimeException e) {
            log.warn("Rednote ContentAgent 结构化解析失败，尝试抽取 JSON 兜底", e);
            String jsonText = extractJsonObject(response);
            RednoteWorkflowState.ContentResponse contentResponse = JSONUtil.toBean(jsonText, RednoteWorkflowState.ContentResponse.class);
            fillBodyContentAlias(contentResponse, jsonText);
            return contentResponse;
        }
    }

    /**
     * 兼容模型返回数据库字段名 body_content，或用户口误写成 bady_content 的情况。
     */
    private static void fillBodyContentAlias(RednoteWorkflowState.ContentResponse contentResponse, String jsonText) {
        if (contentResponse == null || StringUtils.isNotBlank(contentResponse.getBodyContent())) {
            return;
        }
        JSONObject jsonObject = JSONUtil.parseObj(jsonText);
        String bodyContent = StringUtils.defaultIfBlank(jsonObject.getStr("body_content"), jsonObject.getStr("bady_content"));
        contentResponse.setBodyContent(bodyContent);
    }

    /**
     * 从模型输出中截取 JSON 对象，避免少量解释文本导致解析失败。
     */
    private static String extractJsonObject(String response) {
        if (StrUtil.isBlank(response)) {
            throw new IllegalStateException("ContentAgent 未返回内容");
        }
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("ContentAgent 未返回合法 JSON");
        }
        return response.substring(start, end + 1);
    }

    /**
     * 统一补齐正文和标签，避免落库字段为空。
     */
    public static void normalizeContent(RednoteWorkflowState.ContentResponse contentResponse, Integer tagCount) {
        if (contentResponse == null) {
            throw new IllegalStateException("ContentAgent 未返回结构化文案");
        }
        contentResponse.setBodyContent(StringUtils.defaultIfBlank(contentResponse.getBodyContent(), "暂无小红书正文内容。"));
        contentResponse.setTags(normalizeTags(contentResponse.getTags(), tagCount));
    }

    /**
     * 清洗标签数量和格式，统一保存为带 # 的标签文本。
     */
    private static List<String> normalizeTags(List<String> tags, Integer tagCount) {
        int expectedCount = tagCount == null || tagCount <= 0 ? DEFAULT_TAG_COUNT : tagCount;
        List<String> normalizedTags = new ArrayList<>();
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                if (StringUtils.isBlank(tag)) {
                    continue;
                }
                String normalizedTag = tag.trim();
                normalizedTags.add(normalizedTag.startsWith("#") ? normalizedTag : "#" + normalizedTag);
                if (normalizedTags.size() >= expectedCount) {
                    return normalizedTags;
                }
            }
        }
        int defaultIndex = 0;
        while (normalizedTags.size() < expectedCount) {
            String defaultTag = DEFAULT_TAGS.get(defaultIndex % DEFAULT_TAGS.size());
            normalizedTags.add(defaultIndex < DEFAULT_TAGS.size() ? defaultTag : defaultTag + (defaultIndex + 1));
            defaultIndex++;
        }
        return normalizedTags;
    }
}
