package com.sakura.passage_creator.prompt.model.enums;

import java.util.Arrays;
import java.util.List;

/**
 * Prompt 反馈采集环节枚举，使用稳定值承载统计维度。
 */
public enum PromptFeedbackStageEnum {

    /**
     * 标题生成完毕，等待用户选择标题。
     */
    TITLE_SELECTION("TITLE_SELECTION", "标题生成"),

    /**
     * 大纲生成完毕，等待用户编辑大纲。
     */
    OUTLINE_EDITING("OUTLINE_EDITING", "大纲生成"),

    /**
     * 正文和图片融合完成。
     */
    CONTENT_MERGED("CONTENT_MERGED", "正文融合");

    /**
     * 持久化枚举值。
     */
    private final String value;

    /**
     * 管理端展示名称。
     */
    private final String label;

    PromptFeedbackStageEnum(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 判断入参是否属于支持的反馈环节。
     */
    public static boolean isValid(String value) {
        return Arrays.stream(values()).anyMatch(item -> item.value.equals(value));
    }

    /**
     * 按持久化值解析枚举。
     */
    public static PromptFeedbackStageEnum of(String value) {
        return Arrays.stream(values())
                .filter(item -> item.value.equals(value))
                .findFirst()
                .orElse(null);
    }

    /**
     * 返回管理端统计需要展示的固定环节顺序。
     */
    public static List<PromptFeedbackStageEnum> orderedStages() {
        return List.of(TITLE_SELECTION, OUTLINE_EDITING, CONTENT_MERGED);
    }
}
