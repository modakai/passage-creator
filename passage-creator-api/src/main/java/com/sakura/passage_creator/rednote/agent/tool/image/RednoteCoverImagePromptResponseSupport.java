package com.sakura.passage_creator.rednote.agent.tool.image;

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
 * 封面图提示词 Agent 输出解析工具，负责提取封面文案并组装最终封面提示词。
 */
@Slf4j
public final class RednoteCoverImagePromptResponseSupport {

    /**
     * 默认封面标签数量。
     */
    private static final int DEFAULT_COVER_TAG_COUNT = 5;

    /**
     * 封面图提示词结构化输出转换器。
     */
    private static final BeanOutputConverter<RednoteWorkflowState.CoverImagePromptResponse> OUTPUT_CONVERTER =
            new BeanOutputConverter<>(RednoteWorkflowState.CoverImagePromptResponse.class);

    private RednoteCoverImagePromptResponseSupport() {
    }

    /**
     * 获取封面图提示词结构化输出格式说明。
     */
    public static String format() {
        return OUTPUT_CONVERTER.getFormat();
    }

    /**
     * 解析并清洗封面图提示词输出。
     */
    public static RednoteWorkflowState.CoverImagePromptResponse parseAndNormalize(String response, String subject,
                                                                                 String bodyContent,
                                                                                 List<String> fallbackTags) {
        RednoteWorkflowState.CoverImagePromptResponse promptResponse = parseResponse(response);
        normalize(promptResponse, subject, bodyContent, fallbackTags);
        return promptResponse;
    }

    /**
     * 清洗封面文案和提示词，保证落库字段不为空。
     */
    public static void normalize(RednoteWorkflowState.CoverImagePromptResponse promptResponse, String subject,
                                 String bodyContent, List<String> fallbackTags) {
        if (promptResponse == null) {
            throw new IllegalStateException("封面图片提示词 Agent 未返回结构化结果");
        }
        String safeSubject = StringUtils.defaultIfBlank(subject, "小红书精选");
        promptResponse.setTitle(limitText(StringUtils.defaultIfBlank(promptResponse.getTitle(), safeSubject), 12));
        promptResponse.setSubtitle(limitText(StringUtils.defaultIfBlank(promptResponse.getSubtitle(), "这篇内容值得收藏"), 20));
        promptResponse.setDecorativeText(limitText(StringUtils.defaultIfBlank(promptResponse.getDecorativeText(), "看完直接少走弯路"), 20));
        promptResponse.setTags(normalizeTags(promptResponse.getTags(), fallbackTags));
        promptResponse.setCoverPrompt(StringUtils.defaultIfBlank(promptResponse.getCoverPrompt(),
                buildCoverPrompt(promptResponse, safeSubject, bodyContent)));
    }

    /**
     * 将封面提示词转换为 Graph state 更新。
     */
    public static Map<String, Object> toStateUpdates(RednoteWorkflowState.CoverImagePromptResponse promptResponse) {
        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put(RednoteWorkflowState.KEY_COVER_IMAGE_PROMPT_RESPONSE, promptResponse);
        updates.put(RednoteWorkflowState.KEY_COVER_TITLE, promptResponse.getTitle());
        updates.put(RednoteWorkflowState.KEY_COVER_PROMPT, promptResponse.getCoverPrompt());
        return updates;
    }

    /**
     * 优先使用结构化转换器解析，失败时兼容 markdown 标题格式和字段别名。
     */
    private static RednoteWorkflowState.CoverImagePromptResponse parseResponse(String response) {
        try {
            RednoteWorkflowState.CoverImagePromptResponse promptResponse = OUTPUT_CONVERTER.convert(response);
            fillAliases(promptResponse, extractJsonObjectOrNull(response));
            return promptResponse;
        } catch (RuntimeException e) {
            log.warn("封面图片提示词结构化解析失败，尝试 JSON 兜底", e);
            String jsonText = extractJsonObjectOrNull(response);
            if (StringUtils.isNotBlank(jsonText)) {
                RednoteWorkflowState.CoverImagePromptResponse promptResponse =
                        JSONUtil.toBean(jsonText, RednoteWorkflowState.CoverImagePromptResponse.class);
                fillAliases(promptResponse, jsonText);
                return promptResponse;
            }
            return parseMarkdown(response);
        }
    }

