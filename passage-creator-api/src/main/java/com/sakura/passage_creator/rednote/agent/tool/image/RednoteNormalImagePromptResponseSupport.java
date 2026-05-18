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
 * 普通配图提示词 Agent 输出解析工具，负责结构化、兜底和数量上限裁剪。
 */
@Slf4j
public final class RednoteNormalImagePromptResponseSupport {

    /**
     * 默认普通配图数量。
     */
    private static final int DEFAULT_IMAGE_COUNT = 3;

    /**
     * 普通配图数量上限，不包含封面图。
     */
    private static final int MAX_IMAGE_PROMPT_COUNT = 5;

    /**
     * 普通配图提示词结构化输出转换器。
     */
    private static final BeanOutputConverter<RednoteWorkflowState.NormalImagePromptResponse> OUTPUT_CONVERTER =
            new BeanOutputConverter<>(RednoteWorkflowState.NormalImagePromptResponse.class);

    private RednoteNormalImagePromptResponseSupport() {
    }

    /**
     * 获取普通配图提示词结构化输出格式说明。
     */
    public static String format() {
        return OUTPUT_CONVERTER.getFormat();
    }

    /**
     * 解析模型文本并按 imageCount 裁剪到最多 5 条。
     */
    public static RednoteWorkflowState.NormalImagePromptResponse parseAndNormalize(String response, Integer imageCount,
                                                                                  String subject, String bodyContent) {
        RednoteWorkflowState.NormalImagePromptResponse promptResponse = parseResponse(response);
        normalize(promptResponse, imageCount, subject, bodyContent);
        return promptResponse;
    }

    /**
     * 规范化已结构化的模型输出，避免下游图片生成节点拿到空提示词。
     */
    public static void normalize(RednoteWorkflowState.NormalImagePromptResponse promptResponse, Integer imageCount,
                                 String subject, String bodyContent) {
        if (promptResponse == null) {
            throw new IllegalStateException("普通图片提示词 Agent 未返回结构化结果");
        }
        int expectedCount = normalizeImageCount(imageCount);
        List<RednoteWorkflowState.ImagePromptItem> normalizedItems = new ArrayList<>();
        if (CollUtil.isNotEmpty(promptResponse.getImagePrompts())) {
            for (RednoteWorkflowState.ImagePromptItem item : promptResponse.getImagePrompts()) {
                if (item == null) {
                    continue;
                }
                String prompt = StringUtils.defaultIfBlank(item.getPrompt(), firstNotBlank(item.getVariants()));
                if (StringUtils.isBlank(prompt)) {
                    continue;
                }
                int position = normalizedItems.size() + 1;
                item.setPosition(position);
                item.setPurpose(StringUtils.defaultIfBlank(item.getPurpose(), "普通配图" + position));
                item.setPrompt(prompt.trim());
                item.setVariants(normalizeVariants(item.getVariants(), item.getPrompt()));
                normalizedItems.add(item);
                if (normalizedItems.size() >= expectedCount) {
                    break;
                }
            }
        }
        while (normalizedItems.size() < expectedCount) {
            int position = normalizedItems.size() + 1;
            String fallbackPrompt = buildFallbackPrompt(subject, bodyContent, position);
            normalizedItems.add(RednoteWorkflowState.ImagePromptItem.builder()
                    .position(position)
                    .purpose("普通配图" + position)
                    .prompt(fallbackPrompt)
                    .variants(List.of(fallbackPrompt))
                    .build());
        }
        promptResponse.setImagePrompts(normalizedItems);
    }

    /**
     * 将普通配图提示词转换为 Graph state 更新。
     */
    public static Map<String, Object> toStateUpdates(RednoteWorkflowState.NormalImagePromptResponse promptResponse) {
        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put(RednoteWorkflowState.KEY_NORMAL_IMAGE_PROMPT_RESPONSE, promptResponse);
        updates.put(RednoteWorkflowState.KEY_IMAGE_PROMPTS, promptResponse.getImagePrompts());
        return updates;
    }

    /**
     * 优先使用结构化转换器解析，失败时兼容 prompts/output1 等模型常见别名。
     */
    private static RednoteWorkflowState.NormalImagePromptResponse parseResponse(String response) {
        try {
            return OUTPUT_CONVERTER.convert(response);
        } catch (RuntimeException e) {
            log.warn("普通图片提示词结构化解析失败，尝试 JSON 兜底", e);
            JSONObject jsonObject = JSONUtil.parseObj(extractJsonObject(response));
            Object promptsValue = jsonObject.get("imagePrompts");
            if (promptsValue == null) {
                promptsValue = jsonObject.get("prompts");
            }
            List<RednoteWorkflowState.ImagePromptItem> items = parsePromptItems(promptsValue);
            String output1 = jsonObject.getStr("output1");
            if (StringUtils.isNotBlank(output1)) {
                items.add(RednoteWorkflowState.ImagePromptItem.builder()
                        .purpose("普通配图" + (items.size() + 1))
                        .prompt(output1.trim())
                        .variants(List.of(output1.trim()))
                        .build());
            }
            return RednoteWorkflowState.NormalImagePromptResponse.builder()
                    .imagePrompts(items)
                    .build();
        }
    }

