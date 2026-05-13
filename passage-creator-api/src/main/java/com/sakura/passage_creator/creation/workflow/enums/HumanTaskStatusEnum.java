package com.sakura.passage_creator.creation.workflow.enums;

import lombok.Getter;

/**
 * 人工任务状态。
 */
@Getter
public enum HumanTaskStatusEnum {

    /**
     * 等待用户处理。
     */
    WAITING("WAITING"),

    /**
     * 用户已提交结构化结果。
     */
    COMPLETED("COMPLETED"),

    /**
     * 人工任务已取消。
     */
    CANCELLED("CANCELLED"),

    /**
     * 人工任务已过期。
     */
    EXPIRED("EXPIRED");

    private final String value;

    HumanTaskStatusEnum(String value) {
        this.value = value;
    }
}