    /**
     * 兼容 decorative_text、coverPrompt 等常见字段别名。
     */
    private static void fillAliases(RednoteWorkflowState.CoverImagePromptResponse promptResponse, String jsonText) {
        if (promptResponse == null || StringUtils.isBlank(jsonText)) {
            return;
        }
        JSONObject jsonObject = JSONUtil.parseObj(jsonText);
        if (StringUtils.isBlank(promptResponse.getDecorativeText())) {
            promptResponse.setDecorativeText(jsonObject.getStr("decorative_text"));
        }
        if (StringUtils.isBlank(promptResponse.getCoverPrompt())) {
            promptResponse.setCoverPrompt(jsonObject.getStr("cover_prompt"));
        }
        if (CollUtil.isEmpty(promptResponse.getTags())) {
            promptResponse.setTags(parseTags(jsonObject.get("tags")));
        }
    }

    /**
     * 兼容模型返回 markdown 分段格式。
     */
    private static RednoteWorkflowState.CoverImagePromptResponse parseMarkdown(String response) {
        if (StrUtil.isBlank(response)) {
            throw new IllegalStateException("封面图片提示词 Agent 未返回内容");
        }
        return RednoteWorkflowState.CoverImagePromptResponse.builder()
                .title(extractAfterHeading(response, "封面图主标题"))
                .subtitle(extractAfterHeading(response, "封面图副标题"))
                .decorativeText(extractAfterHeading(response, "点缀文案"))
                .tags(parseTags(extractAfterHeading(response, "相关标签")))
                .build();
    }

    /**
     * 从 markdown 标题下提取首个非空内容行。
     */
    private static String extractAfterHeading(String response, String heading) {
        int start = response.indexOf(heading);
        if (start < 0) {
            return null;
        }
        String[] lines = response.substring(start + heading.length()).split("\\R");
        for (String line : lines) {
            String cleaned = line.replace("#", "").trim();
            if (StringUtils.isBlank(cleaned) || cleaned.contains("封面图") || cleaned.contains("点缀文案")
                    || cleaned.contains("相关标签")) {
                continue;
            }
            return cleaned;
        }
        return null;
    }

    /**
     * 从模型输出中截取 JSON 对象，未找到时返回 null 以便 markdown 兜底。
     */
    private static String extractJsonObjectOrNull(String response) {
        if (StrUtil.isBlank(response)) {
            return null;
        }
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return response.substring(start, end + 1);
    }

    /**
     * 清洗封面标签，保证格式统一为 # 开头。
     */
    private static List<String> normalizeTags(List<String> tags, List<String> fallbackTags) {
        List<String> normalized = new ArrayList<>();
        appendTags(normalized, tags);
        appendTags(normalized, fallbackTags);
        int index = 1;
        while (normalized.size() < DEFAULT_COVER_TAG_COUNT) {
            normalized.add("#封面关键词" + index);
            index++;
        }
        return normalized.stream().limit(DEFAULT_COVER_TAG_COUNT).toList();
    }

    /**
     * 批量追加标签并补齐 # 前缀。
     */
    private static void appendTags(List<String> target, List<String> tags) {
        if (CollUtil.isEmpty(tags)) {
            return;
        }
        for (String tag : tags) {
            if (StringUtils.isBlank(tag)) {
                continue;
            }
            String normalizedTag = tag.trim();
            target.add(normalizedTag.startsWith("#") ? normalizedTag : "#" + normalizedTag);
            if (target.size() >= DEFAULT_COVER_TAG_COUNT) {
                return;
            }
        }
    }

    /**
     * 解析标签字段，兼容数组和空格分隔字符串。
     */
    private static List<String> parseTags(Object value) {
        List<String> tags = new ArrayList<>();
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item != null && StringUtils.isNotBlank(item.toString())) {
                    tags.add(item.toString().trim());
                }
            }
            return tags;
        }
        if (value != null && StringUtils.isNotBlank(value.toString())) {
            for (String tag : value.toString().split("\\s+")) {
                if (StringUtils.isNotBlank(tag)) {
                    tags.add(tag.trim());
                }
            }
        }
        return tags;
    }

    /**
     * 限制封面短文案长度，避免生成图中文字过长。
     */
    private static String limitText(String value, int maxLength) {
        String trimmed = StringUtils.defaultString(value).trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    /**
     * 基于封面文案组装可直接用于图片模型的中文提示词。
     */
    private static String buildCoverPrompt(RednoteWorkflowState.CoverImagePromptResponse promptResponse,
                                           String subject, String bodyContent) {
        String contentSummary = StringUtils.abbreviate(StringUtils.defaultIfBlank(bodyContent, subject), 100);
        return subject + "，小红书封面图，主标题：" + promptResponse.getTitle()
                + "，副标题：" + promptResponse.getSubtitle()
                + "，点缀文案：" + promptResponse.getDecorativeText()
                + "，相关标签：" + String.join(" ", promptResponse.getTags())
                + "，画面信息基于内容：" + contentSummary
                + "，竖版封面构图，醒目中文排版，干净高级设计，明亮自然光，高清细节";
    }
}
