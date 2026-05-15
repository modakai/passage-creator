package com.sakura.passage_creator.prompt.model.enums;

import java.util.Arrays;

/**
 * Prompt 反馈满意度枚举。
 */
public enum PromptFeedbackRatingEnum {

    /**
     * 用户对当前环节 Prompt 效果非常满意。
     */
    VERY_SATISFIED("VERY_SATISFIED", "非常满意"),

    /**
     * 用户对当前环节 Prompt 效果满意。
     */
    SATISFIED("SATISFIED", "满意"),

    /**
     * 用户认为当前环节 Prompt 效果一般。
     */
    NEUTRAL("NEUTRAL", "一般"),

    /**
     * 用户对当前环节 Prompt 效果不满意。
     */
    UNSATISFIED("UNSATISFIED", "不满意");

    /**
     * 持久化枚举值。
     */
    private final String value;

    /**
     * 管理端展示名称。
     */
    private final String label;

    PromptFeedbackRatingEnum(String value, String label) {
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
     * 判断入参是否属于支持的满意度结果。
     */
    public static boolean isValid(String value) {
        return Arrays.stream(values()).anyMatch(item -> item.value.equals(value));
    }

    /**
     * 按持久化值解析枚举。
     */
    public static PromptFeedbackRatingEnum of(String value) {
        return Arrays.stream(values())
                .filter(item -> item.value.equals(value))
                .findFirst()
                .orElse(null);
    }
}