    /**
     * 解析数组形式或字符串形式的提示词列表。
     */
    private static List<RednoteWorkflowState.ImagePromptItem> parsePromptItems(Object promptsValue) {
        List<RednoteWorkflowState.ImagePromptItem> items = new ArrayList<>();
        if (promptsValue instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                items.add(parsePromptItem(item, items.size() + 1));
            }
            return items;
        }
        if (promptsValue != null && StringUtils.isNotBlank(promptsValue.toString())) {
            items.add(parsePromptItem(promptsValue, 1));
        }
        return items;
    }

    /**
     * 兼容单个提示词既可能是字符串，也可能是对象。
     */
    private static RednoteWorkflowState.ImagePromptItem parsePromptItem(Object value, int position) {
        if (value instanceof CharSequence text) {
            String prompt = text.toString().trim();
            return RednoteWorkflowState.ImagePromptItem.builder()
                    .position(position)
                    .purpose("普通配图" + position)
                    .prompt(prompt)
                    .variants(List.of(prompt))
                    .build();
        }
        JSONObject jsonObject = JSONUtil.parseObj(JSONUtil.toJsonStr(value));
        String prompt = StringUtils.defaultIfBlank(jsonObject.getStr("prompt"), jsonObject.getStr("output1"));
        List<String> variants = parseStringList(jsonObject.get("variants"));
        return RednoteWorkflowState.ImagePromptItem.builder()
                .position(jsonObject.getInt("position", position))
                .purpose(StringUtils.defaultIfBlank(jsonObject.getStr("purpose"), "普通配图" + position))
                .prompt(prompt)
                .variants(variants)
                .build();
    }

    /**
     * 从模型返回中截取 JSON 对象。
     */
    private static String extractJsonObject(String response) {
        if (StrUtil.isBlank(response)) {
            throw new IllegalStateException("普通图片提示词 Agent 未返回内容");
        }
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("普通图片提示词 Agent 未返回合法 JSON");
        }
        return response.substring(start, end + 1);
    }

    /**
     * 将 imageCount 统一限定在 1 到 5 之间。
     */
    private static int normalizeImageCount(Integer imageCount) {
        if (imageCount == null || imageCount <= 0) {
            return DEFAULT_IMAGE_COUNT;
        }
        return Math.min(imageCount, MAX_IMAGE_PROMPT_COUNT);
    }

    /**
     * 清洗备选提示词，最多保留 3 条。
     */
    private static List<String> normalizeVariants(List<String> variants, String prompt) {
        List<String> normalized = new ArrayList<>();
        if (CollUtil.isNotEmpty(variants)) {
            for (String variant : variants) {
                if (StringUtils.isBlank(variant)) {
                    continue;
                }
                normalized.add(variant.trim());
                if (normalized.size() >= 3) {
                    break;
                }
            }
        }
        if (normalized.isEmpty() && StringUtils.isNotBlank(prompt)) {
            normalized.add(prompt);
        }
        return normalized;
    }

    /**
     * 解析字符串数组，兼容模型返回的非标准字段。
     */
    private static List<String> parseStringList(Object value) {
        List<String> values = new ArrayList<>();
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item != null && StringUtils.isNotBlank(item.toString())) {
                    values.add(item.toString().trim());
                }
            }
        }
        return values;
    }

    /**
     * 获取第一个非空备选提示词。
     */
    private static String firstNotBlank(List<String> values) {
        if (CollUtil.isEmpty(values)) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 基于正文兜底生成可用于图片模型的中文提示词。
     */
    private static String buildFallbackPrompt(String subject, String bodyContent, int position) {
        String safeSubject = StringUtils.defaultIfBlank(subject, "小红书分享主题");
        String safeContent = StringUtils.abbreviate(StringUtils.defaultIfBlank(bodyContent, safeSubject), 80);
        return safeSubject + "，小红书普通配图" + position + "，围绕内容：" + safeContent
                + "，写实摄影风格，明亮自然光，干净背景，细节丰富，高清质感";
    }
}
